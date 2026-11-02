FROM eclipse-temurin:17-jre

LABEL maintainer="abhinav307" \
      description="Maven CI/CD Microservice"

# Create a dedicated non-root user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

WORKDIR /app

# Copy the built JAR artifact
COPY target/maven-cicd-demo.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose microservice HTTP port
EXPOSE 8080

# Configure container runtime environment variables
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Container healthcheck probe
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:${PORT}/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
