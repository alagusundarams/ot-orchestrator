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

/**
 * OpenText API configuration
 */
@Data
class OpentextConfig {
    private String path; // e.g., "/v2/nodes/{nodeId}/categories"
    private String method; // GET, POST, PUT, DELETE
    private boolean requiresAuth; // Whether this endpoint needs authentication
}

/**
 * Request/Response mapping configuration
 */
@Data
class MappingConfig {
    private Map<String, String> input; // JSONPath expressions to extract from caller payload
    private Map<String, String> pathParams; // Path parameter mappings
    private Map<String, String> queryParams; // Query parameter mappings
    private Map<String, String> headers; // Header mappings
}

/**
 * Response transformation configuration
 */
@Data
class ResponseConfig {
    private String type; // json, binary, text
    private String encoding; // base64 (for binary)
    private boolean includeMetadata; // Include response metadata
    private Map<String, String> transform; // Response field transformations
}
