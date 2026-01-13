package com.orchestrator.routes;

import com.orchestrator.exception.GlobalExceptionHandler;
import com.orchestrator.processor.*;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token-based Orchestration Route
 * 
 * Flow:
 * 1. Receive request with {id, metadata?, supper_response_codes?}
 * 2. Get authentication token via POST /v1/auth
 * 3. Call GET /v2/nodes/{id}/categories with Bearer token
 * 4. Return response
 * 
 * Uses separate processor classes for clean code and testability.
 * OpenTelemetry auto-instrumentation handles all tracing.
 */
@Component
@RequiredArgsConstructor
public class TokenOrchestrationRoute extends RouteBuilder {

    private final AuthRequestProcessor authRequestProcessor;
    private final TokenExtractorProcessor tokenExtractorProcessor;
    private final CategoriesRequestProcessor categoriesRequestProcessor;
    private final ResponseValidatorProcessor responseValidatorProcessor;
    private final GlobalExceptionHandler globalExceptionHandler;

    @Value("${orchestrator.auth.url}")
    private String authUrl;

    @Override
    public void configure() throws Exception {

        // Global exception handling
        onException(Exception.class)
                .handled(true)
                .process(globalExceptionHandler);

        // REST endpoint to trigger orchestration
        rest("/api/orchestrate")
                .post("/execute")
                .consumes("application/json")
                .produces("application/json")
                .to("direct:orchestrate");

        // Main orchestration flow
        from("direct:orchestrate")
                .routeId("token-orchestration-flow")
                .log("Starting orchestration - Exchange ID: ${exchangeId}")
                .setProperty("originalRequest", body())
                .to("direct:getAuthToken")
                .to("direct:callCategoriesEndpoint")
                .log("Orchestration completed successfully")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

        // Step 1: Get authentication token
        from("direct:getAuthToken")
                .routeId("get-auth-token")
                .log("═══ STEP 1: AUTH TOKEN REQUEST ═══")
                .log("Calling: " + authUrl + "/v1/auth")
                .process(authRequestProcessor)
                .log("Sending POST request to auth endpoint...")
                .toD("https4://" + authUrl.replace("https://", "")
                        + "/v1/auth?bridgeEndpoint=true&throwExceptionOnFailure=false&sslContextParameters=#sslContextParameters")
                .log("Auth response received successfully")
                .process(tokenExtractorProcessor)
                .log("Token extracted successfully");

        // Step 2: Call categories endpoint with token
        from("direct:callCategoriesEndpoint")
                .routeId("call-categories-endpoint")
                .log("═══ STEP 2: CATEGORIES API REQUEST ═══")
                .log("Calling: ${exchangeProperty.targetUrl}")
                .process(categoriesRequestProcessor)
                .log("Sending GET request to categories endpoint...")
                .toD("${exchangeProperty.targetUrl}?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Categories response received successfully")
                .process(responseValidatorProcessor)
                .log("Response validation completed");
    }
}
