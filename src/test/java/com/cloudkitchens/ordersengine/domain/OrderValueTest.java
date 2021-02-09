package com.cloudkitchens.ordersengine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OrderValueTest {

    @Test
    public void getCurrentValue_nonOverflow() {
        final Instant createdAt = Instant.ofEpochSecond(0);
        final double shelfLife = 10.0D;
        final OrderValue value = new OrderValue(createdAt, shelfLife, 1.0D, ShelfType.HOT);

        assertEquals(shelfLife, value.getRemainingShelfLife(Instant.ofEpochMilli(0)));
        // At 2.5s, value = (10 - 2.5 -2.5)/10 = 0.5. So, half shelfLife is remaining.
        assertEquals(5.0D, value.getRemainingShelfLife(Instant.ofEpochMilli(2500)));

        assertEquals(Instant.ofEpochMilli(5000L), value.getZeroTime(Instant.ofEpochSecond(0)));
    }

    @Test
    public void getCurrentValue_overflow() {
        final Instant createdAt = Instant.ofEpochSecond(0);
        final double shelfLife = 10.0D;
        final OrderValue value = new OrderValue(createdAt, shelfLife, 1.0D, ShelfType.OVERFLOW);

        assertEquals(shelfLife, value.getRemainingShelfLife(Instant.ofEpochMilli(0)));
        // At 2.5s, value = (10 - 2.5 - 5)/10 = 0.25. So, quarter shelfLife is remaining.
        assertEquals(2.5D, value.getRemainingShelfLife(Instant.ofEpochMilli(2500)));

        assertEquals(Instant.ofEpochMilli(3333L), value.getZeroTime(Instant.ofEpochSecond(0)));
    }
}
