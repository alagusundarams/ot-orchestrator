package com.orchestrator.config;

import org.apache.camel.component.http.HttpComponent;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * HTTP Client Configuration
 * Configures HTTP client to accept self-signed certificates
 * 
 * WARNING: This disables SSL verification - USE ONLY FOR DEVELOPMENT/TESTING
 * For production, import the server's certificate into Java's truststore
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public HttpComponent httpComponent() throws Exception {
        HttpComponent httpComponent = new HttpComponent();
        httpComponent.setHttpClientConfigurer(clientBuilder -> {
            try {
                // Create SSL context that trusts all certificates
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial((chain, authType) -> true) // Trust all certificates
                        .build();

                // Create SSL socket factory with no hostname verification
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        NoopHostnameVerifier.INSTANCE);

                // Create connection manager with SSL factory
                HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .setMaxConnTotal(200)
                        .setMaxConnPerRoute(20)
                        .build();

                // Set connection manager on the builder
                clientBuilder.setConnectionManager(connectionManager);
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure HTTP client for self-signed certificates", e);
            }
        });

        return httpComponent;
    }
}
