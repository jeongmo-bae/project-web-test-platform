plugins {
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // JUnit 5
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    implementation("org.junit.platform:junit-platform-launcher:1.10.2")

    // AssertJ
    implementation("org.assertj:assertj-core:3.25.3")

    // Lombok (optional)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
