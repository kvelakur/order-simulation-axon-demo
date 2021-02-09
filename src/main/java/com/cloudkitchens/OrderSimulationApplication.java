package com.cloudkitchens;

import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.cloudkitchens.enginesim.Simulation;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class OrderSimulationApplication {

    public static void main(final String[] args) {
        SpringApplication.run(OrderSimulationApplication.class, args);
    }

    @Bean
    @Profile("!testing")
    public ApplicationRunner applicationRunner(@Autowired final Simulation simulation,
        @Autowired final EventStore eventStore,
        @Autowired final EventStorageEngine eventStorageEngine,
        @Autowired final TransactionManager transactionManager) {
        return args -> {
            log.info("Using Event Store {}", eventStore.getClass());
            log.info("Using Event Storage Engine {}", eventStorageEngine.getClass());
            log.info("Using Transaction Manager {}", transactionManager.getClass());
            log.info("Started order-simulation");
            simulation.startSimulation();
        };
    }

}
