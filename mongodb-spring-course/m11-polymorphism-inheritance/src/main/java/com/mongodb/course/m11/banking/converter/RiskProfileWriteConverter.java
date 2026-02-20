package com.mongodb.course.m11.banking.converter;

import com.mongodb.course.m11.banking.model.RiskProfile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class RiskProfileWriteConverter implements Converter<RiskProfile, String> {

    @Override
    public String convert(RiskProfile source) {
        return source.level() + ":" + source.category();
    }
}
