package com.cloudkitchens.ordersengine.shelves.domain.events;

import java.util.List;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;

import lombok.Value;

@Value
public class OrderDiscarded {
    private final ShelfType shelfType;
    private final OrderId orderId;
    private final List<OrderId> shelfContents;
}
