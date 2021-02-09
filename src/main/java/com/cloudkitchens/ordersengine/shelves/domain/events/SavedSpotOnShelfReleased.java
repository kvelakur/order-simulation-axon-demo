package com.cloudkitchens.ordersengine.shelves.domain.events;

import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class SavedSpotOnShelfReleased {
    private final ShelfType shelfType;
    private final SavedSpotId savedSpotId;
}
