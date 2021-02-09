package com.cloudkitchens.ordersengine.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Temperature {
    HOT("hot"), COLD("cold"), FROZEN("frozen");

    @JsonValue
    private final String value;
}
