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
                .log("Step 1: Requesting authentication token")
                .process(authRequestProcessor)
                .toD(authUrl + "/v1/auth?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .process(tokenExtractorProcessor);

        // Step 2: Call categories endpoint with token
        from("direct:callCategoriesEndpoint")
                .routeId("call-categories-endpoint")
                .log("Step 2: Calling GET /v2/nodes/{id}/categories")
                .process(categoriesRequestProcessor)
                .toD("${exchangeProperty.targetUrl}?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .process(responseValidatorProcessor);
    }
}
