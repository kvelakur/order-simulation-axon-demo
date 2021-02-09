package com.cloudkitchens.ordersimulation.aggregates;

import java.util.HashSet;
import java.util.Set;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import com.cloudkitchens.ordersimulation.domain.NoShelfSpotAvailable;
import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.ShelfType;
import com.cloudkitchens.ordersimulation.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersimulation.domain.commands.CreateShelf;
import com.cloudkitchens.ordersimulation.domain.commands.SaveSpotOnShelf;
import com.cloudkitchens.ordersimulation.domain.commands.StageMoveFromShelf;
import com.cloudkitchens.ordersimulation.domain.events.MoveFromShelfStaged;
import com.cloudkitchens.ordersimulation.domain.events.OrderDiscarded;
import com.cloudkitchens.ordersimulation.domain.events.OrderShelved;
import com.cloudkitchens.ordersimulation.domain.events.ShelfCreated;
import com.cloudkitchens.ordersimulation.domain.events.SpotSavedOnShelf;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aggregate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class ShelfAggregate {
    @Getter
    @AggregateIdentifier
    private ShelfType shelfType;

    private int shelfCapacity;

    // These are items currently on shelf
    private Set<OrderId> onShelf;
    // When moving order, save space on destination shelf before removing from source shelf
    private Set<OrderId> savedSpots;

    @CommandHandler
    public ShelfAggregate(final CreateShelf cmd) {
        log.info("Creating a shelf of type {} with capacity {}", cmd.getShelfType(), cmd.getCapacity());
        AggregateLifecycle.apply(new ShelfCreated(cmd.getShelfType(), cmd.getCapacity()));
    }

    @CommandHandler
    public SpotSavedOnShelf handle(final SaveSpotOnShelf cmd) {
        if (onShelf.size() + savedSpots.size() >= shelfCapacity) {
            throw new NoShelfSpotAvailable(cmd.getShelfType(), cmd.getOrderId());
        }
        final SpotSavedOnShelf event = new SpotSavedOnShelf(cmd.getShelfType(), cmd.getOrderId());
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public MoveFromShelfStaged handle(final StageMoveFromShelf cmd) {
        if (!cmd.getSourceShelf().equals(ShelfType.OVERFLOW)) {
            throw new IllegalArgumentException("Move can be initiated from Overflow shelf only");
        }
        if (!onShelf.contains(cmd.getOrderId())) {
            throw new IllegalArgumentException("Order " + cmd.getOrderId() + " that is not on shelf cannot be moved");
        }
        final MoveFromShelfStaged event = new MoveFromShelfStaged(cmd.getSourceShelf(), cmd.getDestinationShelf(),
            cmd.getOrderId());
        AggregateLifecycle.apply(event);
        return event;
    }

    @CommandHandler
    public void handle(final CaptureSavedSpotOnShelf cmd) {
        if (!savedSpots.contains(cmd.getOrderId())) {
            throw new IllegalArgumentException("Order " + cmd.getOrderId() + " that is not saved cannot be captured");
        }
        AggregateLifecycle.apply(new OrderShelved(cmd.getOrderId(), cmd.getShelfType()));
    }

    @EventSourcingHandler
    public void on(final ShelfCreated event) {
        shelfType = event.getShelfType();
        shelfCapacity = event.getCapacity();
        onShelf = new HashSet<>(shelfCapacity);
    }

    @EventSourcingHandler
    public void on(final SpotSavedOnShelf event) {
        savedSpots.add(event.getOrderId());
    }

    @EventSourcingHandler
    public void on(final MoveFromShelfStaged event) {
        onShelf.remove(event.getOrderId());
        savedSpots.add(event.getOrderId());
    }

    @EventHandler
    public void on(final OrderShelved event) {
        savedSpots.remove(event.getOrderId());
        onShelf.add(event.getOrderId());
    }

    @EventHandler
    public void on(final OrderDiscarded event) {
        onShelf.remove(event.getOrderId());
    }
}
