package com.cloudkitchens.ordersengine.domain;

import lombok.Getter;

@Getter
public class OverflowItemTypeNotFound extends RuntimeException {
    private static final long serialVersionUID = 8000605359227892506L;
    private final Temperature temperature;

    public OverflowItemTypeNotFound(final Temperature temperature) {
        super("Overflow shelf does not contain item of temperature " + temperature);
        this.temperature = temperature;
    }
}
