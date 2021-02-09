package com.cloudkitchens.ordersengine.domain;

import java.time.Instant;

public class OrderValue {
    private static final double MILLIS = 1000D;

    private final Instant shelvedAt;
    private final Double remainingShelfLife;
    private final Double decayRate;
    private final ShelfType shelfType;

    public OrderValue(final Instant shelvedAt, final Double remainingShelfLife, final Double decayRate,
        final ShelfType shelfType) {
        this.shelvedAt = shelvedAt;
        this.remainingShelfLife = remainingShelfLife;
        this.decayRate = decayRate;
        this.shelfType = shelfType;
    }

    private double getElapsedSeconds(final Instant now) {
        return (now.toEpochMilli() - shelvedAt.toEpochMilli()) / MILLIS;
    }

    private double getDecayModifier() {
        return shelfType.equals(ShelfType.OVERFLOW) ? 2.0D : 1.0D;
    }

    public double getRemainingShelfLife(final Instant now) {
        return remainingShelfLife * getCurrentValue(now);
    }

    public double getCurrentValue(final Instant now) {
        final double ageOnShelf = getElapsedSeconds(now);
        final double decayModifier = getDecayModifier();
        return (remainingShelfLife - ageOnShelf - decayRate * ageOnShelf * decayModifier) / remainingShelfLife;
    }

    public Instant getZeroTime(final Instant now) {
        final double secsToZero = remainingShelfLife / (1 + decayRate * getDecayModifier());
        return now.plusMillis((long) (secsToZero * MILLIS));
    }
}
