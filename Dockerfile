# 빌드 단계: 의존성 캐시와 Kotlin 빌드
FROM openjdk:21-slim AS build

WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# 의존성 다운로드 (소스 코드 변경 없이 의존성만 변경될 때 캐시 활용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# 실행 단계
FROM openjdk:21-slim

WORKDIR /app

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]