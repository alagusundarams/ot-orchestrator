package com.orchestrator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;

/**
 * Processor to convert binary file content to base64 and add metadata
 * Extracts content-type, filename, and size from response headers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinaryToBase64Processor implements Processor {

    private final ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        // Get binary content from response body
        byte[] binaryContent = exchange.getIn().getBody(byte[].class);

        if (binaryContent == null || binaryContent.length == 0) {
            throw new IllegalStateException("No binary content received from server");
        }

        log.info("Received binary content, size: {} bytes", binaryContent.length);

        // Encode to base64
        String base64Content = Base64.getEncoder().encodeToString(binaryContent);

        // Extract metadata from response headers
        String contentType = exchange.getIn().getHeader("Content-Type", String.class);
        String contentDisposition = exchange.getIn().getHeader("Content-Disposition", String.class);
        String nodeId = exchange.getProperty("nodeId", String.class);

        // Parse filename from Content-Disposition header if available
        String fileName = extractFileName(contentDisposition);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "node_" + nodeId; // Fallback filename
        }

        // Build JSON response with metadata
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "success");
        response.put("nodeId", nodeId);
        response.put("fileName", fileName);
        response.put("contentType", contentType != null ? contentType : "application/octet-stream");
        response.put("sizeBytes", binaryContent.length);
        response.put("base64Content", base64Content);
        response.put("timestamp", Instant.now().toString());

        log.info("File content converted to base64 successfully");
        log.debug("Metadata - fileName: {}, contentType: {}, size: {} bytes",
                fileName, contentType, binaryContent.length);

        // Set response
        exchange.getIn().setBody(objectMapper.writeValueAsString(response));
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    }

    /**
     * Extract filename from Content-Disposition header
     * Example: attachment; filename="document.pdf"
     */
    private String extractFileName(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }

        // Look for filename= or filename*=
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=") || part.startsWith("filename*=")) {
                String fileName = part.substring(part.indexOf('=') + 1).trim();
                // Remove quotes if present
                return fileName.replaceAll("^\"|\"$", "");
            }
        }

        return null;
    }
}
