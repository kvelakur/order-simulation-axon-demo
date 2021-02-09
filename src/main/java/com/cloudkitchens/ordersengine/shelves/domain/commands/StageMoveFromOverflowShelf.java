package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class StageMoveFromOverflowShelf {
    @TargetAggregateIdentifier
    private final ShelfType sourceShelf; // This will always be equal to OVERFLOW
    // Attempt to move an order with this temperature
    private final Temperature temperature;
}
