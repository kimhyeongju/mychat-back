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
USER spring

# app/build.gradle에서 archiveFileName을 app.jar로 고정했으므로 와일드카드가 필요 없다
COPY --from=build /workspace/app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]