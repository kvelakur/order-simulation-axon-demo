package com.cloudkitchens.ordersimulation.domain.events;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.Temperature;

import lombok.Value;

@Value
public class OrderCreated {
    private final OrderId id;
    private final String name;
    private final Temperature temp;
    private final Long shelfLife;
    private final Double decayRate;
}
