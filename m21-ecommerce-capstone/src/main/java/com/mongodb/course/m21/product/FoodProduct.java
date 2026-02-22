package com.mongodb.course.m21.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m21_products")
@TypeAlias("FoodProduct")
public final class FoodProduct implements Product {

    @Id
    private String id;
    private String sku;
    private String name;
    private String category;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private int stockQuantity;
    private String expiryDate;
    private boolean organic;

    public FoodProduct() {
    }

    public FoodProduct(String id, String sku, String name, String category,
                        BigDecimal price, int stockQuantity,
                        String expiryDate, boolean organic) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.expiryDate = expiryDate;
        this.organic = organic;
    }

    @Override public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    @Override public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @Override public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isOrganic() { return organic; }
    public void setOrganic(boolean organic) { this.organic = organic; }
}
