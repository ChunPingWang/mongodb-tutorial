Feature: Criteria API 查詢
  使用 MongoTemplate + Criteria API 進行程式化查詢

  Scenario: 依餘額範圍查詢銀行帳戶
    Given 系統中有以下銀行帳戶
      | accountNumber | holderName | type     | balance | status |
      | ACC-001       | Alice      | SAVINGS  | 50000   | ACTIVE |
      | ACC-002       | Bob        | CHECKING | 15000   | ACTIVE |
      | ACC-003       | Charlie    | SAVINGS  | 80000   | FROZEN |
      | ACC-004       | David      | CHECKING | 3000    | CLOSED |
    When 我查詢餘額在 10000 到 60000 之間的帳戶
    Then 應該回傳 2 個帳戶
    And 回傳的帳戶餘額都在範圍內

  Scenario: 動態多條件查詢保險保單
    Given 系統中有以下保險保單
      | policyNumber | holderName | policyType | premium | status |
      | POL-001      | Alice      | HEALTH     | 800     | ACTIVE |
      | POL-002      | Bob        | TERM_LIFE  | 1200    | ACTIVE |
      | POL-003      | Charlie    | HEALTH     | 900     | EXPIRED |
      | POL-004      | David      | TERM_LIFE  | 1500    | ACTIVE |
    When 我以動態條件查詢 policyType 為 "HEALTH" 且 status 為 "ACTIVE"
    Then 應該回傳 1 個保單
    And 保單號碼為 "POL-001"
