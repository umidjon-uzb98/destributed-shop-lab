
# syntax=docker/dockerfile:1

# STAGE 1: Application build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package -DskipTests && \
    cp target/*.jar app.jar


# STAGE 2: Application runtime
FROM eclipse-temurin:21 AS runtime

WORKDIR /app

COPY --from=build --chown=10001:10001 \
    /build/app.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
