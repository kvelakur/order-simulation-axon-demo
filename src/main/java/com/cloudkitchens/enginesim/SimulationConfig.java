package com.cloudkitchens.enginesim;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "ordersim", ignoreUnknownFields = false)
@Data
public class SimulationConfig {
    private int perSecIngestRate = 2;
}
