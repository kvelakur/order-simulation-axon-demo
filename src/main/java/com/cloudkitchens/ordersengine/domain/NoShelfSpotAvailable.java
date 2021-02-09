package com.cloudkitchens.ordersengine.domain;

import lombok.Getter;

@Getter
public class NoShelfSpotAvailable extends RuntimeException {
    private static final long serialVersionUID = 122079457368883794L;
    private final ShelfType shelfType;

    public NoShelfSpotAvailable(final ShelfType s) {
        super(String.format("A spot on %s shelf could not be saved for", s));
        shelfType = s;
    }

}
