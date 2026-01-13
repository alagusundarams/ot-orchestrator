package com.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Main Spring Boot Application for OT Orchestrator
 * 
 * This application uses Apache Camel for orchestration and integration
 * patterns.
 */
@SpringBootApplication
public class OTOrchestratorApplication {

    // DEVELOPMENT ONLY - Disable SSL verification for self-signed certificates
    static {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("WARNING: SSL VERIFICATION DISABLED - DEVELOPMENT ONLY!");
        System.out.println("═══════════════════════════════════════════════════════════");

        // Disable hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        try {
            // Create trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            System.out.println("SSL bypass configured successfully");
        } catch (Exception e) {
            System.err.println("Failed to configure SSL bypass: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(OTOrchestratorApplication.class, args);
    }
}
