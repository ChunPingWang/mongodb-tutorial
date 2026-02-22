package com.mongodb.course.m11.banking.converter;

import com.mongodb.course.m11.banking.model.RiskProfile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToRiskProfileConverter implements Converter<String, RiskProfile> {

    @Override
    public RiskProfile convert(String source) {
        String[] parts = source.split(":", 2);
        return new RiskProfile(Integer.parseInt(parts[0]), parts[1]);
    }
}
