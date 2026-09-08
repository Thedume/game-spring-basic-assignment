# 1. Java 21 이미지
FROM eclipse-temurin:21-jdk-alpine

# 2. 작업 디렉터리
WORKDIR /app

# 3. Gradle로 빌드된 Spring Boot JAR 복사
COPY build/libs/*.jar /app/app.jar

# 4. Spring Boot 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]