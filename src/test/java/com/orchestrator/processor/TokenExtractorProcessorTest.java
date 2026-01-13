package com.orchestrator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenExtractorProcessorTest {

    private TokenExtractorProcessor processor;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new TokenExtractorProcessor(new ObjectMapper());
        camelContext = new DefaultCamelContext();
    }

    @Test
    void shouldExtractTokenFromValidResponse() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        String validResponse = "{\"ticket\":\"test-token-12345\"}";
        exchange.getIn().setBody(validResponse);

        // When
        processor.process(exchange);

        // Then
        String extractedToken = exchange.getProperty("authToken", String.class);
        assertNotNull(extractedToken);
        assertEquals("test-token-12345", extractedToken);
    }

    @Test
    void shouldThrowExceptionWhenTicketFieldMissing() {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        String invalidResponse = "{\"access_token\":\"wrong-field\"}";
        exchange.getIn().setBody(invalidResponse);

        // When/Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> processor.process(exchange));
        assertTrue(exception.getMessage().contains("ticket"));
    }

    @Test
    void shouldThrowExceptionWhenTokenIsEmpty() {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        String emptyTokenResponse = "{\"ticket\":\"\"}";
        exchange.getIn().setBody(emptyTokenResponse);

        // When/Then
        assertThrows(IllegalStateException.class, () -> processor.process(exchange));
    }
}
