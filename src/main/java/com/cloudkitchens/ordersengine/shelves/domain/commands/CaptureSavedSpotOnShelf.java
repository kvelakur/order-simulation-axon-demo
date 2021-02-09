package com.cloudkitchens.ordersengine.shelves.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class CaptureSavedSpotOnShelf {
    @TargetAggregateIdentifier
    private final ShelfType shelfType;
    private final SavedSpotId savedSpotId;

    private final OrderId orderId;
    private final Temperature temperature;
    private final Double remainingValue;
    private final Double initialShelfLife;
    private final Double decayRate;
}
