package com.cloudkitchens.ordersengine.shelves.domain.events;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;

import lombok.Value;

@Value
public class DiscardFromOverflowShelfStaged {
    private final OrderId toShelve;

    private final SavedSpotId savedSpotId;
}
