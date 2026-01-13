package com.orchestrator.routes;

import com.orchestrator.exception.GlobalExceptionHandler;
import com.orchestrator.processor.AuthRequestProcessor;
import com.orchestrator.processor.TokenExtractorProcessor;
import com.orchestrator.processor.dynamic.DynamicPayloadTransformer;
import com.orchestrator.processor.dynamic.DynamicResponseTransformer;
import com.orchestrator.processor.dynamic.EndpointResolverProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dynamic orchestration route that handles any configured endpoint
 * 
 * Flow:
 * 1. Resolve endpoint configuration
 * 2. Authenticate if required
 * 3. Transform payload dynamically
 * 4. Call OpenText API
 * 5. Transform response
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicOrchestrationRoute extends RouteBuilder {

    private final EndpointResolverProcessor endpointResolver;
    private final AuthRequestProcessor authRequestProcessor;
    private final TokenExtractorProcessor tokenExtractorProcessor;
    private final DynamicPayloadTransformer payloadTransformer;
    private final DynamicResponseTransformer responseTransformer;
    private final GlobalExceptionHandler globalExceptionHandler;

    @Value("${orchestrator.auth.url}")
    private String authUrl;

    @Override
    public void configure() throws Exception {

        // Global exception handling
        onException(Exception.class)
                .handled(true)
                .process(globalExceptionHandler);

        // REST endpoint for dynamic orchestration
        rest("/api/dynamic")
                .post("/{endpointName}")
                .consumes("application/json")
                .produces("application/json")
                .to("direct:dynamicOrchestrate");

        // Main dynamic orchestration flow
        from("direct:dynamicOrchestrate")
                .routeId("dynamic-orchestration")
                .log("Starting dynamic orchestration - Exchange ID: ${exchangeId}")
                .setProperty("originalRequest", body())
                .process(endpointResolver)
                .choice()
                .when(simple("${exchangeProperty.requiresAuth}"))
                .to("direct:getDynamicAuthToken")
                .end()
                .process(payloadTransformer)
                .log("Calling: ${exchangeProperty.targetUrl}")
                .toD("${exchangeProperty.targetUrl}?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .process(responseTransformer)
                .log("Dynamic orchestration completed successfully")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

        // Get auth token for dynamic endpoints
        from("direct:getDynamicAuthToken")
                .routeId("get-dynamic-auth-token")
                .log("═══ AUTHENTICATION ═══")
                .log("Calling: " + authUrl + "/v1/auth")
                .process(authRequestProcessor)
                .toD(authUrl + "/v1/auth?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .process(tokenExtractorProcessor)
                .log("Token extracted successfully");
    }
}
