import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlin = "1.4.21"
    kotlin("jvm") version kotlin
    kotlin("plugin.spring") version kotlin
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}

repositories {
    mavenCentral(); mavenLocal()
}

dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-web")
    implementation("org.springframework.boot", "spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin")

    testImplementation("org.springframework.boot", "spring-boot-starter-test")
}


tasks {
    val java = "11"
    withType<KotlinCompile> {
        kotlinOptions { jvmTarget = java }; sourceCompatibility = java; targetCompatibility = java
    }
    test { useJUnitPlatform() }
}
