package com.mongodb.course.m08;

import com.mongodb.course.m08.banking.AccountType;
import com.mongodb.course.m08.banking.BankAccount;
import com.mongodb.course.m08.insurance.InsurancePolicyDocument;
import com.mongodb.course.m08.insurance.PolicyType;
import com.mongodb.course.m08.service.BeanValidationService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SharedContainersConfig.class)
class BeanValidationTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    BeanValidationService beanValidationService;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("m08_bank_accounts");
        mongoTemplate.dropCollection("m08_insurance_policies");
    }

    @Test
    void validBankAccount_savesSuccessfully() {
        BankAccount account = new BankAccount("ACC-12345", "Alice", AccountType.SAVINGS, new BigDecimal("5000"));

        BankAccount saved = beanValidationService.saveBankAccount(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountNumber()).isEqualTo("ACC-12345");
    }

    @Test
    void blankAccountNumber_throwsConstraintViolation() {
        BankAccount account = new BankAccount("", "Alice", AccountType.SAVINGS, new BigDecimal("5000"));

        assertThatThrownBy(() -> beanValidationService.saveBankAccount(account))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void nullBalance_throwsConstraintViolation() {
        BankAccount account = new BankAccount("ACC-12345", "Alice", AccountType.SAVINGS, null);
        account.setBalance(null);

        assertThatThrownBy(() -> beanValidationService.saveBankAccount(account))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void negativeBalance_throwsConstraintViolation() {
        BankAccount account = new BankAccount("ACC-12345", "Alice", AccountType.SAVINGS, new BigDecimal("-100"));

        assertThatThrownBy(() -> beanValidationService.saveBankAccount(account))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void blankPolicyHolderName_throwsConstraintViolation() {
        InsurancePolicyDocument policy = new InsurancePolicyDocument(
                "POL-001", "", PolicyType.HEALTH,
                new BigDecimal("500"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1)
        );

        assertThatThrownBy(() -> beanValidationService.savePolicy(policy))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void negativePremium_throwsConstraintViolation() {
        InsurancePolicyDocument policy = new InsurancePolicyDocument(
                "POL-001", "Alice Chen", PolicyType.HEALTH,
                new BigDecimal("-500"), new BigDecimal("1000000"),
                LocalDate.now(), LocalDate.now().plusYears(1)
        );

        assertThatThrownBy(() -> beanValidationService.savePolicy(policy))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
