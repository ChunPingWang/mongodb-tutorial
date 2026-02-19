package com.mongodb.course.m04.bson;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document("bson_type_demo")
public class BsonTypeDemoDocument {

    @Id
    private String id;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private Instant createdAt;
    private List<String> tags;
    private Address address;

    public BsonTypeDemoDocument() {
    }

    public BsonTypeDemoDocument(BigDecimal amount, Instant createdAt, List<String> tags, Address address) {
        this.amount = amount;
        this.createdAt = createdAt;
        this.tags = tags;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public static class Address {

        private String city;
        private String zipCode;

        public Address() {
        }

        public Address(String city, String zipCode) {
            this.city = city;
            this.zipCode = zipCode;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }
}
