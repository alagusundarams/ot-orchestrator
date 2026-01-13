package com.orchestrator.config;

import com.orchestrator.config.model.EndpointConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and manages endpoint configurations from YAML file
 */
@Slf4j
@Configuration
public class EndpointConfigLoader {

    @Value("classpath:endpoints.yml")
    private Resource endpointsResource;

    @Getter
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    @PostConstruct
    public void loadConfigurations() {
        try (InputStream inputStream = endpointsResource.getInputStream()) {
            Yaml yaml = new Yaml();
            EndpointConfigWrapper wrapper = yaml.loadAs(inputStream, EndpointConfigWrapper.class);

            if (wrapper != null && wrapper.getEndpoints() != null) {
                for (EndpointConfig config : wrapper.getEndpoints()) {
                    endpoints.put(config.getName(), config);
                    log.info("Loaded endpoint configuration: {}", config.getName());
                }
                log.info("Successfully loaded {} endpoint configurations", endpoints.size());
            } else {
                log.warn("No endpoint configurations found in endpoints.yml");
            }
        } catch (Exception e) {
            log.error("Failed to load endpoint configurations", e);
            throw new RuntimeException("Failed to load endpoint configurations", e);
        }
    }

    /**
     * Get endpoint configuration by name
     */
    public EndpointConfig getEndpoint(String name) {
        EndpointConfig config = endpoints.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + name);
        }
        return config;
    }

    /**
     * Check if endpoint exists
     */
    public boolean hasEndpoint(String name) {
        return endpoints.containsKey(name);
    }

    /**
     * Wrapper class for YAML deserialization
     */
    @Getter
    public static class EndpointConfigWrapper {
        private List<EndpointConfig> endpoints;

        public void setEndpoints(List<EndpointConfig> endpoints) {
            this.endpoints = endpoints;
        }
    }
}
