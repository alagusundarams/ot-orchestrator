package com.orchestrator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * Processor to prepare the file content download request
 * Extracts node ID and sets up headers for binary content download
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadRequestProcessor implements Processor {

    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        // Get the auth token from exchange property
        String token = exchange.getProperty("authToken", String.class);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Auth token not found in exchange");
        }

        // Get the original request body
        String requestBody = exchange.getProperty("originalRequest", String.class);
        var jsonRequest = objectMapper.readTree(requestBody);

        // Extract node ID
        String nodeId = jsonRequest.path("id").asText();
        if (nodeId.isEmpty()) {
            throw new IllegalArgumentException("Node ID is required in request body");
        }

        // Get target endpoint base URL
        String targetEndpoint = exchange.getContext().resolvePropertyPlaceholders("{{orchestrator.target.endpoint}}");

        // Build the content download URL
        String contentUrl = targetEndpoint + "/v2/nodes/" + nodeId + "/content";

        log.info("Preparing file download request for node: {}", nodeId);
        log.debug("Content URL: {}", contentUrl);

        // Set the URL as exchange property for the route
        exchange.setProperty("contentUrl", contentUrl);
        exchange.setProperty("nodeId", nodeId);

        // Set headers for binary content download
        exchange.getIn().setHeader("OTCSTicket", token);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
        exchange.getIn().setHeader("Accept", "*/*"); // Accept any content type
        exchange.getIn().setBody(null); // GET request has no body

        log.info("File download request prepared successfully");
    }
}
