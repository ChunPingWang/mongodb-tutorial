package com.mongodb.course.m12.bdd;

import com.mongodb.course.m12.infrastructure.EventStore;
import com.mongodb.course.m12.insurance.event.ClaimEvent;
import com.mongodb.course.m12.insurance.service.ClaimProcessService;
import io.cucumber.java.Before;
import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClaimProcessSteps {

    @Autowired
    ClaimProcessService claimProcessService;

    @Autowired
    EventStore eventStore;

    @Autowired
    MongoTemplate mongoTemplate;

    private List<ClaimEvent> lastAuditTrail;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m12_claim_events");
        mongoTemplate.remove(new Query(), "m12_snapshots");
        lastAuditTrail = null;
    }

    @當("提出理賠 {string} 保單 {string} 理賠人 {string} 金額 {int} 類別 {string}")
    public void 提出理賠(String claimId, String policyId, String claimantName,
                       int amount, String category) {
        claimProcessService.fileClaim(claimId, policyId, claimantName,
                new BigDecimal(amount), category);
    }

    @當("調查理賠 {string} 調查員 {string} 結果 {string}")
    public void 調查理賠(String claimId, String investigatorName, String findings) {
        claimProcessService.investigate(claimId, investigatorName, findings);
    }

    @那麼("理賠 {string} 狀態為 {string}")
    public void 理賠狀態為(String claimId, String expectedStatus) {
        var claim = claimProcessService.loadClaim(claimId);
        assertThat(claim.getStatus().name()).isEqualTo(expectedStatus);
    }

    @那麼("理賠 {string} 事件數量為 {int}")
    public void 理賠事件數量為(String claimId, int expectedCount) {
        var events = claimProcessService.getEventHistory(claimId);
        assertThat(events).hasSize(expectedCount);
    }

    @假設("已提出理賠 {string} 金額 {int}")
    public void 已提出理賠(String claimId, int amount) {
        claimProcessService.fileClaim(claimId, "POL-DEFAULT", "測試理賠人",
                new BigDecimal(amount), "General");
    }

    @假設("已完成調查理賠 {string}")
    public void 已完成調查理賠(String claimId) {
        claimProcessService.investigate(claimId, "調查員", "調查完成");
    }

    @假設("已評估理賠 {string} 金額 {int}")
    public void 已評估理賠(String claimId, int amount) {
        claimProcessService.assess(claimId, "評估員", new BigDecimal(amount), "評估完成");
    }

    @當("核准理賠 {string} 金額 {int}")
    public void 核准理賠(String claimId, int amount) {
        claimProcessService.approve(claimId, new BigDecimal(amount), "核准主管");
    }

    @當("支付理賠 {string} 金額 {int}")
    public void 支付理賠(String claimId, int amount) {
        claimProcessService.pay(claimId, new BigDecimal(amount), "PAY-REF-001");
    }

    @當("拒絕理賠 {string} 原因 {string}")
    public void 拒絕理賠(String claimId, String reason) {
        claimProcessService.reject(claimId, reason, "拒絕主管");
    }

    @假設("已核准理賠 {string} 金額 {int}")
    public void 已核准理賠(String claimId, int amount) {
        claimProcessService.approve(claimId, new BigDecimal(amount), "核准主管");
    }

    @當("查詢理賠 {string} 稽核軌跡")
    public void 查詢稽核軌跡(String claimId) {
        lastAuditTrail = claimProcessService.getEventHistory(claimId);
    }

    @那麼("稽核軌跡包含 {int} 筆事件")
    public void 稽核軌跡包含事件(int expectedCount) {
        assertThat(lastAuditTrail).hasSize(expectedCount);
    }

    @那麼("稽核軌跡事件類型依序為 {string}")
    public void 稽核軌跡事件類型依序為(String expectedTypes) {
        var expected = Arrays.asList(expectedTypes.split(","));
        var actual = lastAuditTrail.stream()
                .map(e -> e.getClass().getSimpleName())
                .toList();
        assertThat(actual).isEqualTo(expected);
    }
}
