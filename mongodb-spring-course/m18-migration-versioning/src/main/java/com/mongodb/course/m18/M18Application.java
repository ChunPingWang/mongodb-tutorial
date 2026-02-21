package com.mongodb.course.m18;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMongock
public class M18Application {

    public static void main(String[] args) {
        SpringApplication.run(M18Application.class, args);
    }
}
