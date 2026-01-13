package com.orchestrator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CategoriesRequestProcessorTest {

    private CategoriesRequestProcessor processor;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new CategoriesRequestProcessor(new ObjectMapper());
        ReflectionTestUtils.setField(processor, "targetEndpoint", "http://localhost:8082");
        camelContext = new DefaultCamelContext();
    }

    @Test
    void shouldBuildCorrectUrlWithRequiredIdOnly() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("authToken", "test-token");
        exchange.setProperty("originalRequest", "{\"id\":\"12345\"}");

        // When
        processor.process(exchange);

        // Then
        String targetUrl = exchange.getProperty("targetUrl", String.class);
        assertEquals("http://localhost:8082/v2/nodes/12345/categories", targetUrl);
        assertEquals("test-token", exchange.getIn().getHeader("OTCSTicket"));
        assertEquals("GET", exchange.getIn().getHeader(Exchange.HTTP_METHOD));
    }

    @Test
    void shouldBuildUrlWithOptionalParameters() throws Exception {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("authToken", "test-token");
        exchange.setProperty("originalRequest",
                "{\"id\":\"12345\",\"metadata\":\"true\",\"supper_response_codes\":\"200,201\"}");

        // When
        processor.process(exchange);

        // Then
        String targetUrl = exchange.getProperty("targetUrl", String.class);
        assertTrue(targetUrl.contains("metadata=true"));
        assertTrue(targetUrl.contains("supper_response_codes=200%2C201"));
    }

    @Test
    void shouldThrowExceptionWhenIdMissing() {
        // Given
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty("authToken", "test-token");
        exchange.setProperty("originalRequest", "{\"metadata\":\"true\"}");

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(exchange));
        assertTrue(exception.getMessage().contains("id"));
    }
}
