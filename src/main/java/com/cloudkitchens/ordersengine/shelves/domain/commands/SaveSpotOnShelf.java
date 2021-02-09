package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class SaveSpotOnShelf {
    @TargetAggregateIdentifier
    private final ShelfType shelfType;
}
