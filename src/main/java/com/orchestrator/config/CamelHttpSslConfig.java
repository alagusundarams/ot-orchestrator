package com.orchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import javax.net.ssl.SSLContext;

/**
 * Camel HTTP Component SSL Configuration
 * 
 * Configures Camel's HTTP component AFTER Spring context is initialized
 * to ensure it uses our SSL bypass settings.
 * 
 * WARNING: This disables SSL verification - USE ONLY FOR DEVELOPMENT/TESTING
 */
@Slf4j
@Configuration
public class CamelHttpSslConfig {

    /**
     * Configure Camel HTTP component after Spring context is fully initialized
     * This ensures our SSL bypass is applied to all HTTP calls made by Camel
     */
    @EventListener(ContextRefreshedEvent.class)
    public void configureCamelHttpComponent(ContextRefreshedEvent event) {
        try {
            log.warn("═══════════════════════════════════════════════════════════");
            log.warn("CONFIGURING CAMEL HTTP COMPONENT FOR SELF-SIGNED CERTS");
            log.warn("SSL VERIFICATION DISABLED - DEVELOPMENT ONLY!");
            log.warn("═══════════════════════════════════════════════════════════");

            CamelContext camelContext = event.getApplicationContext().getBean(CamelContext.class);

            // Create SSL context that trusts ALL certificates
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> {
                        if (chain != null && chain.length > 0) {
                            log.debug("Trusting certificate: {}", chain[0].getSubjectX500Principal());
                        }
                        return true; // Trust everything
                    })
                    .build();

            // Create SSL socket factory with NO hostname verification
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

            // Get HTTP component from Camel context
            HttpComponent httpComponent = camelContext.getComponent("http", HttpComponent.class);

            // Configure HTTP component to use our SSL-bypassing client
            httpComponent.setHttpClientConfigurer(clientBuilder -> {
                log.info("Applying SSL bypass to Camel HTTP client builder");
                clientBuilder.setConnectionManager(connectionManager);
            });

            log.info("✓ Camel HTTP component configured successfully with SSL bypass");

        } catch (Exception e) {
            log.error("Failed to configure Camel HTTP component with SSL bypass", e);
            throw new RuntimeException("Failed to configure SSL bypass for Camel HTTP component", e);
        }
    }
}
