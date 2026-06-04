import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    jacoco
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.serviceportal"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories { mavenCentral() }

extra["jjwtVersion"] = "0.12.6"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // YAML parser para validar o conteúdo dos workflows recebidos
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // JWT (mesmo padrão do generic-orchestrator)
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")

    // Testes
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("service-portal-manager.jar")
}

// JaCoCo: gate ≥ 95% de cobertura de instruções no código de aplicação.
// Excluímos a classe ManagerApplication (apenas o `main`), DTOs/exceptions e as
// data classes de domínio (@Document) — meros holders sem lógica de negócio; o
// que o JaCoCo contabiliza neles é equals/hashCode/copy/componentN gerados.
val coverageExclusions = listOf(
    "com/serviceportal/manager/ManagerApplication*",
    "com/serviceportal/manager/dto/**",
    "com/serviceportal/manager/exception/**",
    "com/serviceportal/manager/domain/**"
)

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(coverageExclusions) }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) { exclude(coverageExclusions) }
        })
    )
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}
