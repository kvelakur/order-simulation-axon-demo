package com.cloudkitchens.enginesim;

import java.util.List;
import java.util.function.Supplier;

import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;

public interface OrdersSupplier extends Supplier<List<CreateOrder>> {
}
