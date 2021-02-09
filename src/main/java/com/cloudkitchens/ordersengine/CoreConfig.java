package com.cloudkitchens.ordersengine;

import javax.persistence.EntityManager;

import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jpa.JpaTokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.serialization.JavaSerializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CoreConfig {
    @Bean
    public DeadlineManager deadlineManager(final org.axonframework.config.Configuration configuration) {
        return new SimpleDeadlineManager(new ConfigurationScopeAwareProvider(configuration));
    }

    @Bean
    public EventStorageEngine eventStore(final EntityManager entityManager,
        final PlatformTransactionManager transactionManager) {
        log.info("Using entity manager {}", entityManager.getClass());
        log.info("Using transaction manager {}", transactionManager.getClass());
        return new JpaEventStorageEngine(new SimpleEntityManagerProvider(entityManager),
            new SpringTransactionManager(transactionManager));

    }

    @Bean
    public TokenStore tokenStore(final EntityManager entityManager) {
        return new JpaTokenStore(new SimpleEntityManagerProvider(entityManager), new JavaSerializer());
    }

    @Autowired
    public void config(final EventProcessingConfiguration configurer, final TokenStore tokenStore) {

        configurer.usingTrackingProcessors();
    }

}
