package com.mongodb.course.m01.bdd;

import com.mongodb.course.m01.SharedContainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@Import(SharedContainersConfig.class)
public class CucumberSpringConfig {
}
