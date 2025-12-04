# syntax=docker/dockerfile:1

# --- Stage 1: build the application ---
FROM gradle:8.4-jdk21-alpine AS builder

ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /home/gradle/project

# Copiar código
COPY . .

# Crear archivo gradle.properties dentro del contenedor
RUN mkdir -p /home/gradle/.gradle && \
    echo "gpr.user=${GITHUB_USER}" >> /home/gradle/.gradle/gradle.properties && \
    echo "gpr.key=${GITHUB_TOKEN}" >> /home/gradle/.gradle/gradle.properties

RUN chmod +x gradlew

# Build del JAR con credenciales disponibles
RUN ./gradlew --no-daemon clean bootJar

# --- Stage 2: run the application ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

ENV JAVA_OPTS=""

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]