package com.mongodb.course.m01.rdb;

import jakarta.persistence.Embeddable;

@Embeddable
public class AddressEmbeddable {

    private String street;
    private String city;
    private String zipCode;

    protected AddressEmbeddable() {}

    public AddressEmbeddable(String street, String city, String zipCode) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
    }

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getZipCode() { return zipCode; }
}
