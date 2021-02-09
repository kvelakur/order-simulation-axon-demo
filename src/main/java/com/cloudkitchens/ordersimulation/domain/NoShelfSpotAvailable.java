package com.cloudkitchens.ordersimulation.domain;

import lombok.Getter;

@Getter
public class NoShelfSpotAvailable extends RuntimeException {
    private static final long serialVersionUID = 122079457368883794L;
    private final ShelfType shelfType;
    private final OrderId orderId;

    public NoShelfSpotAvailable(final ShelfType s, final OrderId o) {
        super(String.format("A spot on %s shelf could not be saved for %s", s, o));
        shelfType = s;
        orderId = o;
    }

}
