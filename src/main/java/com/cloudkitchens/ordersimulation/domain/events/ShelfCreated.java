package com.cloudkitchens.ordersimulation.domain.events;

import com.cloudkitchens.ordersimulation.domain.ShelfType;

import lombok.Value;

@Value
public class ShelfCreated {
    private final ShelfType shelfType;
    private final int capacity;
}
