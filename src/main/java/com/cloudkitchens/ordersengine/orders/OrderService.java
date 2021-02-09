package com.cloudkitchens.ordersengine.orders;

import java.util.concurrent.CompletableFuture;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;

public interface OrderService {
    CompletableFuture<Void> createOrder(CreateOrder createOrder);

    CompletableFuture<Void> deliverOrder(ShelfType type, OrderId orderId);
}
