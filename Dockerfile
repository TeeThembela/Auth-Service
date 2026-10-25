FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /auth-service-api
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runner
RUN addgroup -S appgroup && adduser -S -G appgroup appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /auth-service-api/target/auth-service-api.jar ./app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]