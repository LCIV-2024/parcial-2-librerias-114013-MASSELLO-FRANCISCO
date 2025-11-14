# Multi-stage build para compilar y ejecutar la app Spring Boot

# Etapa 1: build con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiamos descriptor de dependencias para aprovechar la cache
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiamos el código fuente y construimos el JAR
COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa 2: runtime liviano
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
