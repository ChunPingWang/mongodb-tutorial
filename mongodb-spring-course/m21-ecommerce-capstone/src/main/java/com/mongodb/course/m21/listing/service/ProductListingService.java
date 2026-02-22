package com.mongodb.course.m21.listing.service;

import com.mongodb.course.m21.listing.model.ListingStatus;
import com.mongodb.course.m21.listing.model.ProductListing;
import com.mongodb.course.m21.listing.specification.CategoryProfitabilitySpec;
import com.mongodb.course.m21.listing.specification.MinimumStockSpec;
import com.mongodb.course.m21.product.*;
import com.mongodb.course.m21.projection.OrderQueryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProductListingService {

    private final ProductCatalogService productCatalogService;
    private final OrderQueryService orderQueryService;

    private final CategoryProfitabilitySpec profitSpec = new CategoryProfitabilitySpec();
    private final MinimumStockSpec stockSpec = new MinimumStockSpec();

    public ProductListingService(ProductCatalogService productCatalogService,
                                  OrderQueryService orderQueryService) {
        this.productCatalogService = productCatalogService;
        this.orderQueryService = orderQueryService;
    }

    public ProductListing submit(String sku, String productName, String category,
                                  String productType, BigDecimal basePrice, int stockQuantity) {
        return ProductListing.submit(sku, productName, category, productType, basePrice, stockQuantity);
    }

    public ProductListing review(ProductListing listing) {
        var metrics = orderQueryService.computeCategoryMetrics(listing.getCategory());
        listing.review(profitSpec, stockSpec, metrics);

        if (listing.getStatus() == ListingStatus.APPROVED) {
            createProduct(listing);
        }

        return listing;
    }

    private void createProduct(ProductListing listing) {
        String id = UUID.randomUUID().toString();

        Product product = switch (listing.getRequestedProductType()) {
            case "Electronics" -> new ElectronicsProduct(
                    id, listing.getSku(), listing.getProductName(), listing.getCategory(),
                    listing.getBasePrice(), listing.getStockQuantity(),
                    "Generic", 12);
            case "Clothing" -> new ClothingProduct(
                    id, listing.getSku(), listing.getProductName(), listing.getCategory(),
                    listing.getBasePrice(), listing.getStockQuantity(),
                    "M", "Black");
            case "Food" -> new FoodProduct(
                    id, listing.getSku(), listing.getProductName(), listing.getCategory(),
                    listing.getBasePrice(), listing.getStockQuantity(),
                    "2026-12-31", false);
            default -> throw new IllegalArgumentException("Unknown product type: "
                    + listing.getRequestedProductType());
        };

        productCatalogService.save(product);
    }
}
