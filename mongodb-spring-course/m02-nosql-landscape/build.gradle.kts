plugins {
    id("course.spring-module")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.datastax.oss:java-driver-core:4.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.testcontainers:cassandra")
}
