package com.mongodb.course.m07.dto;

import java.util.List;
import java.util.Map;

public record OrderProductDetail(String orderNumber, String customerName, List<Map<String, Object>> items) {
}
