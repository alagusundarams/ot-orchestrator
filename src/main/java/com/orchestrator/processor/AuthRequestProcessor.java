package com.orchestrator.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Processor to build form-urlencoded authentication request
 */
@Slf4j
@Component
public class AuthRequestProcessor implements Processor {

    @Value("${orchestrator.auth.username}")
    private String username;

    @Value("${orchestrator.auth.password}")
    private String password;

    @Value("${orchestrator.auth.domain:}")
    private String domain;

    @Override
    public void process(Exchange exchange) throws Exception {
        StringBuilder formBody = new StringBuilder();
        formBody.append("username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
        formBody.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));

        // Add domain only if provided
        if (domain != null && !domain.isEmpty()) {
            formBody.append("&domain=").append(URLEncoder.encode(domain, StandardCharsets.UTF_8));
        }

        exchange.getIn().setBody(formBody.toString());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");

        log.debug("Built auth request with username: {}", username);
    }
}
