package com.cloudkitchens.enginesim;

import java.util.List;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

import com.cloudkitchens.ordersengine.orders.domain.commands.CreateOrder;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class OrdersSupplierFactory extends AbstractFactoryBean<OrdersSupplier> {
    @Override
    public Class<?> getObjectType() {
        return OrdersSupplier.class;
    }

    @Override
    protected OrdersSupplier createInstance() throws Exception {
        final List<CreateOrder> orders = SimulationUtils.readJson("orders.json", new TypeReference<>() {});
        return () -> orders;
    }
}
