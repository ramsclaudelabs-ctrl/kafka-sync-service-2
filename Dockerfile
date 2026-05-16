# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY .mvn .mvn
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
ARG APP_USER=svc
RUN useradd --create-home --shell /usr/sbin/nologin ${APP_USER}
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER ${APP_USER}
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]
