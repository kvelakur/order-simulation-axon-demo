package com.cloudkitchens.ordersimulation.domain.events;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.ShelfType;

import lombok.Value;

@Value
public class MoveFromShelfStaged {
    private final ShelfType sourceShelf;
    private final ShelfType destinationShelf;
    private final OrderId orderId;
}
