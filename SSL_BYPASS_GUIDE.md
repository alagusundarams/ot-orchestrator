# SSL Certificate Bypass - System Properties Approach

If the HttpClientConfig bean isn't working, add these JVM arguments when starting the application:

## Option 1: Add to application.yml (Spring Boot way)

Add to `src/main/resources/application.yml`:

```yaml
# DEVELOPMENT ONLY - Disable SSL verification
server:
  ssl:
    enabled: false

# System properties for SSL bypass
spring:
  main:
    allow-bean-definition-overriding: true
```

## Option 2: Add JVM Arguments

When running the application:

```bash
# Mac
JAVA_OPTS="-Djavax.net.ssl.trustAll=true -Dhttps.protocols=TLSv1.2,TLSv1.3" ./gradlew-java17.sh bootRun

# Windows
set JAVA_OPTS=-Djavax.net.ssl.trustAll=true -Dhttps.protocols=TLSv1.2,TLSv1.3
gradlew.bat bootRun
```

## Option 3: Programmatic SSL Bypass (Add to main class)

Add this to `OTOrchestratorApplication.java` before `SpringApplication.run()`:

```java
static {
    // DEVELOPMENT ONLY - Disable SSL verification
    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    
    try {
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
        sc.init(null, new javax.net.ssl.TrustManager[]{
            new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
            }
        }, new java.security.SecureRandom());
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

## Option 4: Import Certificate (RECOMMENDED for Production)

1. Export the server's certificate:
   ```bash
   echo | openssl s_client -connect yourhostname:443 2>&1 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > server.crt
   ```

2. Import into Java keystore:
   ```bash
   keytool -import -trustcacerts -alias myserver \
     -file server.crt \
     -keystore $JAVA_HOME/lib/security/cacerts \
     -storepass changeit
   ```

## Verify Current Config

Check startup logs for:
```
═══════════════════════════════════════════════════════════
CONFIGURING HTTP CLIENT TO ACCEPT SELF-SIGNED CERTIFICATES
SSL VERIFICATION IS DISABLED - FOR DEVELOPMENT ONLY!
═══════════════════════════════════════════════════════════
```

If you DON'T see this message, the HttpClientConfig bean is not being loaded.
