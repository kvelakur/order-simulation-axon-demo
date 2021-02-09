package com.cloudkitchens.ordersimulation.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Temperature {
    HOT("hot"), COLD("cold"), FROZEN("frozen");

    @Getter
    private final String value;
}
