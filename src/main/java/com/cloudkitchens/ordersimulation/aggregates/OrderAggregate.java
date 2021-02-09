package com.cloudkitchens.ordersimulation.aggregates;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.commands.CreateOrder;
import com.cloudkitchens.ordersimulation.domain.events.OrderCreated;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aggregate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class OrderAggregate {
    @Getter
    @AggregateIdentifier
    private OrderId id;

    @CommandHandler
    public OrderAggregate(final CreateOrder cmd) {
        log.info("Order id={} placed", cmd.getId());
        AggregateLifecycle
            .apply(new OrderCreated(cmd.getId(), cmd.getName(), cmd.getTemp(), cmd.getShelfLife(), cmd.getDecayRate()));
    }

    @EventSourcingHandler
    public void on(final OrderCreated cmd) {
        id = cmd.getId();
    }
}
