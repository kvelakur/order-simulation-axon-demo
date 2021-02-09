package com.cloudkitchens.ordersimulation.domain.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.cloudkitchens.ordersimulation.domain.OrderId;
import com.cloudkitchens.ordersimulation.domain.Temperature;

import lombok.Value;

@Value
public class CreateOrder {
    @TargetAggregateIdentifier
    private final OrderId id;
    private final String name;
    private final Temperature temp;
    private final Long shelfLife;
    private final Double decayRate;
}
