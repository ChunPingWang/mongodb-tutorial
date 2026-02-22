package com.mongodb.course.m21.product;

import java.math.BigDecimal;

public sealed interface Product permits ElectronicsProduct, ClothingProduct, FoodProduct {
    String getId();
    String getSku();
    String getName();
    String getCategory();
    BigDecimal getPrice();
    int getStockQuantity();
}
