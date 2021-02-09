package com.cloudkitchens.ordersengine.orders;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventhandling.saga.EndSaga;
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.saga.SagaLifecycle;
import org.axonframework.eventhandling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.OrderValue;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;
import com.cloudkitchens.ordersengine.orders.domain.events.OrderCreated;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.DiscardOrder;
import com.cloudkitchens.ordersengine.shelves.domain.commands.ReleaseSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.SaveSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageDiscardFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageMoveFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.events.DiscardFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.MoveFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDelivered;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDiscarded;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderShelved;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelveOrderWithDiscardRequested;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelveOrderWithMoveRequested;
import com.cloudkitchens.ordersengine.shelves.domain.events.SpotSavedOnShelf;

import lombok.extern.slf4j.Slf4j;

@Saga
@Slf4j
public class OrderLifecycleSaga {
    public static final String DEADLINE_NAME = "order-deadline";

    private Double decayRate;
    private Double initialShelfLife;
    private Double remainingValue;
    private Instant shelvedAt;
    private ShelfType shelfType;
    private Temperature temperature;
    private String deadlineId;

    private static SpotSavedOnShelf saveSpotOnShelf(final ShelfType shelfType, final CommandGateway commandGateway) {
        try {
            return commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(
                new SaveSpotOnShelf(shelfType)));
        } catch (final Throwable t) {
            // log.info("Error saving spot", t);
            return null;
        }
    }

    private static MoveFromOverflowShelfStaged moveFromOverflowShelf(final Temperature temperature,
        final CommandGateway commandGateway) {
        try {
            return commandGateway
                .sendAndWait(GenericCommandMessage
                    .asCommandMessage(new StageMoveFromOverflowShelf(ShelfType.OVERFLOW, temperature)));
        } catch (final Throwable t) {
            // log.info("Error moving order", t);
            return null;
        }
    }

    private static void releaseSavedSpot(final ShelfType shelfType, final SavedSpotId savedSpotId,
        final CommandGateway commandGateway) {
        commandGateway
            .sendAndWait(GenericCommandMessage.asCommandMessage(new ReleaseSavedSpotOnShelf(shelfType, savedSpotId)));
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CompletableFuture<OrderShelved> captureSavedSpot(final ShelfType sT, final SavedSpotId savedSpotId,
        final OrderId orderId,
        final Temperature temp, final double remainingV,
        final double initShelfLife, final double decayR, final CommandGateway commandGateway) {
        return commandGateway
            .send(GenericCommandMessage.asCommandMessage(
                new CaptureSavedSpotOnShelf(sT, savedSpotId, orderId, temp, remainingV,
                    initShelfLife, decayR)));
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void onMoveRequest(final ShelveOrderWithMoveRequested request, final CommandGateway commandGateway,
        final EventBus eventBus) {
        for (final ShelfType type : Arrays.asList(ShelfType.COLD, ShelfType.FROZEN, ShelfType.HOT)) {
            final SpotSavedOnShelf spot = saveSpotOnShelf(type, commandGateway);
            if (spot != null) {
                final MoveFromOverflowShelfStaged overflowSpot = moveFromOverflowShelf(Temperature.valueOf(type.name()),
                    commandGateway);
                if (overflowSpot != null) {
                    captureSavedSpot(spot.getShelfType(),
                        spot.getSavedSpotId(),
                        overflowSpot.getOrderId(),
                        overflowSpot.getTemperature(),
                        overflowSpot.getRemainingValue(),
                        overflowSpot.getInitialShelfLife(),
                        overflowSpot.getDecayRate(),
                        commandGateway);
                    captureSavedSpot(ShelfType.OVERFLOW,
                        overflowSpot.getSavedSpotId(),
                        request.getOrderId(),
                        request.getTemperature(),
                            remainingValue,
                            initialShelfLife,
                            decayRate,
                        commandGateway);
                    return;
                } else {
                    releaseSavedSpot(spot.getShelfType(), spot.getSavedSpotId(), commandGateway);
                }
            }
        }
        eventBus.publish(GenericEventMessage
            .asEventMessage(new ShelveOrderWithDiscardRequested(request.getOrderId(), request.getTemperature())));
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void onDiscardRequest(final ShelveOrderWithDiscardRequested requestEvent,
        final CommandGateway commandGateway) {
        final DiscardFromOverflowShelfStaged staged = commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(
            new StageDiscardFromOverflowShelf(ShelfType.OVERFLOW, requestEvent.getOrderId())));
        captureSavedSpot(ShelfType.OVERFLOW,
            staged.getSavedSpotId(),
            staged.getToShelve(),
                temperature,
                remainingValue,
                initialShelfLife,
                decayRate,
            commandGateway);
    }

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId", payloadType = OrderCreated.class)
    public void onOrderCreated(final EventMessage<OrderCreated> createdMessage, final CommandGateway commandGateway,
        final EventBus eventBus) {
        final OrderCreated event = createdMessage.getPayload();
        final OrderId orderId = event.getOrderId();
        final Instant createdAt = createdMessage.getTimestamp();
        temperature = event.getTemp();
        decayRate = event.getDecayRate();
        initialShelfLife = event.getShelfLife();
        remainingValue = 1.0D;
        log.debug("Start Order saga for {}", event.getOrderId());

        final SpotSavedOnShelf properSpot = saveSpotOnShelf(ShelfType.fromTemperature(event.getTemp()), commandGateway);
        if (properSpot != null) {
            captureSavedSpot(
                properSpot.getShelfType(),
                properSpot.getSavedSpotId(),
                orderId,
                event.getTemp(),
                    remainingValue,
                    initialShelfLife,
                    decayRate,
                commandGateway);
            return;
        }
        final SpotSavedOnShelf overflowSpot = saveSpotOnShelf(ShelfType.OVERFLOW, commandGateway);
        if (overflowSpot != null) {
            captureSavedSpot(
                overflowSpot.getShelfType(),
                overflowSpot.getSavedSpotId(),
                orderId,
                event.getTemp(),
                    remainingValue,
                    initialShelfLife,
                    decayRate,
                commandGateway);
            return;
        }

        // Order could not be placed on neither proper shelf or overflow shelf. Request that item from overflow be moved
        // so space can be made.
        eventBus.publish(GenericEventMessage.asEventMessage(new ShelveOrderWithMoveRequested(event.getOrderId(),
            event.getTemp())));
    }

    @SagaEventHandler(associationProperty = "orderId", payloadType = OrderShelved.class)
    public void onShelved(final EventMessage<OrderShelved> shelvedMessage, final DeadlineManager deadlineManager) {
        final OrderShelved event = shelvedMessage.getPayload();
        cancelDeadline(deadlineManager);
        remainingValue = event.getRemainingValue();
        shelvedAt = shelvedMessage.getTimestamp();
        shelfType = event.getShelfType();
        final Double remainingShelfLife = remainingValue * initialShelfLife;
        final OrderValue value = new OrderValue(shelvedAt, remainingShelfLife, decayRate,
            event.getShelfType());
        final Instant now = Instant.now();
        deadlineId = deadlineManager
            .schedule(value.getZeroTime(now), DEADLINE_NAME, event.getOrderId());
        log.info("Order {} with value {} was shelved on {}. On shelf {} items: {}",
            event.getOrderId(),
            value.getCurrentValue(now),
            event.getShelfType(),
            event.getShelfContents().size(),
            event.getShelfContents());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void onDiscard(final OrderDiscarded event, final DeadlineManager deadlineManager) {
        cancelDeadline(deadlineManager);
        final Double remainingShelfLife = remainingValue * initialShelfLife;
        final OrderValue value = new OrderValue(shelvedAt, remainingShelfLife, decayRate,
            event.getShelfType());
        log.info("Order {} with value {} was discarded from {}. On shelf {} items: {}",
            event.getOrderId(),
            value.getCurrentValue(Instant.now()),
            event.getShelfType(),
            event.getShelfContents().size(),
            event.getShelfContents());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void onDelivered(final OrderDelivered event, final DeadlineManager deadlineManager) {
        cancelDeadline(deadlineManager);
        log.info("Order {} with value {} was delivered from {}. On shelf {} items: {}",
            event.getOrderId(),
            event.getCurrentValue(),
            event.getShelfType(),
            event.getShelfContents().size(),
            event.getShelfContents());
    }

    @DeadlineHandler(deadlineName = DEADLINE_NAME)
    public void handleDeadline(final OrderId orderId, final CommandGateway commandGateway) {
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(
            new DiscardOrder(shelfType, orderId)));
        SagaLifecycle.end();
    }

    private void cancelDeadline(final DeadlineManager deadlineManager) {
        if (deadlineId == null) {
            return;
        }
        deadlineManager.cancelSchedule(DEADLINE_NAME, deadlineId);
    }
}
