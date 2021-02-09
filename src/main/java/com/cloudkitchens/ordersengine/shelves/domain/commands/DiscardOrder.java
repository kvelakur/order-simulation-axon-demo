package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class DiscardOrder {
    @TargetAggregateIdentifier
    private final ShelfType shelfType;
    private final OrderId orderId;
}
