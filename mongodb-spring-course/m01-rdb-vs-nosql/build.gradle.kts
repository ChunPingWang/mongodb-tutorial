plugins {
    id("course.spring-module")
}

dependencies {
    // M01 needs PostgreSQL + JPA for RDB vs NoSQL comparison
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.testcontainers:postgresql")
    runtimeOnly("org.postgresql:postgresql")
}
