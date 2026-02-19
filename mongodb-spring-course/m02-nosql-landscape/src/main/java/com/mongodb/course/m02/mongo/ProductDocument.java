package com.mongodb.course.m02.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Map;

@Document("products")
public class ProductDocument {

    @Id
    private String id;
    private String name;
    private String category;
    private BigDecimal price;
    private Map<String, Object> specifications;

    public ProductDocument() {
    }

    public ProductDocument(String name, String category, BigDecimal price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public ProductDocument(String name, String category, BigDecimal price, Map<String, Object> specifications) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.specifications = specifications;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Map<String, Object> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, Object> specifications) {
        this.specifications = specifications;
    }
}
