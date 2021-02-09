package com.cloudkitchens.ordersengine.shelves.domain.events;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.Temperature;

import lombok.Value;

@Value
public class ShelveOrderWithMoveRequested {
    private final OrderId orderId;
    private final Temperature temperature;
}
