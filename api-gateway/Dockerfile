FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -B -pl api-gateway -am

COPY api-gateway api-gateway
RUN mvn clean package -DskipTests -pl api-gateway -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder /build/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
