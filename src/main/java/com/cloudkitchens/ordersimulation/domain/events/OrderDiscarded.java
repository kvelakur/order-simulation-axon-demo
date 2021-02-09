package com.cloudkitchens.ordersimulation.domain.events;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.ShelfType;

import lombok.Value;

@Value
public class OrderDiscarded {
    private OrderId orderId;
    private ShelfType shelfType;
}
