package com.cloudkitchens.ordersengine.orders.domain.commands;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.Temperature;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class CreateOrder {
    @TargetAggregateIdentifier
    private OrderId orderId;
    private String name;
    private Temperature temp;
    private Double shelfLife;
    private Double decayRate;

    @JsonCreator
    public CreateOrder(@JsonProperty("id") final OrderId orderId, @JsonProperty("name") final String name,
        @JsonProperty("temp") final Temperature temp, @JsonProperty("shelfLife") final Double shelfLife,
        @JsonProperty("decayRate") final Double decayRate) {
        this.orderId = orderId;
        this.name = name;
        this.temp = temp;
        this.shelfLife = shelfLife;
        this.decayRate = decayRate;
    }
}
