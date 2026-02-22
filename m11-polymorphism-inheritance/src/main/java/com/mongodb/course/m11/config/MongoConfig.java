package com.mongodb.course.m11.config;

import com.mongodb.course.m11.banking.converter.RiskProfileWriteConverter;
import com.mongodb.course.m11.banking.converter.StringToRiskProfileConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new RiskProfileWriteConverter(),
                new StringToRiskProfileConverter()
        ));
    }
}
