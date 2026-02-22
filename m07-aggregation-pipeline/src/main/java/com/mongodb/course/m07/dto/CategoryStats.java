package com.mongodb.course.m07.dto;

public record CategoryStats(String category, long count, double avgPrice, double avgRating) {
}
