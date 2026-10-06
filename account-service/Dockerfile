FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY banking-common/pom.xml banking-common/
COPY account-service/pom.xml account-service/
RUN mvn dependency:go-offline -B -pl account-service -am

COPY banking-common banking-common
COPY account-service account-service
RUN mvn clean package -DskipTests -pl account-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S banking && adduser -S banking -G banking
USER banking

COPY --from=builder /build/account-service/target/*.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
