package com.orchestrator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SSL Context Parameters for Camel HTTPS Component
 * 
 * Provides SSL configuration that trusts all certificates for development.
 * This is used by Camel's https4 component.
 * 
 * WARNING: This disables SSL verification - USE ONLY FOR DEVELOPMENT/TESTING
 */
@Slf4j
@Configuration
public class CamelSslContextConfig {

    @Bean(name = "sslContextParameters")
    public SSLContextParameters sslContextParameters() {
        log.warn("═══════════════════════════════════════════════════════════");
        log.warn("CREATING SSL CONTEXT PARAMETERS - TRUST ALL CERTIFICATES");
        log.warn("SSL VERIFICATION DISABLED - DEVELOPMENT ONLY!");
        log.warn("═══════════════════════════════════════════════════════════");

        // Create trust managers that trust all certificates
        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setTrustManager(new javax.net.ssl.X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        });

        // Create SSL context parameters
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

        log.info("✓ SSL Context Parameters created - trusting all certificates");

        return sslContextParameters;
    }
}
