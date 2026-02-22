package com.mongodb.course.m21.listing.specification;

public class MinimumStockSpec {

    private static final int MINIMUM_STOCK = 10;

    public boolean isSatisfiedBy(int stockQuantity) {
        return stockQuantity >= MINIMUM_STOCK;
    }
}
