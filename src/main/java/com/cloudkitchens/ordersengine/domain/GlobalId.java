package com.cloudkitchens.ordersengine.domain;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public abstract class GlobalId implements Serializable, Comparable<OrderId> {
    private static final long serialVersionUID = -8721481378795951598L;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String value;

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(final OrderId o) {
        return getValue().compareTo(o.getValue());
    }
}
