package com.cloudkitchens.ordersimulation.sagas;

import java.util.Set;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.ShelfType;
import com.cloudkitchens.ordersimulation.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersimulation.domain.commands.SaveSpotOnShelf;
import com.cloudkitchens.ordersimulation.domain.events.OrderCreated;
import com.cloudkitchens.ordersimulation.domain.events.ShelveOrderWithMoveRequested;
import com.cloudkitchens.ordersimulation.domain.events.SpotSavedOnShelf;

import lombok.extern.slf4j.Slf4j;

@Saga
@Slf4j
public class OrderLifecycleSaga {
    // These are items currently on overflow shelf
    private Set<OrderId> onOverflowShelf;

    @StartSaga
    public static void on(final OrderCreated event, final CommandGateway commandGateway, final EventBus eventBus) {
        log.debug("Start Order saga for {}", event.getId());
        if (saveSpotOnShelf(ShelfType.fromTemperature(event.getTemp()), event.getId(), commandGateway)) {
            captureSavedSpot(ShelfType.fromTemperature(event.getTemp()), event.getId(), commandGateway);
        } else if (saveSpotOnShelf(ShelfType.FROZEN, event.getId(), commandGateway)) {
            captureSavedSpot(ShelfType.FROZEN, event.getId(), commandGateway);
        }
        eventBus.publish(GenericEventMessage.asEventMessage(new ShelveOrderWithMoveRequested(event.getId())));
    }

    public static boolean saveSpotOnShelf(final ShelfType type, final OrderId orderId,
        final CommandGateway commandGateway) {
        try {
            final SpotSavedOnShelf spotSaved = commandGateway
                .sendAndWait(new SaveSpotOnShelf(type, orderId));
            return true;
        } catch (final Throwable t) {
            return false;
        }
    }

    public static void captureSavedSpot(final ShelfType type, final OrderId orderId,
        final CommandGateway commandGateway) {
        final SpotSavedOnShelf spotSaved = commandGateway
            .sendAndWait(new CaptureSavedSpotOnShelf(type, orderId));
    }

}
