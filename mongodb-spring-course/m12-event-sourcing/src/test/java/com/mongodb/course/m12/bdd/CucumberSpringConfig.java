package com.mongodb.course.m12.bdd;

import com.mongodb.course.m12.SharedContainersConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest
@Import(SharedContainersConfig.class)
public class CucumberSpringConfig {
}
