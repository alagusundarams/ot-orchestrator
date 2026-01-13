package com.orchestrator.processor.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orchestrator.config.model.EndpointConfig;
import com.orchestrator.config.model.ResponseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;

/**
 * Dynamically transforms OpenText API response based on configuration
 * Handles JSON, binary (base64), and text responses
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicResponseTransformer implements Processor {

    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        EndpointConfig config = exchange.getProperty("endpointConfig", EndpointConfig.class);
        ResponseConfig responseConfig = config.getResponse();

        log.info("Transforming response for endpoint: {}", config.getName());

        if (responseConfig == null) {
            // No transformation needed, return as-is
            return;
        }

        String responseType = responseConfig.getType();

        if ("binary".equalsIgnoreCase(responseType)) {
            transformBinaryResponse(exchange, responseConfig);
        } else if ("json".equalsIgnoreCase(responseType)) {
            transformJsonResponse(exchange, responseConfig);
        } else {
            // Default: pass through
            log.debug("No transformation for response type: {}", responseType);
        }

        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }

    /**
     * Transform binary response to base64 with metadata
     */
    private void transformBinaryResponse(Exchange exchange, ResponseConfig config) throws Exception {
        byte[] binaryContent = exchange.getIn().getBody(byte[].class);

        if (binaryContent == null || binaryContent.length == 0) {
            throw new IllegalStateException("No binary content received");
        }

        log.info("Transforming binary response, size: {} bytes", binaryContent.length);

        // Encode to base64
        String base64Content = Base64.getEncoder().encodeToString(binaryContent);

        // Build response
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");

        if (config.isIncludeMetadata()) {
            // Extract metadata from headers
            String contentType = exchange.getIn().getHeader("Content-Type", String.class);
            String contentDisposition = exchange.getIn().getHeader("Content-Disposition", String.class);
            String nodeId = exchange.getProperty("nodeId", String.class);

            String fileName = extractFileName(contentDisposition);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "node_" + nodeId;
            }

            response.put("nodeId", nodeId);
            response.put("fileName", fileName);
            response.put("contentType", contentType != null ? contentType : "application/octet-stream");
            response.put("sizeBytes", binaryContent.length);
        }

        response.put("base64Content", base64Content);
        response.put("timestamp", Instant.now().toString());

        exchange.getIn().setBody(objectMapper.writeValueAsString(response));
        log.info("Binary response transformed successfully");
    }

    /**
     * Transform JSON response with optional field mappings
     */
    private void transformJsonResponse(Exchange exchange, ResponseConfig config) throws Exception {
        String responseBody = exchange.getIn().getBody(String.class);
        JsonNode originalResponse = objectMapper.readTree(responseBody);

        ObjectNode transformedResponse = objectMapper.createObjectNode();

        // Apply field transformations if configured
        if (config.getTransform() != null && !config.getTransform().isEmpty()) {
            for (var entry : config.getTransform().entrySet()) {
                String targetField = entry.getKey();
                String sourceValue = entry.getValue();

                // Simple string values or JSONPath could be supported here
                transformedResponse.put(targetField, sourceValue);
            }
        }

        // Include original data
        transformedResponse.set("data", originalResponse);
        transformedResponse.put("timestamp", Instant.now().toString());

        exchange.getIn().setBody(objectMapper.writeValueAsString(transformedResponse));
        log.info("JSON response transformed successfully");
    }

    /**
     * Extract filename from Content-Disposition header
     */
    private String extractFileName(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }

        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=") || part.startsWith("filename*=")) {
                String fileName = part.substring(part.indexOf('=') + 1).trim();
                return fileName.replaceAll("^\"|\"$", "");
            }
        }

        return null;
    }
}
