package com.mongodb.course.m08.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document("m08_product_versions")
public class ProductV3 {

    @Id
    private String id;
    private String name;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private boolean inStock;
    private String category;
    private List<String> tags = new ArrayList<>();
    private double rating;
    private String description;
    private Map<String, String> specifications = new HashMap<>();
    private int schemaVersion = 3;

    public ProductV3() {
    }

    public ProductV3(String name, BigDecimal price, boolean inStock, String category,
                     List<String> tags, double rating, String description,
                     Map<String, String> specifications) {
        this.name = name;
        this.price = price;
        this.inStock = inStock;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.rating = rating;
        this.description = description;
        this.specifications = specifications != null ? specifications : new HashMap<>();
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
