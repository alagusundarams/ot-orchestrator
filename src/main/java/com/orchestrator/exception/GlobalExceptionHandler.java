package com.orchestrator.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Global exception handler for all orchestration routes
 * Logs exceptions gracefully and returns formatted error responses
 */
@Slf4j
@Component
public class GlobalExceptionHandler implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        String errorMessage = "Unknown error";
        int statusCode = 500;

        if (cause != null) {
            errorMessage = cause.getMessage();
            log.error("Orchestration error: {}", errorMessage, cause);

            // Determine appropriate status code
            if (cause instanceof IllegalArgumentException) {
                statusCode = 400; // Bad Request
            } else if (cause instanceof IllegalStateException) {
                statusCode = 502; // Bad Gateway (upstream service issue)
            }
        } else {
            log.error("Orchestration error with no exception details");
        }

        String errorResponse = String.format(
                "{\"status\":\"error\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                escapeJson(errorMessage),
                Instant.now().toString());

        exchange.getIn().setBody(errorResponse);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
