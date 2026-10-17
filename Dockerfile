# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache Maven wrapper and dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Compile and package application
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Runtime Stage (Non-root minimal container)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S stockflow && adduser -S stockflow -G stockflow
USER stockflow

COPY --from=builder /workspace/target/stockflow-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
