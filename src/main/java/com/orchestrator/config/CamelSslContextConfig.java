package com.orchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.support.jsse.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * SSL Context Parameters for Camel HTTPS Component
 * 
 * Provides SSL configuration that trusts all certificates for development.
 * This is used by Camel's http/https components.
 * 
 * WARNING: This disables SSL verification - USE ONLY FOR DEVELOPMENT/TESTING
 */
@Slf4j
@Configuration
public class CamelSslContextConfig {

    /**
     * Create SSL context parameters that trust all certificates
     * This bean is referenced in routes as #sslContextParameters
     */
    @Bean(name = "sslContextParameters")
    public SSLContextParameters sslContextParameters() {
        log.warn("═══════════════════════════════════════════════════════════");
        log.warn("CREATING SSL CONTEXT PARAMETERS - TRUST ALL CERTIFICATES");
        log.warn("SSL VERIFICATION DISABLED - DEVELOPMENT ONLY!");
        log.warn("═══════════════════════════════════════════════════════════");

        // Create custom trust manager that trusts everything
        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();

        // Use a custom X509TrustManager that accepts all certificates
        X509TrustManager trustAllManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // Trust all client certificates
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // Trust all server certificates
                if (certs != null && certs.length > 0) {
                    log.debug("Trusting server certificate: {}", certs[0].getSubjectX500Principal());
                }
            }
        };

        trustManagersParameters.setTrustManager(trustAllManager);

        // Create SSL context parameters with our trust-all manager
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        // Disable certificate hostname verification
        sslContextParameters.setCertAlias("*");

        log.info("✓ SSL Context Parameters created - trusting all certificates");

        return sslContextParameters;
    }
}
