# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY banking-common/pom.xml banking-common/
COPY auth-service/pom.xml auth-service/
RUN mvn dependency:go-offline -B -pl auth-service -am

COPY banking-common banking-common
COPY auth-service auth-service
RUN mvn clean package -DskipTests -pl auth-service -am

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder /build/auth-service/target/*.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
