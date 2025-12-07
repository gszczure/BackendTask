ARG IMAGE_VERSION=21.0.7_6

# Build stage
FROM eclipse-temurin:${IMAGE_VERSION}-jdk-alpine AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src/

RUN ./mvnw -B clean verify


# Runtime stage
FROM eclipse-temurin:${IMAGE_VERSION}-jre AS runtime

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
