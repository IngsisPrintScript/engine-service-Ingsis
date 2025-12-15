# syntax=docker/dockerfile:1

# --- Stage 1: build ---
FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /home/gradle/project

# Copiar TODO el proyecto
COPY . .

# Credenciales SOLO para el build (no quedan en la imagen final)
RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" >> /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew

# Build real (no ocultamos errores)
RUN ./gradlew --no-daemon clean bootJar -x test


# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# App
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# New Relic (runtime, no Gradle)
RUN apk add --no-cache wget unzip \
 && mkdir -p /app/newrelic \
 && wget -q https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
 && unzip newrelic-java.zip -d /app/newrelic \
 && rm newrelic-java.zip

# Config New Relic
COPY newrelic.yml /app/newrelic/newrelic.yml

ENV JAVA_OPTS=""
ENV NEW_RELIC_LOG=stdout

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/newrelic/newrelic.jar $JAVA_OPTS -jar app.jar"]
