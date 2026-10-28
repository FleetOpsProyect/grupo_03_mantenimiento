# Etapa de compilacion: genera el JAR ejecutable con Gradle y Java 21.
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Copiamos primero los archivos de Gradle para aprovechar la cache de Docker.
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# El codigo fuente se copia despues de las dependencias.
COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test

# Etapa de ejecucion: usa solamente el JRE, sin herramientas de compilacion.
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl se utiliza para comprobar el estado de salud de Spring Boot.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --home-dir /app spring

WORKDIR /app

# Copiamos el JAR generado desde la etapa de compilacion.
COPY --from=build /workspace/build/libs/*.jar app.jar

# La aplicacion no se ejecuta como root.
RUN chown -R spring:spring /app

USER spring:spring

# Puerto interno expuesto por Spring Boot.
EXPOSE 8083

# Ajustes de memoria y finalizacion ante falta de memoria.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Docker marca el contenedor como saludable cuando Spring esta listo.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8083/actuator/health/readiness || exit 1

# Comando principal del contenedor.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
