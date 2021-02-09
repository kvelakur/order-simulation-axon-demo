package com.cloudkitchens.ordersimulation.domain.events;

import com.cloudkitchens.ordersimulation.domain.OrderId;

import lombok.Value;

@Value
public class ShelveOrderWithDiscardRequested {
    private final OrderId id;

}
