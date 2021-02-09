package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class ReleaseSavedSpotOnShelf {
    @TargetAggregateIdentifier
    private final ShelfType shelfType;
    private final SavedSpotId savedSpotId;
}
