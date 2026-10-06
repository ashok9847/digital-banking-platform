FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY banking-common/pom.xml banking-common/
COPY notification-service/pom.xml notification-service/
RUN mvn dependency:go-offline -B -pl notification-service -am

COPY banking-common banking-common
COPY notification-service notification-service
RUN mvn clean package -DskipTests -pl notification-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder /build/notification-service/target/*.jar app.jar

EXPOSE 8085

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
