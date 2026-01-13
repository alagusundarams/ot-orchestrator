package com.orchestrator.routes;

import com.orchestrator.exception.GlobalExceptionHandler;
import com.orchestrator.processor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Camel route for file content download orchestration
 * 
 * Flow:
 * 1. Authenticate and get token
 * 2. Download binary file content
 * 3. Convert to base64 and add metadata
 * 4. Return JSON response
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadRoute extends RouteBuilder {

    private final AuthRequestProcessor authRequestProcessor;
    private final TokenExtractorProcessor tokenExtractorProcessor;
    private final FileDownloadRequestProcessor fileDownloadRequestProcessor;
    private final BinaryToBase64Processor binaryToBase64Processor;
    private final GlobalExceptionHandler globalExceptionHandler;

    @Value("${orchestrator.auth.url}")
    private String authUrl;

    @Override
    public void configure() throws Exception {

        // Global exception handling
        onException(Exception.class)
                .handled(true)
                .process(globalExceptionHandler);

        // REST endpoint for file download
        rest("/api/orchestrate")
                .post("/download")
                .consumes("application/json")
                .produces("application/json")
                .to("direct:downloadFile");

        // Main file download orchestration flow
        from("direct:downloadFile")
                .routeId("file-download-orchestration")
                .log("Starting file download orchestration - Exchange ID: ${exchangeId}")
                .setProperty("originalRequest", body())
                .to("direct:getAuthToken")
                .to("direct:downloadFileContent")
                .log("File download completed successfully")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

        // Step 1: Get authentication token (reuse from TokenOrchestrationRoute)
        from("direct:getAuthToken")
                .routeId("get-auth-token-download")
                .log("═══ STEP 1: AUTH TOKEN REQUEST ═══")
                .log("Calling: " + authUrl + "/v1/auth")
                .process(authRequestProcessor)
                .log("Sending POST request to auth endpoint...")
                .toD(authUrl + "/v1/auth?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Auth response received successfully")
                .process(tokenExtractorProcessor)
                .log("Token extracted successfully");

        // Step 2: Download file content and convert to base64
        from("direct:downloadFileContent")
                .routeId("download-file-content")
                .log("═══ STEP 2: FILE CONTENT DOWNLOAD ═══")
                .log("Calling: ${exchangeProperty.contentUrl}")
                .process(fileDownloadRequestProcessor)
                .log("Sending GET request to content endpoint...")
                .toD("${exchangeProperty.contentUrl}?bridgeEndpoint=true&throwExceptionOnFailure=true")
                .log("Binary content received successfully")
                .process(binaryToBase64Processor)
                .log("Content converted to base64 with metadata");
    }
}
