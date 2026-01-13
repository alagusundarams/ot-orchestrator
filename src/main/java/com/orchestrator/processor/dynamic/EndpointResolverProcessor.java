package com.orchestrator.processor.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.orchestrator.config.EndpointConfigLoader;
import com.orchestrator.config.model.EndpointConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves endpoint configuration based on request
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointResolverProcessor implements Processor {

    private final EndpointConfigLoader configLoader;
    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        // Get endpoint name from path parameter
        String endpointName = exchange.getIn().getHeader("endpointName", String.class);

        if (endpointName == null || endpointName.isEmpty()) {
            throw new IllegalArgumentException("Endpoint name is required");
        }

        log.info("Resolving endpoint configuration for: {}", endpointName);

        // Load endpoint configuration
        EndpointConfig config = configLoader.getEndpoint(endpointName);

        // Store configuration in exchange properties
        exchange.setProperty("endpointConfig", config);
        exchange.setProperty("requiresAuth", config.getOpentext().isRequiresAuth());
        exchange.setProperty("endpointName", endpointName);

        log.debug("Endpoint configuration resolved: method={}, path={}, requiresAuth={}",
                config.getOpentext().getMethod(),
                config.getOpentext().getPath(),
                config.getOpentext().isRequiresAuth());
    }
}
