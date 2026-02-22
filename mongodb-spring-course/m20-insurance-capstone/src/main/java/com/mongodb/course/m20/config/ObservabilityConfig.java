package com.mongodb.course.m20.config;

import com.mongodb.course.m20.observability.SlowQueryDetector;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    SlowQueryDetector slowQueryDetector() {
        return new SlowQueryDetector(100);
    }

    @Bean
    MongoClientSettingsBuilderCustomizer slowQueryDetectorCustomizer(SlowQueryDetector detector) {
        return builder -> builder.addCommandListener(detector);
    }
}
