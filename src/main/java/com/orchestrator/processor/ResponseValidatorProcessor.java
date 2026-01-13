package com.orchestrator.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * Processor to validate categories API response
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseValidatorProcessor implements Processor {

    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        String response = exchange.getIn().getBody(String.class);
        log.debug("Validating response structure");

        JsonNode responseJson = objectMapper.readTree(response);

        if (!responseJson.has("links")) {
            log.warn("Response missing 'links' field");
        }
        if (!responseJson.has("results")) {
            log.warn("Response missing 'results' field");
        }

        log.info("Response validation completed");
    }
}
