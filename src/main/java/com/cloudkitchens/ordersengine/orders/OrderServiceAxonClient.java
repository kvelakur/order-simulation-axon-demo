package com.cloudkitchens.ordersengine.orders;

import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;
import com.cloudkitchens.ordersengine.shelves.domain.commands.DeliverOrder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderServiceAxonClient implements OrderService {
    private final CommandGateway commandGateway;

    @Override
    public CompletableFuture<Void> createOrder(final CreateOrder createOrder) {
        return commandGateway.send(createOrder);
    }

    @Override
    public CompletableFuture<Void> deliverOrder(final ShelfType type, final OrderId orderId) {
        final DeliverOrder deliverOrder = new DeliverOrder(type, orderId);
        return commandGateway.send(deliverOrder);
    }
}
