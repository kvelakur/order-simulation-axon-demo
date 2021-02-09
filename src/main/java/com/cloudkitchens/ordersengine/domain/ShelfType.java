package com.cloudkitchens.ordersengine.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ShelfType {
    HOT("hot", 10), COLD("cold", 10), FROZEN("frozen", 10), OVERFLOW("overflow", 20);

    private final String value;
    private final int capacity;

    public static ShelfType fromTemperature(final Temperature temp) {
        return ShelfType.valueOf(temp.name());
    }

}
