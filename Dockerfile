# syntax=docker/dockerfile:1

# ---- Build ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias
COPY pom.xml ./
RUN mvn -q -B -Dmaven.test.skip=true dependency:go-offline

# Copiamos el código y build
COPY src src
RUN mvn -q -B -DskipTests clean package

# ---- Runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 10001 spring
COPY --from=build /app/target/*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    TZ=America/Argentina/Buenos_Aires
EXPOSE 8080
USER spring
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
