package com.mongodb.course.m12.bdd;

import com.mongodb.course.m12.banking.event.AccountEvent;
import com.mongodb.course.m12.banking.model.BankAccount;
import com.mongodb.course.m12.banking.service.BankAccountService;
import com.mongodb.course.m12.infrastructure.EventStore;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.並且;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BankAccountSteps {

    @Autowired
    BankAccountService bankAccountService;

    @Autowired
    EventStore eventStore;

    @Autowired
    MongoTemplate mongoTemplate;

    private BankAccount lastLoadedAccount;
    private Exception lastException;

    @Before
    public void cleanUp() {
        mongoTemplate.remove(new Query(), "m12_account_events");
        mongoTemplate.remove(new Query(), "m12_snapshots");
        lastLoadedAccount = null;
        lastException = null;
    }

    @當("開立帳戶 {string} 持有人 {string} 初始餘額 {int} 元")
    public void 開立帳戶(String accountId, String holder, int balance) {
        bankAccountService.openAccount(accountId, holder, new BigDecimal(balance), "TWD");
    }

    @當("存入 {int} 元到帳戶 {string}")
    public void 存入到帳戶(int amount, String accountId) {
        bankAccountService.deposit(accountId, new BigDecimal(amount), "存款");
    }

    @那麼("帳戶 {string} 餘額為 {int} 元")
    public void 帳戶餘額為(String accountId, int expectedBalance) {
        var account = bankAccountService.loadAccount(accountId);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal(expectedBalance));
    }

    @那麼("帳戶 {string} 事件數量為 {int}")
    public void 帳戶事件數量為(String accountId, int expectedCount) {
        var events = bankAccountService.getEventHistory(accountId);
        assertThat(events).hasSize(expectedCount);
    }

    @假設("帳戶 {string} 依序發生以下事件")
    public void 帳戶依序發生以下事件(String accountId, DataTable dataTable) {
        List<List<String>> rows = dataTable.asLists();
        for (int i = 1; i < rows.size(); i++) {
            String eventType = rows.get(i).get(0);
            int amount = Integer.parseInt(rows.get(i).get(1));
            switch (eventType) {
                case "開戶" -> bankAccountService.openAccount(accountId, "測試用戶",
                        new BigDecimal(amount), "TWD");
                case "存款" -> bankAccountService.deposit(accountId, new BigDecimal(amount), "存款");
                case "提款" -> bankAccountService.withdraw(accountId, new BigDecimal(amount), "提款");
                case "轉帳" -> bankAccountService.transfer(accountId, new BigDecimal(amount),
                        "TARGET-001", "轉帳");
                default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
            }
        }
    }

    @當("從事件重播帳戶 {string}")
    public void 從事件重播帳戶(String accountId) {
        lastLoadedAccount = bankAccountService.loadAccount(accountId);
    }

    @那麼("帳戶 {string} 版本號為 {int}")
    public void 帳戶版本號為(String accountId, int expectedVersion) {
        var account = (lastLoadedAccount != null && lastLoadedAccount.getAccountId().equals(accountId))
                ? lastLoadedAccount
                : bankAccountService.loadAccount(accountId);
        assertThat(account.getVersion()).isEqualTo(expectedVersion);
    }

    @假設("已開立帳戶 {string} 初始餘額 {int} 元")
    public void 已開立帳戶(String accountId, int balance) {
        bankAccountService.openAccount(accountId, "測試用戶", new BigDecimal(balance), "TWD");
    }

    @當("嘗試從帳戶 {string} 提款 {int} 元")
    public void 嘗試提款(String accountId, int amount) {
        try {
            bankAccountService.withdraw(accountId, new BigDecimal(amount), "提款");
        } catch (Exception e) {
            lastException = e;
        }
    }

    @那麼("操作失敗並回傳餘額不足錯誤")
    public void 操作失敗並回傳餘額不足錯誤() {
        assertThat(lastException).isInstanceOf(IllegalStateException.class);
        assertThat(lastException.getMessage()).contains("Insufficient funds");
    }

    @假設("連續存入 {int} 筆 {int} 元到帳戶 {string}")
    public void 連續存入(int count, int amount, String accountId) {
        for (int i = 0; i < count; i++) {
            bankAccountService.deposit(accountId, new BigDecimal(amount), "存款" + (i + 1));
        }
    }

    @那麼("帳戶 {string} 產生快照於版本 {int}")
    public void 帳戶產生快照(String accountId, int expectedVersion) {
        var snapshot = eventStore.loadLatestSnapshot(accountId, "BankAccount");
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().version()).isEqualTo(expectedVersion);
    }

    @當("再存入 {int} 元到帳戶 {string}")
    public void 再存入(int amount, String accountId) {
        bankAccountService.deposit(accountId, new BigDecimal(amount), "額外存款");
    }
}
