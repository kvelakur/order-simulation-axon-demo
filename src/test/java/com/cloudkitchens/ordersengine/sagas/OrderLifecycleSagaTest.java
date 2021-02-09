package com.cloudkitchens.ordersengine.sagas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudkitchens.ordersengine.domain.NoShelfSpotAvailable;
import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.OverflowItemTypeNotFound;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;
import com.cloudkitchens.ordersengine.orders.OrderLifecycleSaga;
import com.cloudkitchens.ordersengine.orders.domain.events.OrderCreated;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.ReleaseSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.SaveSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageDiscardFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageMoveFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.events.DiscardFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.MoveFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDelivered;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDiscarded;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderShelved;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelveOrderWithDiscardRequested;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelveOrderWithMoveRequested;
import com.cloudkitchens.ordersengine.shelves.domain.events.SpotSavedOnShelf;

class OrderLifecycleSagaTest {

    private SagaTestFixture<OrderLifecycleSaga> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new SagaTestFixture<>(OrderLifecycleSaga.class);
    }

    // 1. Order created for a new HOT item
    // 2. Save a spot on HOT shelf succeeds
    // 3. Place item on saved HOT spot
    @Test
    public void testOrderCreated_placedOnRightShelf() {
        final OrderId orderId = new OrderId("order1");
        final SavedSpotId savedSpotId = new SavedSpotId();

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                return new SpotSavedOnShelf(spot.getShelfType(), savedSpotId);
            }
            if (o instanceof CaptureSavedSpotOnShelf) {
                final var capture = (CaptureSavedSpotOnShelf) o;
                return new OrderShelved(capture.getOrderId(), capture.getSavedSpotId(), ShelfType.HOT, Temperature.HOT,
                    capture.getRemainingValue(), capture.getInitialShelfLife(), capture.getDecayRate(),
                    Arrays.asList(capture.getOrderId()));
            }
            return null;
        });
        fixture.givenNoPriorActivity()
            .whenPublishingA(GenericEventMessage
                .asEventMessage(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D)))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.HOT),
                new CaptureSavedSpotOnShelf(ShelfType.HOT, savedSpotId, orderId, Temperature.HOT, 1.0, 300.0, 1.0))
            .expectPublishedEvents();
    }

    // 1. Order created for a new HOT item
    // 2. Save a spot on HOT shelf fails
    // 3. Save a spot on OVERFLOW shelf succeeds
    // 3. Place item on saved OVERFLOW spot
    @Test
    public void testOrderCreated_placedOnOverflowShelf() {
        final OrderId orderId = new OrderId("order1");
        final SavedSpotId savedSpotId = new SavedSpotId();

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                if (spot.getShelfType().equals(ShelfType.HOT)) {
                    throw new NoShelfSpotAvailable(ShelfType.HOT);
                }
                return new SpotSavedOnShelf(ShelfType.OVERFLOW, savedSpotId);
            }
            if (o instanceof CaptureSavedSpotOnShelf) {
                final var capture = (CaptureSavedSpotOnShelf) o;
                return new OrderShelved(capture.getOrderId(), capture.getSavedSpotId(), ShelfType.OVERFLOW,
                    Temperature.HOT, capture.getRemainingValue(), capture.getInitialShelfLife(),
                    capture.getDecayRate(), Arrays.asList(capture.getOrderId()));
            }
            return null;
        });
        fixture.givenNoPriorActivity()
            .whenPublishingA(GenericEventMessage
                .asEventMessage(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D)))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.HOT),
                new SaveSpotOnShelf(ShelfType.OVERFLOW),
                new CaptureSavedSpotOnShelf(ShelfType.OVERFLOW, savedSpotId, orderId, Temperature.HOT, 1.0, 300.0, 1.0))
            .expectPublishedEvents();
    }

    // 1. Order created for a new HOT item
    // 2. Save a spot on HOT shelf fails
    // 3. Save a spot on OVERFLOW shelf fails
    // 3. Request that an OVERFLOW shelf item be moved to appropriate shelf so HOT item can accommodated
    @Test
    public void testOrderCreated_requestForMoveFromOverflowShelf() {
        final OrderId orderId = new OrderId("order1");

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                throw new NoShelfSpotAvailable(spot.getShelfType());
            }
            return null;
        });
        fixture.givenNoPriorActivity()
            .whenPublishingA(GenericEventMessage
                .asEventMessage(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D)))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.HOT),
                new SaveSpotOnShelf(ShelfType.OVERFLOW))
            .expectPublishedEvents(new ShelveOrderWithMoveRequested(orderId, Temperature.HOT));
    }

    // 1. Requested move from OVERFLOW shelf so HOT item can be accommodated
    // 2. Try saving a spot on COLD shelf, fails. There is no spot on COLD shelf
    // 3. Try saving a spot on FROZEN shelf, succeeds.
    // 4. Try finding a FROZEN item from OVERFLOW, succeeds.
    // 5. Perform the OVERFLOW item -> FROZEN, new HOT item to OVERFLOW swap
    @Test
    public void testMoveFromOverflowShelf_success0() {
        final OrderId orderId = new OrderId("order1");
        final OrderId movedOrderId = new OrderId("movedOrderId");
        final SavedSpotId shelfSpot = new SavedSpotId("shelfSpot");
        final SavedSpotId overflowSpot = new SavedSpotId("overflowSpot");
        final Random random = new Random();
        final double decayRate = random.nextDouble();
        final double shelfLife = random.nextDouble() * 1000;

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                if (spot.getShelfType() == ShelfType.COLD) {
                    throw new NoShelfSpotAvailable(spot.getShelfType());
                }
                return new SpotSavedOnShelf(ShelfType.FROZEN, shelfSpot);
            }
            if (o instanceof StageMoveFromOverflowShelf) {
                final var spot = (StageMoveFromOverflowShelf) o;
                return new MoveFromOverflowShelfStaged(movedOrderId, overflowSpot, spot.getTemperature(), 1.0,
                    shelfLife, decayRate);
            }
            return null;
        });
        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(new ShelveOrderWithMoveRequested(orderId, Temperature.HOT))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.COLD),
                new SaveSpotOnShelf(ShelfType.FROZEN),
                new StageMoveFromOverflowShelf(ShelfType.OVERFLOW, Temperature.FROZEN),
                new CaptureSavedSpotOnShelf(ShelfType.FROZEN, shelfSpot, movedOrderId, Temperature.FROZEN, 1.0,
                    shelfLife,
                    decayRate),
                new CaptureSavedSpotOnShelf(ShelfType.OVERFLOW, overflowSpot, orderId, Temperature.HOT, 1.0, 300.0,
                    1.0))
            .expectPublishedEvents();
    }

    // 1. Requested move from overflow shelf so HOT item can be accommodated
    // 2. Try saving a spot on COLD shelf, succeeds.
    // 3. Try finding a COLD item from OVERFLOW, fails. No OVERFLOW item of type COLD
    // 4. Release saved spot on COLD shelf.
    // 5. Try saving a spot on FROZEN shelf, succeeds.
    // 6. Try finding a FROZEN item from OVERFLOW, succeeds.
    // 7. Perform the overflow item -> FROZEN, new HOT item to OVERFLOW swap
    @Test
    public void testMoveFromOverflowShelf_success1() {
        final OrderId orderId = new OrderId("order1");
        final OrderId movedOrderId = new OrderId("movedOrderId");
        final SavedSpotId shelfSpot = new SavedSpotId("shelfSpot");
        final SavedSpotId overflowSpot = new SavedSpotId("overflowSpot");
        final Random random = new Random();
        final double decayRate = random.nextDouble();
        final double shelfLife = random.nextDouble() * 1000;

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                return new SpotSavedOnShelf(spot.getShelfType(), shelfSpot);
            }
            if (o instanceof StageMoveFromOverflowShelf) {
                final var spot = (StageMoveFromOverflowShelf) o;
                if (spot.getTemperature() == Temperature.COLD) {
                    throw new OverflowItemTypeNotFound(spot.getTemperature());
                }
                return new MoveFromOverflowShelfStaged(movedOrderId, overflowSpot, spot.getTemperature(), 1.0,
                    shelfLife, decayRate);
            }
            return null;
        });
        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(new ShelveOrderWithMoveRequested(orderId, Temperature.HOT))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.COLD),
                new StageMoveFromOverflowShelf(ShelfType.OVERFLOW, Temperature.COLD),
                new ReleaseSavedSpotOnShelf(ShelfType.COLD, shelfSpot),
                new SaveSpotOnShelf(ShelfType.FROZEN),
                new StageMoveFromOverflowShelf(ShelfType.OVERFLOW, Temperature.FROZEN),
                new CaptureSavedSpotOnShelf(ShelfType.FROZEN, shelfSpot, movedOrderId, Temperature.FROZEN, 1.0,
                    shelfLife,
                    decayRate),
                new CaptureSavedSpotOnShelf(ShelfType.OVERFLOW, overflowSpot, orderId, Temperature.HOT, 1.0, 300.0,
                    1.0))
            .expectPublishedEvents();
    }

    // 1. Requested move from OVERFLOW shelf so HOT item can be accommodated
    // 2. Try saving a spot on COLD shelf, fails. There is no spot on COLD shelf
    // 3. Try saving a spot on FROZEN shelf, fails. There is no spot on COLD shelf
    // 4. Try saving a spot on HOT shelf, fails. There is no spot on HOT shelf
    // 5. Request that an item from OVERFLOW be discarded so new HOT item can be accommodated
    @Test
    public void testMoveFromOverflowShelf_fails() {
        final OrderId orderId = new OrderId("order1");

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof SaveSpotOnShelf) {
                final var spot = (SaveSpotOnShelf) o;
                throw new NoShelfSpotAvailable(spot.getShelfType());
            }

            return null;
        });
        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(new ShelveOrderWithMoveRequested(orderId, Temperature.HOT))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new SaveSpotOnShelf(ShelfType.COLD),
                new SaveSpotOnShelf(ShelfType.FROZEN),
                new SaveSpotOnShelf(ShelfType.HOT))
            .expectPublishedEvents(new ShelveOrderWithDiscardRequested(orderId, Temperature.HOT));
    }

    // 1. Requested a discard from OVERFLOW shelf so HOT order order can be accommodated
    // 2. Stage a discard from OVERFLOW, succeeds.
    // 3. Discard the staged order
    // 4. Capture the saved spot on OVERFLOW for new HOT item
    @Test
    public void testDiscardFromOverflow_success0() {
        final OrderId orderId = new OrderId("order1");
        final SavedSpotId savedSpotId = new SavedSpotId("savedSpotId");
        final OrderId toDiscard = new OrderId("toDiscard");

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof StageDiscardFromOverflowShelf) {
                final var spot = (StageDiscardFromOverflowShelf) o;
                return new DiscardFromOverflowShelfStaged(orderId, savedSpotId);
            }
            return null;
        });

        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(new ShelveOrderWithDiscardRequested(orderId, Temperature.HOT))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new StageDiscardFromOverflowShelf(ShelfType.OVERFLOW, orderId),
                new CaptureSavedSpotOnShelf(ShelfType.OVERFLOW, savedSpotId, orderId, Temperature.HOT, 1.0, 300.0, 1.0))
            .expectPublishedEvents();
    }

    // 1. Requested a discard from OVERFLOW shelf so HOT order order can be accommodated
    // 2. Stage a discard from OVERFLOW, succeeds.
    // 3. No item to be discarded, so don't discard anything
    // 4. Capture the saved spot on OVERFLOW for new HOT item
    @Test
    public void testDiscardFromOverflow_success1() {
        final OrderId orderId = new OrderId("order1");
        final SavedSpotId savedSpotId = new SavedSpotId("savedSpotId");

        fixture.setCallbackBehavior((o, metaData) -> {
            if (o instanceof StageDiscardFromOverflowShelf) {
                final var spot = (StageDiscardFromOverflowShelf) o;
                return new DiscardFromOverflowShelfStaged(orderId, savedSpotId);
            }
            return null;
        });

        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(new ShelveOrderWithDiscardRequested(orderId, Temperature.HOT))
            .expectActiveSagas(1)
            .expectDispatchedCommands(new StageDiscardFromOverflowShelf(ShelfType.OVERFLOW, orderId),
                new CaptureSavedSpotOnShelf(ShelfType.OVERFLOW, savedSpotId, orderId, Temperature.HOT, 1.0, 300.0, 1.0))
            .expectPublishedEvents();
    }

    @Test
    public void testOnItemShelved() {
        final OrderId orderId = new OrderId("order1");
        // ToDo: figure matcher for deadline
        fixture.givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .whenPublishingA(
                new OrderShelved(orderId, new SavedSpotId(), ShelfType.HOT, Temperature.HOT, 1.0, 300.0, 1.0,
                    Arrays.asList(orderId)))
            .expectDispatchedCommands()
            .expectPublishedEvents();

    }

    @Test
    public void testOnItemDelivered() {
        final OrderId orderId = new OrderId("order1");
        fixture
            .givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .andThenAPublished(
                new OrderShelved(orderId, new SavedSpotId(), ShelfType.HOT, Temperature.HOT, 1.0, 300.0, 1.0,
                    Arrays.asList(orderId)))
            .whenPublishingA(new OrderDelivered(ShelfType.HOT, orderId, 1.0, new ArrayList<>()))
            .expectDispatchedCommands()
            .expectPublishedEvents()
            .expectNoScheduledDeadlines()
            .expectActiveSagas(0);

    }

    @Test
    public void testOnItemDiscarded() {
        final OrderId orderId = new OrderId("order1");
        fixture
            .givenAPublished(new OrderCreated(orderId, "pork chop", Temperature.HOT, 300.0D, 1.0D))
            .andThenAPublished(
                new OrderShelved(orderId, new SavedSpotId(), ShelfType.OVERFLOW, Temperature.HOT, 1.0, 300.0, 1.0,
                    Arrays.asList(orderId)))
            .whenPublishingA(new OrderDiscarded(ShelfType.OVERFLOW, orderId, new ArrayList<>()))
            .expectDispatchedCommands()
            .expectPublishedEvents()
            .expectNoScheduledDeadlines();
    }

}
