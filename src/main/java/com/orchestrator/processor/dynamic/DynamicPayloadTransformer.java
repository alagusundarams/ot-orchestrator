package com.orchestrator.processor.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.orchestrator.config.model.EndpointConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamically transforms caller payload to OpenText API format
 * Uses JSONPath to extract fields and builds URL, query params, and headers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicPayloadTransformer implements Processor {

    private final ObjectMapper objectMapper;

    @Value("${orchestrator.target.endpoint}")
    private String targetEndpoint;

    @Override
    public void process(Exchange exchange) throws Exception {
        EndpointConfig config = exchange.getProperty("endpointConfig", EndpointConfig.class);
        String requestBody = exchange.getProperty("originalRequest", String.class);
        String authToken = exchange.getProperty("authToken", String.class);

        log.info("Transforming payload for endpoint: {}", config.getName());

        // Parse input JSON
        JsonNode inputJson = objectMapper.readTree(requestBody);

        // Extract values using JSONPath
        Map<String, Object> extractedValues = extractValues(inputJson, config.getMapping().getInput());

        // Add auth token if available
        if (authToken != null) {
            extractedValues.put("authToken", authToken);
        }

        // Build URL with path parameters
        String url = buildUrl(config.getOpentext().getPath(), extractedValues);

        // Build query string
        String queryString = buildQueryString(config.getMapping().getQueryParams(), extractedValues);

        // Set headers
        setHeaders(exchange, config.getMapping().getHeaders(), extractedValues);

        // Set HTTP method
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, config.getOpentext().getMethod());

        // Build full URL
        String fullUrl = targetEndpoint + url + (queryString.isEmpty() ? "" : "?" + queryString);
        exchange.setProperty("targetUrl", fullUrl);

        // Clear body for GET requests
        if ("GET".equalsIgnoreCase(config.getOpentext().getMethod())) {
            exchange.getIn().setBody(null);
        }

        log.info("Transformed URL: {}", fullUrl);
        log.debug("Extracted values: {}", extractedValues);
    }

    /**
     * Extract values from input JSON using JSONPath expressions
     */
    private Map<String, Object> extractValues(JsonNode inputJson, Map<String, String> mappings) {
        Map<String, Object> extracted = new HashMap<>();

        if (mappings == null) {
            return extracted;
        }

        String jsonString = inputJson.toString();

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String key = entry.getKey();
            String jsonPath = entry.getValue();

            try {
                Object value = JsonPath.read(jsonString, jsonPath);
                extracted.put(key, value);
                log.debug("Extracted {}: {} from path: {}", key, value, jsonPath);
            } catch (Exception e) {
                log.warn("Failed to extract value for key '{}' using path '{}': {}", key, jsonPath, e.getMessage());
                extracted.put(key, null);
            }
        }

        return extracted;
    }

    /**
     * Build URL by replacing path parameters
     */
    private String buildUrl(String pathTemplate, Map<String, Object> values) {
        String url = pathTemplate;

        // Replace {paramName} with actual values
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (url.contains(placeholder) && entry.getValue() != null) {
                url = url.replace(placeholder, entry.getValue().toString());
            }
        }

        return url;
    }

    /**
     * Build query string from query parameter mappings
     */
    private String buildQueryString(Map<String, String> queryParams, Map<String, Object> values) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String paramName = entry.getKey();
            String valueTemplate = entry.getValue();

            // Replace {valueName} with actual value
            String paramValue = replaceTemplate(valueTemplate, values);

            if (paramValue != null && !paramValue.isEmpty()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(paramName).append("=").append(paramValue);
            }
        }

        return queryString.toString();
    }

    /**
     * Set headers from header mappings
     */
    private void setHeaders(Exchange exchange, Map<String, String> headers, Map<String, Object> values) {
        if (headers == null) {
            return;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            String valueTemplate = entry.getValue();

            String headerValue = replaceTemplate(valueTemplate, values);

            if (headerValue != null && !headerValue.isEmpty()) {
                exchange.getIn().setHeader(headerName, headerValue);
                log.debug("Set header {}: {}", headerName, headerValue);
            }
        }
    }

    /**
     * Replace {valueName} placeholders with actual values
     */
    private String replaceTemplate(String template, Map<String, Object> values) {
        if (template == null) {
            return null;
        }

        String result = template;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder) && entry.getValue() != null) {
                result = result.replace(placeholder, entry.getValue().toString());
            }
        }

        return result;
    }
}
