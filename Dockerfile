# syntax=docker/dockerfile:1

# ------------------------------
# Build stage
# ------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first to leverage Docker layer caching
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------------------
# Runtime stage
# ------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Render provides PORT at runtime; app defaults to 8080 if not set
EXPOSE 8080

# Copy fat jar from build stage
COPY --from=build /app/target/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
