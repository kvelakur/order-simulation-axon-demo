## Axon demo: order simulation

Axon Framework demo that features multiple aggregates, and a saga that handles interaction between two aggregates. It
uses an in-memory H2 database as the Axon event store.

### Description of the application:

The application simulates order fulfillment and delivery for a kitchen. The kitchen receives a `n` (a configurable
number) orders per second. Upon receiving the order, it should be placed on a matching shelf based on the rules in
Shelves section. The delivery should be requested at a random time 2-6 seconds after the order has been created.

#### Orders

Orders have an associated unique `id`, temperature type (one of hot, cold or frozen), a shelf life in seconds, and a
decay rate.

```json
{
  "id": "972aa5b8-5d83-4d5e-8cf3-8a1a1437b18a",
  "name": "Chocolate Gelato",
  "temp": "frozen",
  "shelfLife": 300,
  "decayRate": 0.61
}
```

#### Shelves

Orders have to be placed on shelves corresponding to their temperature: hot, cold, frozen. In addition, there is a
overflow shelf where an order of any temperature can be placed. The capacities of the shelves are as follows:

| Name   |      Allowable Temperature      |  Capacity |
|----------|:-------------:|------:|
| Hot shelf |  hot | 10 |
| Cold shelf |    cold   |   10 |
| Frozen shelf | frozen |    10 |
| Overflow shelf | Any |    20 |

Rules for placing order of shelf:

1. An order should be placed on a shelf that matches it temperature
2. If that shelf is full the order can be placed on the overflow shelf
3. If the overflow is full, an existing order on the overflow shelf can moved to an allowable shelf with roon
4. If no such move is possible, a random order from overflow shelf should be discarded as waste.

#### Shelf life and order value

Orders have a value that will eventually decay to 0.

Current value of the order is calculated as

```
value = (shelfLife - orderAge - decayRate*orderAge*shelfDecayModifier)/shelfLife
```

where `shelfDecayModifier` is 1.0 for single-temperature shelves and 2.0 for overflow shelf. When order value reaches 0,
it should discarded. Zero value orders should never be delivered.

### Instruction to run code in a docker container

1. Build and execute code in docker container using:

```shell
docker-compose up
```

2. Ingestion rate can be modified in `docker-compose.yml`

### Instruction to run code

1. Set JAVA_HOME environment variable to a JDK11 directory.
2. Build code using

```shell
./mvnw clean package
```

3. Run code using

```shell
java -jar target/order-simulation-0.0.1-SNAPSHOT.jar
```

4. Modify ingestion rate in `src/main/resources/application.yml`

### Description of how orders are moved

The placement of an order on a shelf is orchestrated using a [saga](https://www.youtube.com/watch?v=xDuwrtwYHu8). A
description of the events that drive the placement:

1. First, we attempt to place order on regular shelf.

`OrderCreated -> SpotSavedOnShelf(regular) -> OrderShelved`

2. If that fails, we attempt to place order on overflow shelf.

`OrderCreated -> SpotSavedOnShelf(overflow) -> OrderShelved`

3. If both 1 and 2 fail, we request that an item on overflow shelf be moved so order can be accommodated.

`ShelveOrderWithMoveRequested`

4. For each non-overflow shelf type t, we try to save a spot and find an order on overflow that can fill it. The empty
   spot is now available for the order.

`ShelveOrderWithMoveRequested -> SpotSavedOnShelf(t) -> MoveFromOverflowShelfStaged(t) -> OrderShelved, OrderShelved`

5. If any of the intermediate steps in 4 fail, we reverse them. If order still couldn't be shelved, we then request that
   an item from overflow shelf be discarded to make room for order.

`ShelveOrderWithDiscardRequested`

6. Stage removal from overflow shelf, and save the spot for order.

`ShelveOrderWithDiscardRequested -> DiscardFromOverflowShelfStaged -> OrderDiscarded, OrderShelved`
