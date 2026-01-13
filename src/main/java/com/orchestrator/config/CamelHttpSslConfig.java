package com.orchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * Camel HTTP Component SSL Configuration
 * 
 * Forces Camel's HTTP component to accept self-signed certificates
 * by configuring the HTTP client with a trust-all SSL context.
 * 
 * WARNING: This disables SSL verification - USE ONLY FOR DEVELOPMENT/TESTING
 */
@Slf4j
@Configuration
public class CamelHttpSslConfig {

    @Bean
    public HttpComponent configureHttpComponent(CamelContext camelContext) throws Exception {
        log.warn("═══════════════════════════════════════════════════════════");
        log.warn("CONFIGURING CAMEL HTTP COMPONENT FOR SELF-SIGNED CERTS");
        log.warn("SSL VERIFICATION DISABLED - DEVELOPMENT ONLY!");
        log.warn("═══════════════════════════════════════════════════════════");

        // Create SSL context that trusts all certificates
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> {
                    log.debug("Trusting certificate: {}",
                            chain != null && chain.length > 0 ? chain[0].getSubjectX500Principal() : "unknown");
                    return true; // Trust everything
                })
                .build();

        // Create SSL socket factory with no hostname verification
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE // Accept any hostname
        );

        // Create connection manager with SSL bypass
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(20)
                .build();

        // Create HTTP client with our SSL-bypassing connection manager
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // Get or create HTTP component
        HttpComponent httpComponent = camelContext.getComponent("http", HttpComponent.class);

        // Configure the HTTP component to use our custom client
        httpComponent.setHttpClientConfigurer(clientBuilder -> {
            clientBuilder.setConnectionManager(connectionManager);
        });

        log.info("✓ Camel HTTP component configured with SSL bypass");

        return httpComponent;
    }
}
