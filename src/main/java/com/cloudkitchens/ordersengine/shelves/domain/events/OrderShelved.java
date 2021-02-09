package com.cloudkitchens.ordersengine.shelves.domain.events;

import java.util.List;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class OrderShelved {
    private final OrderId orderId;
    private final SavedSpotId savedSpotId;
    private final ShelfType shelfType;
    private final Temperature temperature;

    private final Double remainingValue;
    private final Double initialShelfLife;
    private final Double decayRate;
    private final List<OrderId> shelfContents;
}
