package com.cloudkitchens.ordersengine.shelves.domain.events;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class MoveFromOverflowShelfStaged {
    private final OrderId orderId;
    private final SavedSpotId savedSpotId;

    // Staged order has this temperature
    private final Temperature temperature;
    private final Double remainingValue;
    private final Double initialShelfLife;
    private final Double decayRate;
}
