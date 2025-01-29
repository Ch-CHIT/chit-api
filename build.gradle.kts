plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.springframework.boot") version "3.3.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // JSON 직렬화 및 역직렬화
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Kotlin 관련 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Querydsl
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")
    
    // 캐시 및 저장소 라이브러리
    implementation("org.ehcache:ehcache:3.10.8")
    implementation("org.hibernate.orm:hibernate-jcache:6.5.3.Final")
    
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    
    // flyway
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")
    
    // 보안 관련 라이브러리
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
    
    // 유틸리티 라이브러리
    implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    
    // API 문서화
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    
    // 테스트 관련 라이브러리
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}