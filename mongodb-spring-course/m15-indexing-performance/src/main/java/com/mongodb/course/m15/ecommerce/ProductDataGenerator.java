package com.mongodb.course.m15.ecommerce;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductDataGenerator {

    private static final String[] CATEGORIES = {
            "Electronics", "Books", "Clothing", "Home", "Sports"
    };
    private static final String[] ADJECTIVES = {
            "Premium", "Classic", "Modern", "Compact", "Advanced",
            "Professional", "Essential", "Ultra", "Smart", "Elite"
    };
    private static final String[] NOUNS = {
            "Widget", "Gadget", "Device", "Tool", "Kit",
            "System", "Unit", "Module", "Station", "Hub"
    };
    private static final String[] TAGS = {
            "portable", "gaming", "office", "premium", "budget", "wireless", "usb-c"
    };

    private final MongoTemplate mongoTemplate;

    public ProductDataGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int generateProducts(int count) {
        var products = new ArrayList<Product>(count);

        for (int i = 0; i < count; i++) {
            String category = CATEGORIES[i % CATEGORIES.length];
            String adjective = ADJECTIVES[i % ADJECTIVES.length];
            String noun = NOUNS[(i / ADJECTIVES.length) % NOUNS.length];
            String name = adjective + " " + noun + " " + (i + 1);
            String description = "A " + adjective.toLowerCase() + " " + noun.toLowerCase()
                    + " for " + category.toLowerCase() + " use. Features wireless connectivity and portable design.";
            long price = ((i % 100) + 1) * 50L;
            boolean inStock = (i % 10) < 7; // 70% in stock
            int stockQuantity = inStock ? (i % 50) + 1 : 0;

            List<String> productTags = List.of(
                    TAGS[i % TAGS.length],
                    TAGS[(i + 3) % TAGS.length]
            );

            products.add(Product.of(name, description, category, price, productTags, inStock, stockQuantity));
        }

        var inserted = mongoTemplate.insertAll(products);
        return inserted.size();
    }
}
