# ==============================
# 1단계: Spring Boot 빌드
# ==============================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Windows에서 작성된 gradlew의 CRLF 문제 방지
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test


# ==============================
# 2단계: Spring Boot 실행
# ==============================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]