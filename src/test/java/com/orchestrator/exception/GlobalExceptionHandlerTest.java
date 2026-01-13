package com.orchestrator.exception;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        camelContext = new DefaultCamelContext();
    }

    @Test
    void shouldHandleIllegalArgumentExceptionWith400() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT,
                new IllegalArgumentException("Invalid parameter"));

        // When
        handler.process(exchange);

        // Then
        assertEquals(400, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        String body = exchange.getIn().getBody(String.class);
        assertTrue(body.contains("error"));
        assertTrue(body.contains("Invalid parameter"));
    }

    @Test
    void shouldHandleIllegalStateExceptionWith502() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT,
                new IllegalStateException("Service unavailable"));

        // When
        handler.process(exchange);

        // Then
        assertEquals(502, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    void shouldHandleGenericExceptionWith500() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT,
                new RuntimeException("Unexpected error"));

        // When
        handler.process(exchange);

        // Then
        assertEquals(500, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }
}
