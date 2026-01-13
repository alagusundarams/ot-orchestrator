package com.orchestrator.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.time.Instant;

/**
 * Global exception handler for all orchestration routes
 * Logs exceptions gracefully with full stack traces and returns formatted error
 * responses
 */
@Slf4j
@Component
public class GlobalExceptionHandler implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        String errorMessage = "Unknown error";
        String errorType = "UNKNOWN";
        int statusCode = 500;

        if (cause != null) {
            errorMessage = cause.getMessage();

            // Identify error type and location
            if (isSSLError(cause)) {
                errorType = "SSL_CERTIFICATE_ERROR";
                statusCode = 502;
                log.error("═══════════════════════════════════════════════════════════");
                log.error("SSL CERTIFICATE ERROR DETECTED!");
                log.error("This is likely a Zscaler SSL inspection issue.");
                log.error("Error occurred in: {}", identifyErrorLocation(exchange));
                log.error("═══════════════════════════════════════════════════════════");
            } else if (cause instanceof IllegalArgumentException) {
                errorType = "INVALID_REQUEST";
                statusCode = 400;
                log.error("Invalid request parameter: {}", errorMessage);
            } else if (cause instanceof IllegalStateException) {
                errorType = "SERVICE_ERROR";
                statusCode = 502;
                log.error("Upstream service error: {}", errorMessage);
            }

            // Log full error details
            log.error("┌─────────────────────────────────────────────────────────┐");
            log.error("│ ORCHESTRATION ERROR                                     │");
            log.error("├─────────────────────────────────────────────────────────┤");
            log.error("│ Error Type    : {}", errorType);
            log.error("│ Status Code   : {}", statusCode);
            log.error("│ Error Message : {}", errorMessage);
            log.error("│ Location      : {}", identifyErrorLocation(exchange));
            log.error("│ Exchange ID   : {}", exchange.getExchangeId());
            log.error("└─────────────────────────────────────────────────────────┘");

            // Print full stack trace
            log.error("Full stack trace:", cause);

        } else {
            log.error("Orchestration error with no exception details");
        }

        String errorResponse = String.format(
                "{\"status\":\"error\",\"errorType\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                errorType,
                escapeJson(errorMessage),
                Instant.now().toString());

        exchange.getIn().setBody(errorResponse);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }

    private boolean isSSLError(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof SSLException ||
                    current instanceof CertificateException ||
                    current.getClass().getName().contains("PKIX") ||
                    (current.getMessage() != null && current.getMessage().contains("PKIX"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String identifyErrorLocation(Exchange exchange) {
        String routeId = exchange.getFromRouteId();
        if (routeId != null) {
            if (routeId.contains("auth")) {
                return "AUTH_ENDPOINT (POST /v1/auth)";
            } else if (routeId.contains("categories")) {
                return "CATEGORIES_ENDPOINT (GET /v2/nodes/{id}/categories)";
            }
            return routeId;
        }
        return "UNKNOWN";
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
