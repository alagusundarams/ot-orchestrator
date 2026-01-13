package com.orchestrator.config.model;

import lombok.Data;

/**
 * OpenText API configuration
 */
@Data
public class OpentextConfig {
    private String path; // e.g., "/v2/nodes/{nodeId}/categories"
    private String method; // GET, POST, PUT, DELETE
    private boolean requiresAuth; // Whether this endpoint needs authentication
}
