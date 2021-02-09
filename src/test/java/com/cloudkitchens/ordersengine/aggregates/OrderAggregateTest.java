package com.cloudkitchens.ordersengine.aggregates;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.Temperature;
import com.cloudkitchens.ordersengine.orders.aggregates.OrderAggregate;
import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;
import com.cloudkitchens.ordersengine.orders.domain.events.OrderCreated;

class OrderAggregateTest {

    private FixtureConfiguration<OrderAggregate> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new AggregateTestFixture<>(OrderAggregate.class);
    }

    @Test
    public void createNewOrder() {
        final OrderId orderId = new OrderId("order1");
        final CreateOrder cmd = new CreateOrder(orderId, "pork chops", Temperature.HOT, 300.0D, 1.0D);

        fixture.givenNoPriorActivity()
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(new OrderCreated(orderId, "pork chops", Temperature.HOT, 300.0D, 1.0D));
    }

}
