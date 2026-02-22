package com.mongodb.course.m21.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("m21_products")
@TypeAlias("ClothingProduct")
public final class ClothingProduct implements Product {

    @Id
    private String id;
    private String sku;
    private String name;
    private String category;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private int stockQuantity;
    private String size;
    private String color;

    public ClothingProduct() {
    }

    public ClothingProduct(String id, String sku, String name, String category,
                            BigDecimal price, int stockQuantity,
                            String size, String color) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.size = size;
        this.color = color;
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

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
