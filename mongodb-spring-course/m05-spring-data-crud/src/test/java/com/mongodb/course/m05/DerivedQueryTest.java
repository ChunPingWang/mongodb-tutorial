package com.mongodb.course.m05;

import com.mongodb.course.m05.ecommerce.Product;
import com.mongodb.course.m05.ecommerce.ProductRepository;
import com.mongodb.course.m05.insurance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class DerivedQueryTest {

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        policyRepository.deleteAll();
        productRepository.deleteAll();
    }

    // --- Insurance domain helpers ---

    private InsurancePolicyDocument createPolicy(String policyNumber, String holder, PolicyType type,
                                                  BigDecimal premium, BigDecimal coverage) {
        return new InsurancePolicyDocument(policyNumber, holder, type, premium, coverage,
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));
    }

    private void seedPolicies() {
        policyRepository.saveAll(List.of(
                createPolicy("POL-001", "Alice", PolicyType.TERM_LIFE,
                        new BigDecimal("200.00"), new BigDecimal("500000.00")),
                createPolicy("POL-002", "Bob", PolicyType.HEALTH,
                        new BigDecimal("350.00"), new BigDecimal("100000.00")),
                createPolicy("POL-003", "Charlie", PolicyType.TERM_LIFE,
                        new BigDecimal("180.00"), new BigDecimal("300000.00")),
                createPolicy("POL-004", "Diana", PolicyType.AUTO,
                        new BigDecimal("120.00"), new BigDecimal("50000.00")),
                createPolicy("POL-005", "Eve", PolicyType.TERM_LIFE,
                        new BigDecimal("500.00"), new BigDecimal("1000000.00"))
        ));
    }

    // --- Product domain helpers ---

    private Product createProduct(String sku, String name, String category,
                                  BigDecimal price, boolean inStock, List<String> tags) {
        Product product = new Product(sku, name, category, price, inStock);
        product.setTags(tags);
        return product;
    }

    private void seedProducts() {
        productRepository.saveAll(List.of(
                createProduct("SKU-001", "Gaming Laptop", "Electronics",
                        new BigDecimal("1299.99"), true, List.of("gaming", "laptop", "high-end")),
                createProduct("SKU-002", "Office Laptop", "Electronics",
                        new BigDecimal("699.99"), true, List.of("office", "laptop", "budget")),
                createProduct("SKU-003", "Wireless Mouse", "Electronics",
                        new BigDecimal("29.99"), true, List.of("accessory", "wireless")),
                createProduct("SKU-004", "Java Programming Book", "Books",
                        new BigDecimal("49.99"), true, List.of("programming", "java")),
                createProduct("SKU-005", "MongoDB Guide", "Books",
                        new BigDecimal("39.99"), false, List.of("programming", "mongodb")),
                createProduct("SKU-006", "Mechanical Keyboard", "Electronics",
                        new BigDecimal("149.99"), true, List.of("accessory", "gaming", "high-end"))
        ));
    }

    // --- Insurance tests ---

    @Test
    @DisplayName("LAB-02-01: findByPolicyType filters by type")
    void findByPolicyType() {
        seedPolicies();

        List<InsurancePolicyDocument> termLifePolicies = policyRepository.findByPolicyType(PolicyType.TERM_LIFE);

        assertThat(termLifePolicies).hasSize(3);
        assertThat(termLifePolicies).allMatch(p -> p.getPolicyType() == PolicyType.TERM_LIFE);
    }

    @Test
    @DisplayName("LAB-02-02: findByPremiumGreaterThan uses comparison operator")
    void findByPremiumGreaterThan() {
        seedPolicies();

        List<InsurancePolicyDocument> expensive = policyRepository.findByPremiumGreaterThan(new BigDecimal("300.00"));

        assertThat(expensive).hasSize(2);
        assertThat(expensive).allMatch(p -> p.getPremium().compareTo(new BigDecimal("300.00")) > 0);
    }

    @Test
    @DisplayName("LAB-02-03: findByStatus filters active policies")
    void findByStatus() {
        seedPolicies();
        // All seeded policies are ACTIVE
        List<InsurancePolicyDocument> active = policyRepository.findByStatus(PolicyStatus.ACTIVE);

        assertThat(active).hasSize(5);
    }

    @Test
    @DisplayName("LAB-02-04: findByPolicyType with pagination returns Page metadata")
    void findByPolicyType_withPagination() {
        seedPolicies();

        Page<InsurancePolicyDocument> page = policyRepository.findByPolicyType(
                PolicyType.TERM_LIFE, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
    }

    @Test
    @DisplayName("LAB-02-05: @Query annotation with $in finds active policies by types")
    void findActivePoliciesByTypes_queryAnnotation() {
        seedPolicies();

        List<InsurancePolicyDocument> lifeAndHealth =
                policyRepository.findActivePoliciesByTypes(List.of(PolicyType.TERM_LIFE, PolicyType.HEALTH));

        assertThat(lifeAndHealth).hasSize(4);
        assertThat(lifeAndHealth).allMatch(p ->
                p.getPolicyType() == PolicyType.TERM_LIFE || p.getPolicyType() == PolicyType.HEALTH);
    }

    // --- Product tests ---

    @Test
    @DisplayName("LAB-02-06: findByCategory filters products")
    void findByCategory() {
        seedProducts();

        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(4);
        assertThat(electronics).allMatch(p -> p.getCategory().equals("Electronics"));
    }

    @Test
    @DisplayName("LAB-02-07: findByTagsContaining matches array element")
    void findByTagsContaining() {
        seedProducts();

        List<Product> gamingProducts = productRepository.findByTagsContaining("gaming");

        assertThat(gamingProducts).hasSize(2);
        assertThat(gamingProducts).allMatch(p -> p.getTags().contains("gaming"));
    }

    @Test
    @DisplayName("LAB-02-08: @Query with $all finds products matching all tags")
    void findByAllTags_queryAnnotation() {
        seedProducts();

        List<Product> gamingHighEnd = productRepository.findByAllTags(List.of("gaming", "high-end"));

        assertThat(gamingHighEnd).hasSize(2);
        assertThat(gamingHighEnd).allMatch(p ->
                p.getTags().contains("gaming") && p.getTags().contains("high-end"));
    }

    @Test
    @DisplayName("LAB-02-09: findByNameContainingIgnoreCase does case-insensitive search")
    void findByNameContainingIgnoreCase() {
        seedProducts();

        List<Product> laptops = productRepository.findByNameContainingIgnoreCase("laptop");

        assertThat(laptops).hasSize(2);
        assertThat(laptops).allMatch(p -> p.getName().toLowerCase().contains("laptop"));
    }

    @Test
    @DisplayName("LAB-02-10: findByInStockTrue filters in-stock products")
    void findByInStockTrue() {
        seedProducts();

        List<Product> inStock = productRepository.findByInStockTrue();

        assertThat(inStock).hasSize(5);
        assertThat(inStock).allMatch(Product::isInStock);
    }

    @Test
    @DisplayName("LAB-02-11: findByCategory with pagination and sort")
    void findByCategory_withPaginationAndSort() {
        seedProducts();

        Page<Product> page = productRepository.findByCategory("Electronics",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent().get(0).getPrice())
                .isLessThan(page.getContent().get(1).getPrice());
    }

    @Test
    @DisplayName("LAB-02-12: findByCategoryOrderByPriceAsc returns sorted results")
    void findByCategoryOrderByPriceAsc() {
        seedProducts();

        List<Product> sorted = productRepository.findByCategoryOrderByPriceAsc("Electronics");

        assertThat(sorted).hasSize(4);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i).getPrice()).isLessThanOrEqualTo(sorted.get(i + 1).getPrice());
        }
    }
}
