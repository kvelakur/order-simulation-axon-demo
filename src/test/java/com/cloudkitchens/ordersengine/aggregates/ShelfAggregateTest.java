package com.cloudkitchens.ordersengine.aggregates;

import java.util.ArrayList;
import java.util.Arrays;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.axonframework.test.matchers.Matchers;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudkitchens.ordersengine.domain.NoShelfSpotAvailable;
import com.cloudkitchens.ordersengine.domain.OrderId;
import com.cloudkitchens.ordersengine.domain.SavedSpotId;
import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.domain.Temperature;
import com.cloudkitchens.ordersengine.shelves.aggregates.ShelfAggregate;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CaptureSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CreateShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.DeliverOrder;
import com.cloudkitchens.ordersengine.shelves.domain.commands.DiscardOrder;
import com.cloudkitchens.ordersengine.shelves.domain.commands.ReleaseSavedSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.SaveSpotOnShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageDiscardFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.commands.StageMoveFromOverflowShelf;
import com.cloudkitchens.ordersengine.shelves.domain.events.DiscardFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.MoveFromOverflowShelfStaged;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDelivered;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderDiscarded;
import com.cloudkitchens.ordersengine.shelves.domain.events.OrderShelved;
import com.cloudkitchens.ordersengine.shelves.domain.events.SavedSpotOnShelfReleased;
import com.cloudkitchens.ordersengine.shelves.domain.events.ShelfCreated;
import com.cloudkitchens.ordersengine.shelves.domain.events.SpotSavedOnShelf;

