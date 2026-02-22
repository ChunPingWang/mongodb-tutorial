package com.mongodb.course.m02.bdd;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.mongodb.course.m02.cassandra.CassandraProductService;
import com.mongodb.course.m02.mongo.ProductDocument;
import com.mongodb.course.m02.mongo.ProductMongoRepository;
import com.mongodb.course.m02.redis.RedisProductService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NoSqlComparisonSteps {

    @Autowired
    private ProductMongoRepository mongoRepository;

    @Autowired
    private RedisProductService redisService;

    @Autowired
    private CassandraProductService cassandraService;

    @Autowired
    private CqlSession cqlSession;

    private ProductDocument savedMongoProduct;
    private String redisValue;
    private List<Row> cassandraResults;

    @Before
    public void cleanUp() {
        mongoRepository.deleteAll();
        cqlSession.execute("TRUNCATE products");
    }

    @Given("a product {string} in category {string} priced at {int}")
    public void aProductInCategoryPricedAt(String name, String category, int price) {
        // Store in MongoDB
        savedMongoProduct = mongoRepository.save(
                new ProductDocument(name, category, new BigDecimal(price)));

        // Store in Redis
        redisService.save("product:" + savedMongoProduct.getId(),
                name + "|" + category + "|" + price);

        // Store in Cassandra
        cassandraService.save(savedMongoProduct.getId(), name, category, new BigDecimal(price));
    }

    @When("I query each store for the product")
    public void iQueryEachStoreForTheProduct() {
        // MongoDB: query by category
        List<ProductDocument> mongoResults = mongoRepository.findByCategory(savedMongoProduct.getCategory());
        assertThat(mongoResults).isNotEmpty();

        // Redis: exact key lookup
        redisValue = redisService.findByKey("product:" + savedMongoProduct.getId());

        // Cassandra: query by partition key
        cassandraResults = cassandraService.findByCategory(savedMongoProduct.getCategory());
    }

    @Then("MongoDB supports ad-hoc queries by any field")
    public void mongoDbSupportsAdHocQueries() {
        assertThat(mongoRepository.findByCategory(savedMongoProduct.getCategory())).isNotEmpty();
        assertThat(mongoRepository.findByNameContaining("Laptop")).isNotEmpty();
    }

    @And("Redis returns data only by exact key")
    public void redisReturnsByExactKey() {
        assertThat(redisValue).isNotNull();
        assertThat(redisService.findByKey("nonexistent")).isNull();
    }

    @And("Cassandra returns data by partition key")
    public void cassandraReturnsByPartitionKey() {
        assertThat(cassandraResults).isNotEmpty();
    }

    @Given("products with different schemas in MongoDB")
    public void productsWithDifferentSchemasInMongoDB() {
        mongoRepository.save(new ProductDocument("Mouse", "electronics", new BigDecimal("500")));
        mongoRepository.save(new ProductDocument("Laptop", "electronics", new BigDecimal("35000"),
                Map.of("cpu", "M3 Pro", "ram", "18GB")));
    }

    @When("I retrieve the products from MongoDB")
    public void iRetrieveTheProductsFromMongoDB() {
        List<ProductDocument> products = mongoRepository.findByCategory("electronics");
        assertThat(products).hasSize(2);
    }

    @Then("documents with different fields coexist in the same collection")
    public void documentsWithDifferentFieldsCoexist() {
        List<ProductDocument> products = mongoRepository.findByCategory("electronics");
        ProductDocument simple = products.stream()
                .filter(p -> p.getName().equals("Mouse")).findFirst().orElseThrow();
        ProductDocument rich = products.stream()
                .filter(p -> p.getName().equals("Laptop")).findFirst().orElseThrow();

        assertThat(simple.getSpecifications()).isNull();
        assertThat(rich.getSpecifications()).containsKeys("cpu", "ram");
    }
}
