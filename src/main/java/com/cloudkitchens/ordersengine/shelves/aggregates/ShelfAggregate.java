package com.cloudkitchens.ordersengine.shelves.aggregates;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.commandhandling.model.AggregateLifecycle;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;

import com.cloudkitchens.ordersengine.domain.NoShelfSpotAvailable;
import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.OrderValue;
import com.cloudkitchens.ordersengine.domain.OverflowItemTypeNotFound;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CreateShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.DeliverOrder;
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
import com.cloudkitchens.ordersengine.shelves.domain.events.SavedSpotOnShelfReleased;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelfCreated;
import com.cloudkitchens.ordersengine.shelves.domain.events.SpotSavedOnShelf;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aggregate
@NoArgsConstructor
@Slf4j
public class ShelfAggregate {
    @Getter
    @AggregateIdentifier
    private ShelfType shelfType;

    private Integer shelfCapacity;

    // These are items currently on shelf
    private Set<OrderShelved> onShelf;
    private Map<OrderId, Instant> shelvedAt;
    // When moving order, save space on destination shelf before removing from source shelf
    private Set<SavedSpotId> savedSpots;

    @CommandHandler
    public ShelfAggregate(final CreateShelf cmd) {
        if (cmd.getCapacity() <= 0) {
            throw new IllegalArgumentException("Cannot create shelves with zero or negative capacity");
        }
        AggregateLifecycle.apply(new ShelfCreated(cmd.getShelfType(), cmd.getCapacity()));
    }

