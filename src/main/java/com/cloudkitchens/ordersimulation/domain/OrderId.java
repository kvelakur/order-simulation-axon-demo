package com.cloudkitchens.ordersimulation.domain;

import java.io.Serializable;

import lombok.Getter;
import lombok.Value;

@Value
public class OrderId implements Serializable, Comparable<OrderId> {
    @Getter
    private static final long serialVersionUID = 7674933037319867095L;
    private final String value;

    @Override
    public int compareTo(final OrderId o) {
        return getValue().compareTo(o.getValue());
    }
}
