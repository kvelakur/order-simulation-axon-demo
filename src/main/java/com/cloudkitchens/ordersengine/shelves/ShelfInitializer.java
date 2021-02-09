package com.cloudkitchens.ordersengine.shelves;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cloudkitchens.ordersengine.domain.ShelfType;
import com.cloudkitchens.ordersengine.shelves.domain.commands.CreateShelf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShelfInitializer {

    private final CommandGateway commandGateway;

    public void initializeShelves() {
        for (final ShelfType type : ShelfType.values()) {
            commandGateway.sendAndWait(new CreateShelf(type, type.getCapacity()));
            log.info("Created shelf {} with capacity {}", type, type.getCapacity());
        }
    }

}
