package com.cloudkitchens.enginesim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.orders.OrderService;
import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;
import com.cloudkitchens.ordersengine.shelves.ShelfInitializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Simulation {
    private static final Random RANDOM = new Random();
    private final OrdersSupplier ordersSupplier;
    private final OrderService orderService;
    private final ShelfInitializer shelfInitializer;
    private final SimulationConfig config;

    public void startSimulation() throws InterruptedException, ExecutionException {
        // Initialize the shelves
        shelfInitializer.initializeShelves();

        final List<CreateOrder> createCommands = ordersSupplier.get();
        final int batchSize = config.getPerSecIngestRate();
        log.info("{} orders will be ingested per second", batchSize);
        final List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        for (int i = 0; i < createCommands.size(); i = i + batchSize) {
            for (int j = i; j < i + batchSize && j < createCommands.size(); j++) {
                final CreateOrder order = createCommands.get(j);
                final long orderDelayMillis = (j / batchSize) * 1000L;
                final long deliveryDelayMillis = orderDelayMillis + 2000L + (long) (RANDOM.nextDouble() * 4000L);
                allFutures.add(placeOrder(order, orderDelayMillis));
                allFutures.add(requestDelivery(ShelfType.fromTemperature(order.getTemp()),
                    order.getOrderId(),
                    deliveryDelayMillis));
            }
        }

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).get();
    }

    private <T> CompletableFuture<T> getDelayedFuture(final long delayMillis, final T toReturn) {
        final Executor delayed = CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS);
        return CompletableFuture.supplyAsync(() -> toReturn, delayed);
    }

    private CompletableFuture<Void> placeOrder(final CreateOrder order, final long orderDelayMillis) {
        return getDelayedFuture(orderDelayMillis, order)
            .thenCompose(orderService::createOrder);
    }

    private CompletableFuture<Void> requestDelivery(final ShelfType type, final OrderId orderId,
        final long deliveryDelayMillis) {
        return getDelayedFuture(deliveryDelayMillis, orderId)
            .thenCompose(id -> {
                try {
                    return CompletableFuture.allOf(orderService.deliverOrder(type, orderId),
                            orderService.deliverOrder(ShelfType.OVERFLOW, orderId));
                } catch (final Exception e) {
                    return null;
                }
            });
    }
}
