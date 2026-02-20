package com.mongodb.course.m08.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document("m08_product_versions")
public class ProductV2 {

    @Id
    private String id;
    private String name;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private boolean inStock;
    private String category;
    private List<String> tags = new ArrayList<>();
    private int schemaVersion = 2;

    public ProductV2() {
    }

    public ProductV2(String name, BigDecimal price, boolean inStock, String category, List<String> tags) {
        this.name = name;
        this.price = price;
        this.inStock = inStock;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
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

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
