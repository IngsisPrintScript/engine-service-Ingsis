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

# Copiar el jar desde el build anterior
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Copiar los archivos de New Relic (agregar estos pasos)
COPY --from=builder /home/gradle/project/build/newrelic/newrelic.jar /app/newrelic.jar
COPY --from=builder /home/gradle/project/build/newrelic/newrelic.yml /app/newrelic.yml

# Configuración opcional para Java (si es necesario)
ENV JAVA_OPTS=""

EXPOSE 8088

# Configurar ENTRYPOINT para incluir el -javaagent
ENTRYPOINT ["sh", "-c", "java -javaagent:/app/newrelic.jar $JAVA_OPTS -jar app.jar"]
