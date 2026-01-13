package com.orchestrator.config.model;

import lombok.Data;
import java.util.Map;

/**
 * Configuration model for a single OpenText endpoint
 */
@Data
public class EndpointConfig {
    private String name;
    private OpentextConfig opentext;
    private MappingConfig mapping;
    private ResponseConfig response;
}
