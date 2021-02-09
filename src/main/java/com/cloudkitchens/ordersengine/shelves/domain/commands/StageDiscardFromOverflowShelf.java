package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class StageDiscardFromOverflowShelf {
    @TargetAggregateIdentifier
    private final ShelfType sourceShelf; // This will always be equal to OVERFLOW

    private final OrderId orderId;
}
