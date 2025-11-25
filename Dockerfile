# --- Stage 1: build the application ---
FROM gradle:8.4-jdk21-alpine AS builder
# (usa la imagen oficial de Gradle con JDK 21)

WORKDIR /home/gradle/project

# Copiar sólo los archivos necesarios para cachear dependencias
COPY build.gradle settings.gradle gradlew gradle /home/gradle/project/

# Descargar dependencias
RUN gradle --no-daemon build -x test || return 0

# Copiar el resto del código
COPY . /home/gradle/project

# Build real, generando el .jar
RUN gradle --no-daemon clean bootJar

# --- Stage 2: run the application ---
FROM eclipse-temurin:21-jre-alpine
# (imagen base JRE 21 liviana)

# Crear directorio
WORKDIR /app

# Copiar el jar desde el stage anterior
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Variables de entorno (opcional)
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
