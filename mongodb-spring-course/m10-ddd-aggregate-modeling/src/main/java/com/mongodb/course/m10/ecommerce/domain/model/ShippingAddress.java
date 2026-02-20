package com.mongodb.course.m10.ecommerce.domain.model;

public record ShippingAddress(String recipientName, String street, String city, String zipCode) {

    public ShippingAddress {
        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("Recipient name cannot be blank");
        }
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("Zip code cannot be blank");
        }
    }
}
