package com.mongodb.course.m07.dto;

import java.math.BigDecimal;

public record TypeStats(String type, long count, BigDecimal total, BigDecimal avg, BigDecimal min, BigDecimal max) {
}
