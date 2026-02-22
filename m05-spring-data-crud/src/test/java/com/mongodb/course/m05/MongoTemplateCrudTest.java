package com.mongodb.course.m05;

import com.mongodb.course.m05.banking.*;
import com.mongodb.course.m05.ecommerce.Product;
import com.mongodb.course.m05.ecommerce.ProductRepository;
import com.mongodb.course.m05.ecommerce.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoTemplateCrudTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        bankAccountRepository.deleteAll();
        productRepository.deleteAll();
    }

    private BankAccount createAndSaveAccount(String accountNumber, String holder,
                                              AccountType type, BigDecimal balance) {
        BankAccount account = new BankAccount(accountNumber, holder, type, balance);
        return bankAccountRepository.save(account);
    }

    private Product createAndSaveProduct(String sku, String name, String category,
                                          BigDecimal price, List<String> tags) {
        Product product = new Product(sku, name, category, price, true);
        product.setTags(tags);
        return productRepository.save(product);
    }

    // --- Banking: MongoTemplate operations ---

    @Test
    @DisplayName("LAB-03-01: deposit increments balance with $inc")
    void deposit_incrementsBalance() {
        BankAccount saved = createAndSaveAccount("ACC-100", "Alice", AccountType.SAVINGS,
                new BigDecimal("1000.00"));

        bankAccountService.deposit(saved.getId(), new BigDecimal("500.00"));

        BankAccount updated = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("LAB-03-02: withdraw decrements balance with $inc negative")
    void withdraw_decrementsBalance() {
        BankAccount saved = createAndSaveAccount("ACC-101", "Bob", AccountType.CHECKING,
                new BigDecimal("2000.00"));

        bankAccountService.withdraw(saved.getId(), new BigDecimal("300.00"));

        BankAccount updated = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("1700.00"));
    }

    @Test
    @DisplayName("LAB-03-03: freezeAccount sets status with $set")
    void freezeAccount_setsStatus() {
        BankAccount saved = createAndSaveAccount("ACC-102", "Charlie", AccountType.SAVINGS,
                new BigDecimal("5000.00"));

        bankAccountService.freezeAccount(saved.getId());

        BankAccount updated = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    @DisplayName("LAB-03-04: findAndCloseAccount returns old state (findAndModify)")
    void findAndCloseAccount_returnsOldState() {
        BankAccount saved = createAndSaveAccount("ACC-103", "Diana", AccountType.SAVINGS,
                new BigDecimal("3000.00"));

        BankAccount oldState = bankAccountService.findAndCloseAccount(saved.getId());

        assertThat(oldState.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        BankAccount newState = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(newState.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(newState.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("LAB-03-05: updateMulti adds interest to all savings accounts")
    void updateMulti_addInterestToAllSavingsAccounts() {
        createAndSaveAccount("ACC-110", "Alice", AccountType.SAVINGS, new BigDecimal("1000.00"));
        createAndSaveAccount("ACC-111", "Bob", AccountType.SAVINGS, new BigDecimal("2000.00"));
        createAndSaveAccount("ACC-112", "Charlie", AccountType.CHECKING, new BigDecimal("3000.00"));

        bankAccountService.addInterestToAll(AccountType.SAVINGS, new BigDecimal("100.00"));

        BankAccount alice = bankAccountRepository.findByHolderName("Alice").getFirst();
        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("1100.00"));

        BankAccount bob = bankAccountRepository.findByHolderName("Bob").getFirst();
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("2100.00"));

        BankAccount checking = bankAccountRepository.findByType(AccountType.CHECKING).getFirst();
        assertThat(checking.getBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("LAB-03-06: partial update does not overwrite other fields")
    void partialUpdate_doesNotOverwriteOtherFields() {
        BankAccount saved = createAndSaveAccount("ACC-120", "Eve", AccountType.SAVINGS,
                new BigDecimal("5000.00"));

        // Partial update: only change holderName via MongoTemplate
        Query query = Query.query(Criteria.where("id").is(saved.getId()));
        Update update = new Update().set("holderName", "Eve Wang");
        mongoTemplate.updateFirst(query, update, BankAccount.class);

        BankAccount updated = bankAccountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getHolderName()).isEqualTo("Eve Wang");
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(updated.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    // --- E-commerce: MongoTemplate array/map operations ---

    @Test
    @DisplayName("LAB-03-07: addTag pushes to array with $push")
    void addTag_pushesToArray() {
        Product saved = createAndSaveProduct("SKU-200", "Laptop", "Electronics",
                new BigDecimal("999.99"), List.of("computer"));

        productService.addTag(saved.getId(), "portable");

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTags()).containsExactly("computer", "portable");
    }

    @Test
    @DisplayName("LAB-03-08: removeTag pulls from array with $pull")
    void removeTag_pullsFromArray() {
        Product saved = createAndSaveProduct("SKU-201", "Mouse", "Electronics",
                new BigDecimal("29.99"), List.of("accessory", "wireless", "ergonomic"));

        productService.removeTag(saved.getId(), "wireless");

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTags()).containsExactly("accessory", "ergonomic");
        assertThat(updated.getTags()).doesNotContain("wireless");
    }

    @Test
    @DisplayName("LAB-03-09: updatePrice sets new price with $set")
    void updatePrice_setsNewPrice() {
        Product saved = createAndSaveProduct("SKU-202", "Keyboard", "Electronics",
                new BigDecimal("149.99"), List.of("accessory"));

        productService.updatePrice(saved.getId(), new BigDecimal("129.99"));

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("129.99"));
    }

    @Test
    @DisplayName("LAB-03-10: addSpecification sets map entry with $set")
    void addSpecification_setsMapEntry() {
        Product saved = createAndSaveProduct("SKU-203", "Monitor", "Electronics",
                new BigDecimal("399.99"), List.of("display"));

        productService.addSpecification(saved.getId(), "resolution", "4K");
        productService.addSpecification(saved.getId(), "size", "27 inch");

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getSpecifications())
                .containsEntry("resolution", "4K")
                .containsEntry("size", "27 inch");
    }

    @Test
    @DisplayName("LAB-03-11: markOutOfStock sets boolean with $set")
    void markOutOfStock_setsBoolean() {
        Product saved = createAndSaveProduct("SKU-204", "Webcam", "Electronics",
                new BigDecimal("79.99"), List.of("camera"));
        assertThat(saved.isInStock()).isTrue();

        productService.markOutOfStock(saved.getId());

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.isInStock()).isFalse();
    }

    @Test
    @DisplayName("LAB-03-12: upsert inserts when product does not exist")
    void upsertProduct_insertsWhenNotExists() {
        productService.upsertProduct("SKU-NEW", "New Product", "Gadgets", new BigDecimal("99.99"));

        List<Product> found = productRepository.findByCategory("Gadgets");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getSku()).isEqualTo("SKU-NEW");
        assertThat(found.getFirst().getName()).isEqualTo("New Product");
    }

    @Test
    @DisplayName("LAB-03-13: upsert updates when product already exists")
    void upsertProduct_updatesWhenExists() {
        createAndSaveProduct("SKU-300", "Old Name", "Gadgets",
                new BigDecimal("50.00"), List.of("gadget"));

        productService.upsertProduct("SKU-300", "Updated Name", "Gadgets", new BigDecimal("75.00"));

        List<Product> all = productRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getName()).isEqualTo("Updated Name");
        assertThat(all.getFirst().getPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("LAB-03-14: insert throws on duplicate, save upserts")
    void mongoTemplate_insert_vs_save() {
        Product product = new Product("SKU-400", "Widget", "Toys", new BigDecimal("9.99"), true);
        mongoTemplate.insert(product);

        // insert with same id throws DuplicateKeyException
        Product duplicate = new Product("SKU-400-dup", "Widget Copy", "Toys",
                new BigDecimal("9.99"), true);
        duplicate.setId(product.getId());
        assertThatThrownBy(() -> mongoTemplate.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);

        // save with same id updates (upserts)
        product.setName("Updated Widget");
        mongoTemplate.save(product);

        Product updated = mongoTemplate.findById(product.getId(), Product.class);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Updated Widget");
    }
}
