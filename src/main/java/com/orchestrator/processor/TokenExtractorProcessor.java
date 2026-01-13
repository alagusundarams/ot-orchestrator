package com.orchestrator.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * Processor to extract authentication token from response
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenExtractorProcessor implements Processor {

    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        String responseBody = exchange.getIn().getBody(String.class);
        log.debug("Auth response received");

        JsonNode jsonResponse = objectMapper.readTree(responseBody);

        if (!jsonResponse.has("ticket")) {
            throw new IllegalStateException("Response missing 'ticket' field");
        }

        String token = jsonResponse.get("ticket").asText();

        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Token is null or empty");
        }

        // Store token in exchange property for next step
        exchange.setProperty("authToken", token);
        log.info("Successfully obtained auth token: {}...",
                token.substring(0, Math.min(10, token.length())));
    }
}