class ShelfAggregateTest {
    private FixtureConfiguration<ShelfAggregate> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new AggregateTestFixture<>(ShelfAggregate.class);
    }

    @Test
    public void createNewShelf() {
        final CreateShelf cmd = new CreateShelf(ShelfType.COLD, 10);

        fixture.givenNoPriorActivity()
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(new ShelfCreated(ShelfType.COLD, 10));
    }

    @Test
    public void createNewShelf_zeroCapacity() {
        final CreateShelf cmd = new CreateShelf(ShelfType.COLD, 0);

        fixture.givenNoPriorActivity()
            .when(cmd)
            .expectException(IllegalArgumentException.class);
    }

    @Test
    public void saveSpotOnShelf() {
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final SaveSpotOnShelf saveCmd = new SaveSpotOnShelf(ShelfType.COLD);

        fixture.given(createdEvent)
            .when(saveCmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(SpotSavedOnShelf.class))));
    }

    @Test
    public void saveSpotWhenShelfFull() {
        final OrderId orderId = new OrderId("order0");
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final OrderShelved shelvedEvent = new OrderShelved(orderId, new SavedSpotId(), ShelfType.COLD,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(ShelfType.COLD, new SavedSpotId());
        final SaveSpotOnShelf saveCmd = new SaveSpotOnShelf(ShelfType.COLD);

        fixture.given(createdEvent)
            .andGiven(shelvedEvent)
            .andGiven(savedEvent)
            .when(saveCmd)
            .expectException(NoShelfSpotAvailable.class);

    }

    @Test
    public void captureSavedSpot() {
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(ShelfType.COLD, new SavedSpotId());
        final CaptureSavedSpotOnShelf cmd = new CaptureSavedSpotOnShelf(ShelfType.COLD, savedEvent.getSavedSpotId(),
            orderId, Temperature.COLD, 1.0, 300.0, 1.0);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(new OrderShelved(orderId, savedEvent.getSavedSpotId(), ShelfType.COLD, Temperature.COLD, 1.0,
                300.0, 1.0, Arrays.asList(orderId)));
    }

    @Test
    public void captureUnsavedSavedSpot() {
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(ShelfType.COLD, new SavedSpotId());
        final CaptureSavedSpotOnShelf cmd = new CaptureSavedSpotOnShelf(ShelfType.COLD, new SavedSpotId(), orderId,
            Temperature.COLD, 1.0, 300.0, 1.0);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .when(cmd)
            .expectException(IllegalArgumentException.class);

    }

    @Test
    public void captureSpotOnWrongShelf() {
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(ShelfType.COLD, new SavedSpotId());
        final CaptureSavedSpotOnShelf cmd = new CaptureSavedSpotOnShelf(ShelfType.COLD, savedEvent.getSavedSpotId(),
            orderId, Temperature.HOT, 1.0, 300.0, 1.0);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .when(cmd)
            .expectException(IllegalArgumentException.class);

    }

    @Test
    public void releaseSavedSpot() {
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(ShelfType.COLD, new SavedSpotId());
        final ReleaseSavedSpotOnShelf cmd = new ReleaseSavedSpotOnShelf(ShelfType.COLD, savedEvent.getSavedSpotId());
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(new SavedSpotOnShelfReleased(ShelfType.COLD, savedEvent.getSavedSpotId()));
    }

    @Test
    public void releaseSavedSpot_whenNotReallySaved() {
        final ShelfCreated createdEvent = new ShelfCreated(ShelfType.COLD, 2);
        final ReleaseSavedSpotOnShelf cmd = new ReleaseSavedSpotOnShelf(ShelfType.COLD, new SavedSpotId());
        fixture.given(createdEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectNoEvents();
    }

    @Test
    public void moveFromOverflowShelf_stage() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));

        final StageMoveFromOverflowShelf cmd = new StageMoveFromOverflowShelf(type, Temperature.COLD);

        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(MoveFromOverflowShelfStaged.class))));
    }

    @Test
    public void moveFromNonOverflowShelf() {
        final ShelfType type = ShelfType.COLD;
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);

        final StageMoveFromOverflowShelf cmd = new StageMoveFromOverflowShelf(type, Temperature.COLD);

        fixture.given(createdEvent)
            .when(cmd)
            .expectException(IllegalArgumentException.class)
            .expectNoEvents();
    }

    @Test
    public void moveFromOverflowShelf_stageAndCapture() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));
        final MoveFromOverflowShelfStaged stagedEvent = new MoveFromOverflowShelfStaged(orderId,
            savedEvent.getSavedSpotId(), Temperature.COLD, 1.0, 300.0, 1.0);

        final OrderId orderIdToPlace = new OrderId("order2");
        final CaptureSavedSpotOnShelf captureCmd = new CaptureSavedSpotOnShelf(type, stagedEvent.getSavedSpotId(),
            orderIdToPlace, Temperature.HOT, 0.5, 200.0, 0.5);

        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .andGiven(stagedEvent)
            .when(captureCmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(
                new OrderShelved(orderIdToPlace, stagedEvent.getSavedSpotId(), type, Temperature.HOT, 0.5, 200.0, 0.5,
                    Arrays.asList(orderIdToPlace)));
    }

    @Test
    public void discardFromOverflowShelf() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedCmd = new OrderShelved(orderId, savedEvent.getSavedSpotId(), ShelfType.COLD,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));
        final StageDiscardFromOverflowShelf cmd = new StageDiscardFromOverflowShelf(type, new OrderId("order2"));
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedCmd)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(DiscardFromOverflowShelfStaged.class))))
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(OrderDiscarded.class))));
    }

    @Test
    public void discardFromNonOverflowShelf() {
        final ShelfType type = ShelfType.COLD;
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);
        final StageDiscardFromOverflowShelf cmd = new StageDiscardFromOverflowShelf(type, new OrderId("order2"));
        fixture.given(createdEvent)
            .when(cmd)
            .expectException(IllegalArgumentException.class);
    }

    @Test
    public void discardFromOverflowShelf_whenEmpty() {
        final ShelfType type = ShelfType.OVERFLOW;
        final ShelfCreated createdEvent = new ShelfCreated(type, 2);
        final StageDiscardFromOverflowShelf cmd = new StageDiscardFromOverflowShelf(type, new OrderId("order2"));
        fixture.given(createdEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(DiscardFromOverflowShelfStaged.class))));
    }

    @Test
    public void orderDiscarded_releasesSpotOnShelf() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));
        final OrderDiscarded discardedEvent = new OrderDiscarded(ShelfType.OVERFLOW, orderId, new ArrayList<>());
        final SaveSpotOnShelf cmd = new SaveSpotOnShelf(type);

        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .andGiven(discardedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(SpotSavedOnShelf.class))));
    }

    @Test
    public void discardOrder() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));

        final DiscardOrder cmd = new DiscardOrder(type, orderId);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEvents(new OrderDiscarded(type, orderId, new ArrayList<>()));
    }

    @Test
    public void discardOrder_notPresent() {
        final ShelfType type = ShelfType.OVERFLOW;
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);

        final DiscardOrder cmd = new DiscardOrder(type, new OrderId("orderId"));
        fixture.given(createdEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectNoEvents();
    }

    @Test
    public void deliverOrder() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 1.0, 300.0, 1.0, Arrays.asList(orderId));

        final DeliverOrder cmd = new DeliverOrder(type, orderId);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectEventsMatching(
                Matchers.payloadsMatching(Matchers.sequenceOf(CoreMatchers.isA(OrderDelivered.class))));
    }

    @Test
    public void deliverOrder_zeroValue() {
        final ShelfType type = ShelfType.OVERFLOW;
        final OrderId orderId = new OrderId("order1");
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);
        final SpotSavedOnShelf savedEvent = new SpotSavedOnShelf(type, new SavedSpotId());
        final OrderShelved shelvedEvent = new OrderShelved(orderId, savedEvent.getSavedSpotId(), type,
            Temperature.COLD, 0.0, 300.0, 1.0, Arrays.asList(orderId));

        final DeliverOrder cmd = new DeliverOrder(type, orderId);
        fixture.given(createdEvent)
            .andGiven(savedEvent)
            .andGiven(shelvedEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectNoEvents();
    }

    @Test
    public void deliverOrder_notPresent() {
        final ShelfType type = ShelfType.OVERFLOW;
        final ShelfCreated createdEvent = new ShelfCreated(type, 1);

        final DeliverOrder cmd = new DeliverOrder(type, new OrderId("orderId"));
        fixture.given(createdEvent)
            .when(cmd)
            .expectSuccessfulHandlerExecution()
            .expectNoEvents();
    }

}