    @CommandHandler
    public SpotSavedOnShelf handle(final SaveSpotOnShelf cmd) {
        if (onShelf.size() + savedSpots.size() >= shelfCapacity) {
            throw new NoShelfSpotAvailable(cmd.getShelfType());
        }
        final SpotSavedOnShelf event = new SpotSavedOnShelf(cmd.getShelfType(), new SavedSpotId());
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public OrderShelved handle(final CaptureSavedSpotOnShelf cmd) {
        if (!savedSpots.contains(cmd.getSavedSpotId())) {
            throw new IllegalArgumentException("Order " + cmd.getOrderId() + " that is not saved cannot be captured");
        }
        if (!shelfType.equals(ShelfType.OVERFLOW)
            && !shelfType.equals(ShelfType.fromTemperature(cmd.getTemperature()))) {
            throw new IllegalArgumentException(String.format("Order %s of type %s cannot be placed on %s shelf",
                cmd.getOrderId(),
                cmd.getTemperature(),
                    shelfType));
        }
        final List<OrderId> orderIds = orderIdsOnShelf();
        orderIds.add(cmd.getOrderId());
        final OrderShelved event = new OrderShelved(cmd.getOrderId(), cmd.getSavedSpotId(), cmd.getShelfType(),
            cmd.getTemperature(), cmd.getRemainingValue(), cmd.getInitialShelfLife(), cmd.getDecayRate(),
            orderIds);
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public SavedSpotOnShelfReleased handle(final ReleaseSavedSpotOnShelf cmd) {
        if (!savedSpots.contains(cmd.getSavedSpotId())) {
            return null;
        }

        final SavedSpotOnShelfReleased event = new SavedSpotOnShelfReleased(cmd.getShelfType(), cmd.getSavedSpotId());
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public MoveFromOverflowShelfStaged handle(final StageMoveFromOverflowShelf cmd) {
        if (!cmd.getSourceShelf().equals(ShelfType.OVERFLOW)) {
            throw new IllegalArgumentException("Move can be initiated from Overflow shelf only");
        }
        final OrderShelved toStage = onShelf.stream()
            .filter(shelved -> shelved.getTemperature().equals(cmd.getTemperature())).findFirst()
            .orElseThrow(() -> new OverflowItemTypeNotFound(cmd.getTemperature()));

        final MoveFromOverflowShelfStaged event = new MoveFromOverflowShelfStaged(
            toStage.getOrderId(),
            new SavedSpotId(toStage.getOrderId().getValue()), toStage.getTemperature(), toStage.getRemainingValue(),
            toStage.getInitialShelfLife(), toStage.getDecayRate());

        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public DiscardFromOverflowShelfStaged handle(final StageDiscardFromOverflowShelf cmd) {
        if (cmd.getSourceShelf() != ShelfType.OVERFLOW) {
            throw new IllegalArgumentException("Discard can be initiated from Overflow shelf only");
        }
        final SavedSpotId savedSpotId = new SavedSpotId();
        final OrderId toDiscard;
        final List<OrderId> idsOnShelf = orderIdsOnShelf();
        if (onShelf.size() > 0) {
            toDiscard = onShelf.stream().findAny().get().getOrderId();
            idsOnShelf.remove(toDiscard);
        } else {
            toDiscard = null;
        }
        final DiscardFromOverflowShelfStaged staged = new DiscardFromOverflowShelfStaged(cmd.getOrderId(), savedSpotId);
        AggregateLifecycle.apply(staged);

        // Also perform discard
        if (toDiscard != null) {
            AggregateLifecycle.apply(new OrderDiscarded(shelfType, toDiscard, idsOnShelf));
        }

        return staged;
    }

    @CommandHandler
    public OrderDiscarded handle(final DiscardOrder cmd) {
        final OrderShelved toDiscard = onShelf.stream()
            .filter(shelved -> shelved.getOrderId().equals(cmd.getOrderId()))
            .findFirst().orElse(null);
        if (toDiscard == null) {
            return null;
        }

        final List<OrderId> orderIds = orderIdsOnShelf();
        orderIds.remove(toDiscard.getOrderId());
        final OrderDiscarded event = new OrderDiscarded(cmd.getShelfType(), cmd.getOrderId(), orderIds);
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public OrderDelivered handle(final DeliverOrder cmd) {
        final OrderId orderId = cmd.getOrderId();
        final OrderShelved toDeliver = onShelf.stream()
            .filter(shelved -> shelved.getOrderId().equals(orderId))
            .findFirst().orElse(null);
        if (toDeliver == null) {
            log.debug("Order {} is not {} shelf", orderId, shelfType);
            return null;
        }
        final double remainingShelfLife = toDeliver.getInitialShelfLife() * toDeliver.getRemainingValue();
        final OrderValue value = new OrderValue(shelvedAt.get(orderId), remainingShelfLife, toDeliver.getDecayRate(),
                shelfType);
        final double currentValue = value.getCurrentValue(Instant.now());
        if (currentValue <= 0) {
            log.debug("Order {} will not be delivered, its value has gone to 0", orderId);
            return null;
        }
        final List<OrderId> orderIds = orderIdsOnShelf();
        orderIds.remove(orderId);
        final OrderDelivered event = new OrderDelivered(shelfType, orderId, currentValue, orderIds);
        AggregateLifecycle.apply(event);
        return event;
    }

    @EventSourcingHandler(payloadType = ShelfCreated.class)
    private void onShelfCreated(final EventMessage<ShelfCreated> eventMessage) {
        final ShelfCreated event = eventMessage.getPayload();
        shelfType = event.getShelfType();
        shelfCapacity = event.getCapacity();
        onShelf = new HashSet<>(shelfCapacity);
        savedSpots = new HashSet<>(shelfCapacity);
        shelvedAt = new HashMap<>();
    }

    @EventSourcingHandler
    private void onSpotSaved(final SpotSavedOnShelf event) {
        savedSpots.add(event.getSavedSpotId());
    }

    @EventSourcingHandler
    private void onSavedSpotReleased(final SavedSpotOnShelfReleased event) {
        final SavedSpotId toRemove = savedSpots.stream().filter(orderId -> event.getSavedSpotId().equals(orderId))
            .findFirst()
            .get();
        savedSpots.remove(toRemove);
    }

    @EventSourcingHandler
    private void onMoveStaged(final MoveFromOverflowShelfStaged event) {
        final OrderShelved toRemove = onShelf.stream()
            .filter(shelved -> shelved.getOrderId().equals(event.getOrderId()))
            .findFirst().get();
        onShelf.remove(toRemove);
        savedSpots.add(event.getSavedSpotId());
    }

    @EventSourcingHandler
    private void onDiscardStaged(final DiscardFromOverflowShelfStaged event) {
        savedSpots.add(event.getSavedSpotId());
    }

    @EventSourcingHandler(payloadType = OrderShelved.class)
    private void onOrderShelved(final EventMessage<OrderShelved> eventMessage) {
        final OrderShelved event = eventMessage.getPayload();
        savedSpots.remove(event.getSavedSpotId());
        onShelf.add(event);
        shelvedAt.put(event.getOrderId(), eventMessage.getTimestamp());
    }

    @EventSourcingHandler
    public void onOrderDiscarded(final OrderDiscarded event) {
        final OrderShelved toRemove = onShelf.stream()
            .filter(shelved -> shelved.getOrderId().equals(event.getOrderId()))
            .findFirst()
            .get();
        onShelf.remove(toRemove);
        shelvedAt.remove(toRemove.getOrderId());
    }

    @EventSourcingHandler
    public void onOrderDelivered(final OrderDelivered event) {
        onShelf.stream()
            .filter(shelved -> shelved.getOrderId().equals(event.getOrderId()))
            .findFirst()
            .ifPresent(toRemove -> {
                onShelf.remove(toRemove);
                shelvedAt.remove(toRemove.getOrderId());
            });
    }

    private List<OrderId> orderIdsOnShelf() {
        return onShelf.stream().map(OrderShelved::getOrderId).collect(Collectors.toList());
    }
}
