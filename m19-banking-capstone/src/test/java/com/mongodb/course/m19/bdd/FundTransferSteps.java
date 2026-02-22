package com.mongodb.course.m19.bdd;

import com.mongodb.course.m19.infrastructure.saga.SagaLogRepository;
import com.mongodb.course.m19.transfer.TransferSagaService;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class FundTransferSteps {

    @Autowired private TransferSagaService transferSagaService;
    @Autowired private SagaLogRepository sagaLogRepository;

    private String lastSagaId;

    @When("從帳戶 {string} 轉帳 {int} 元到帳戶 {string}")
    public void transfer(String sourceId, int amount, String targetId) {
        lastSagaId = transferSagaService.transfer(sourceId, targetId, BigDecimal.valueOf(amount));
    }

    @Then("轉帳 Saga 狀態為 {string}")
    public void verifySagaStatus(String expectedStatus) {
        var sagaLog = sagaLogRepository.findById(lastSagaId).orElseThrow();
        assertThat(sagaLog.status().name()).isEqualTo(expectedStatus);
    }

    @Then("轉帳 Saga 日誌包含 {int} 個步驟")
    public void verifySagaStepCount(int expectedSteps) {
        var sagaLog = sagaLogRepository.findById(lastSagaId).orElseThrow();
        assertThat(sagaLog.steps()).hasSize(expectedSteps);
    }
}
