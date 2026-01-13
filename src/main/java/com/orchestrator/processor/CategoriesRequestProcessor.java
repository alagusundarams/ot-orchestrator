package com.orchestrator.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Processor to prepare GET request to categories endpoint with token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoriesRequestProcessor implements Processor {

    private final ObjectMapper objectMapper;

    @Value("${orchestrator.target.endpoint}")
    private String targetEndpoint;

    @Override
    public void process(Exchange exchange) throws Exception {
        String token = exchange.getProperty("authToken", String.class);
        String requestBody = exchange.getProperty("originalRequest", String.class);
        JsonNode requestJson = objectMapper.readTree(requestBody);

        // Get required 'id' parameter
        if (!requestJson.has("id")) {
            throw new IllegalArgumentException("Required parameter 'id' is missing");
        }

        String nodeId = requestJson.get("id").asText();
        if (nodeId.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'id' cannot be empty");
        }

        // Get optional query parameters
        String metadata = requestJson.has("metadata") ? requestJson.get("metadata").asText() : null;
        String supperResponseCodes = requestJson.has("supper_response_codes")
                ? requestJson.get("supper_response_codes").asText()
                : null;

        // Build query string
        StringBuilder queryString = new StringBuilder();
        if (metadata != null && !metadata.isEmpty()) {
            queryString.append("metadata=").append(URLEncoder.encode(metadata, StandardCharsets.UTF_8));
        }
        if (supperResponseCodes != null && !supperResponseCodes.isEmpty()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append("supper_response_codes=")
                    .append(URLEncoder.encode(supperResponseCodes, StandardCharsets.UTF_8));
        }

        // Build full URL
        String baseUrl = targetEndpoint.endsWith("/") ? targetEndpoint : targetEndpoint + "/";
        String fullUrl = baseUrl + "v2/nodes/" + nodeId + "/categories";
        if (queryString.length() > 0) {
            fullUrl += "?" + queryString.toString();
        }

        // Set headers for GET request
        exchange.getIn().setHeader("OTCSTicket", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        exchange.getIn().setHeader("Accept", "application/json");
        exchange.getIn().setBody(null); // GET request has no body

        // Store URL for dynamic routing
        exchange.setProperty("targetUrl", fullUrl);

        log.info("Prepared GET request to: {}", fullUrl);
    }
}
