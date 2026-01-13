package com.orchestrator.config.model;

import lombok.Data;
import java.util.Map;

/**
 * Request/Response mapping configuration
 */
@Data
public class MappingConfig {
    private Map<String, String> input; // JSONPath expressions to extract from caller payload
    private Map<String, String> pathParams; // Path parameter mappings
    private Map<String, String> queryParams; // Query parameter mappings
    private Map<String, String> headers; // Header mappings
}
