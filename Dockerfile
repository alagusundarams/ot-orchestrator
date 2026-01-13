# Multi-stage build for OT Orchestrator
# Target: OpenJDK 22 on RHEL

# Build stage
FROM gradle:8.5-jdk22 AS build
WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Runtime stage - Using Red Hat UBI with OpenJDK 17
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest

# Set working directory
WORKDIR /deployments

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
