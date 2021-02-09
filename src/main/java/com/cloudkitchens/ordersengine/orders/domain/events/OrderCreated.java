package com.cloudkitchens.ordersengine.orders.domain.events;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class OrderCreated {
    private final OrderId orderId;
    private final String name;
    private final Temperature temp;
    private final Double shelfLife;
    private final Double decayRate;
}
