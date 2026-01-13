package com.orchestrator.config.model;

import lombok.Data;
import java.util.Map;

/**
 * Response transformation configuration
 */
@Data
public class ResponseConfig {
    private String type; // json, binary, text
    private String encoding; // base64 (for binary)
    private boolean includeMetadata; // Include response metadata
    private Map<String, String> transform; // Response field transformations
}
