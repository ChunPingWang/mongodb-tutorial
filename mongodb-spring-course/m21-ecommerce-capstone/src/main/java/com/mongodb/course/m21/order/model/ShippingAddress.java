package com.mongodb.course.m21.order.model;

public record ShippingAddress(
        String recipientName,
        String street,
        String city,
        String postalCode
) {
}
