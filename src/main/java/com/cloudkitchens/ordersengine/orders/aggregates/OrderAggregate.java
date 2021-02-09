package com.cloudkitchens.ordersengine.orders.aggregates;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.commandhandling.model.AggregateLifecycle;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;
import com.cloudkitchens.ordersengine.orders.domain.events.OrderCreated;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aggregate
@NoArgsConstructor
@Slf4j
public class OrderAggregate {
    @Getter
    @AggregateIdentifier
    private OrderId orderId;

    @CommandHandler
    public OrderAggregate(final CreateOrder cmd) {
        AggregateLifecycle
            .apply(new OrderCreated(cmd.getOrderId(), cmd.getName(), cmd.getTemp(), cmd.getShelfLife(),
                cmd.getDecayRate()));
    }

    @EventSourcingHandler
    private void onOrderCreated(final EventMessage<OrderCreated> eventMessage) {
        final OrderCreated event = eventMessage.getPayload();
        orderId = event.getOrderId();
        log.info("Order {}, type {} with value {} created", event.getOrderId(), event.getTemp(), 1.0);
    }
}
