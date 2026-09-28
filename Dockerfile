# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY common/build.gradle common/
COPY auth/build.gradle   auth/
COPY chat/build.gradle   chat/
COPY board/build.gradle  board/
COPY invest/build.gradle invest/
COPY app/build.gradle    app/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY . .
RUN ./gradlew :app:bootJar --no-daemon -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring

COPY --from=build --chown=spring:spring /workspace/app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]