package com.cloudkitchens.ordersimulation.domain.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.ShelfType;

import lombok.Value;

@Value
public class SaveSpotOnShelf {
    @TargetAggregateIdentifier
    private final ShelfType shelfType;
    private final OrderId orderId;
}
