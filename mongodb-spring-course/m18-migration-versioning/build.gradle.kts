plugins {
    id("course.spring-module")
}

dependencies {
    implementation(platform("io.mongock:mongock-bom:5.5.1"))
    implementation("io.mongock:mongock-springboot-v3")
    implementation("io.mongock:mongodb-springdata-v4-driver")
}
