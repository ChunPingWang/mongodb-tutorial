package com.mongodb.course.m08;

import com.mongodb.MongoWriteException;
import com.mongodb.course.m08.ecommerce.Product;
import com.mongodb.course.m08.service.BeanValidationService;
import com.mongodb.course.m08.service.SchemaValidationService;
import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class DualValidationTest {

    private static final String PRODUCT_COLLECTION = "m08_products";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SchemaValidationService schemaValidationService;

    @Autowired
    BeanValidationService beanValidationService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(PRODUCT_COLLECTION);
        MongoJsonSchema schema = schemaValidationService.buildProductSchema();
        schemaValidationService.createCollectionStrict(PRODUCT_COLLECTION, schema);
    }

    @Test
    void beanValidationCatchesFirst_beforeMongoDB() {
        Product invalid = new Product();
        invalid.setSku("");
        invalid.setName("Test Product");
        invalid.setCategory("Test");
        invalid.setPrice(new BigDecimal("10"));

        assertThatThrownBy(() -> beanValidationService.saveProduct(invalid))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void bypassBeanValidation_dbSchemaStillRejects() {
        Document invalid = new Document()
                .append("name", "Test")
                .append("category", "Test")
                .append("price", new Decimal128(new BigDecimal("10")));
        // missing sku

        assertThatThrownBy(() -> mongoTemplate.getCollection(PRODUCT_COLLECTION).insertOne(invalid))
                .isInstanceOf(MongoWriteException.class);
    }

    @Test
    void validProduct_passesBothLayers() {
        Product valid = new Product("SKU-001", "Laptop", "Electronics", new BigDecimal("999"), true);

        assertThatNoException().isThrownBy(() -> beanValidationService.saveProduct(valid));

        assertThat(mongoTemplate.getCollection(PRODUCT_COLLECTION).countDocuments()).isEqualTo(1);
    }

    @Test
    void beanValidation_catchesBlankSku() {
        Product product = new Product("", "Laptop", "Electronics", new BigDecimal("999"), true);

        assertThatThrownBy(() -> beanValidationService.saveProduct(product))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void dbSchema_catchesMalformedFromRawInsert() {
        Document invalid = new Document()
                .append("sku", "SKU-001")
                .append("name", "Test")
                .append("price", new Decimal128(new BigDecimal("10")));
        // missing category

        assertThatThrownBy(() -> mongoTemplate.getCollection(PRODUCT_COLLECTION).insertOne(invalid))
                .isInstanceOf(MongoWriteException.class);
    }
}
