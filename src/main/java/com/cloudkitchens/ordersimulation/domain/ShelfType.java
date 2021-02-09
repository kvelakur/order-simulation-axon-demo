package com.cloudkitchens.ordersimulation.domain;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ShelfType {
    HOT("hot"), COLD("cold"), FROZEN("frozen"), OVERFLOW("overflow");

    private final String value;

    public static ShelfType fromTemperature(final Temperature temp) {
        return ShelfType.valueOf(temp.getValue());
    }
}
