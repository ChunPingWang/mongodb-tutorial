package com.mongodb.course.m07.dto;

import java.util.List;
import java.util.Map;

public record FacetResult(
        List<Map<String, Object>> totalCount,
        List<Map<String, Object>> data,
        List<Map<String, Object>> stats
) {
}
