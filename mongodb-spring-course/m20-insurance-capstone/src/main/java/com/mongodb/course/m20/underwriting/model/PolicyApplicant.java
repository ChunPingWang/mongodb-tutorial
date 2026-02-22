package com.mongodb.course.m20.underwriting.model;

public record PolicyApplicant(
        String name,
        int age,
        String occupation
) {
}
