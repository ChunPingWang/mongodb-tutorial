package com.mongodb.course.m21.listing.model;

import com.mongodb.course.m21.listing.specification.CategoryProfitabilitySpec;
import com.mongodb.course.m21.listing.specification.MinimumStockSpec;
import com.mongodb.course.m21.projection.readmodel.CategoryMetrics;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductListing {

    private String listingId;
    private String sku;
    private String productName;
    private String category;
    private String requestedProductType;
    private BigDecimal basePrice;
    private int stockQuantity;
    private ListingStatus status;
    private String rejectionReason;

    private ProductListing() {
    }

    public static ProductListing submit(String sku, String productName, String category,
                                         String productType, BigDecimal basePrice, int stockQuantity) {
        var listing = new ProductListing();
        listing.listingId = UUID.randomUUID().toString();
        listing.sku = sku;
        listing.productName = productName;
        listing.category = category;
        listing.requestedProductType = productType;
        listing.basePrice = basePrice;
        listing.stockQuantity = stockQuantity;
        listing.status = ListingStatus.SUBMITTED;
        return listing;
    }

    public void review(CategoryProfitabilitySpec profitSpec, MinimumStockSpec stockSpec,
                       CategoryMetrics metrics) {
        if (status != ListingStatus.SUBMITTED) {
            throw new IllegalStateException("Can only review SUBMITTED listings, current: " + status);
        }

        this.status = ListingStatus.UNDER_REVIEW;

        if (!stockSpec.isSatisfiedBy(stockQuantity)) {
            this.status = ListingStatus.REJECTED;
            this.rejectionReason = "Minimum stock requirement not met: " + stockQuantity;
            return;
        }

        if (!profitSpec.isSatisfiedBy(metrics)) {
            this.status = ListingStatus.REJECTED;
            this.rejectionReason = profitSpec.getRejectionReason(metrics);
            return;
        }

        this.status = ListingStatus.APPROVED;
    }

    public String getListingId() { return listingId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getRequestedProductType() { return requestedProductType; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getStockQuantity() { return stockQuantity; }
    public ListingStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
}
