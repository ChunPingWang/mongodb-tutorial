package com.mongodb.course.m07.dto;

import java.math.BigDecimal;

public record BalanceBucket(BigDecimal min, BigDecimal max, long count) {
}
