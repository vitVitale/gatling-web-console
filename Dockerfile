# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy Maven wrapper and pom files
COPY mvnw .
COPY pom.xml .
COPY app/pom.xml app/
COPY app/src app/src

# Build the application
RUN ./mvnw clean package -DskipTests -f app/pom.xml

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directories for volumes
RUN mkdir -p /app/simulations /app/gatling-results /app/data

# Copy JAR from build stage
COPY --from=build /app/app/target/*.jar app.jar

# Environment defaults
ENV API_USERNAME=admin
ENV API_PASSWORD=changeme
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider=off --server-response-timeout=30 \
  http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
