package com.cloudkitchens.ordersengine.shelves.domain.events;

import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class ShelfCreated {
    private final ShelfType shelfType;
    private final Integer capacity;
}
