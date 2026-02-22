Feature: Aggregation Pipeline 基礎操作
  使用 MongoTemplate Aggregation API 進行分組統計

  Scenario: 依帳戶類型統計活躍帳戶數量
    Given 系統中有以下銀行帳戶資料
      | accountNumber | holderName | type     | balance | status |
      | ACC-001       | Alice      | SAVINGS  | 50000   | ACTIVE |
      | ACC-002       | Bob        | CHECKING | 15000   | ACTIVE |
      | ACC-003       | Charlie    | SAVINGS  | 80000   | ACTIVE |
      | ACC-004       | David      | CHECKING | 25000   | CLOSED |
    When 我以 Aggregation 統計活躍帳戶依類型的數量
    Then SAVINGS 類型應有 2 個帳戶
    And CHECKING 類型應有 1 個帳戶

  Scenario: 計算各保單類型的平均保費
    Given 系統中有以下保險保單資料
      | policyNumber | holderName | policyType | premium | status |
      | POL-001      | Alice      | TERM_LIFE  | 1200    | ACTIVE |
      | POL-002      | Bob        | HEALTH     | 800     | ACTIVE |
      | POL-003      | Charlie    | TERM_LIFE  | 1500    | ACTIVE |
      | POL-004      | David      | HEALTH     | 900     | ACTIVE |
    When 我以 Aggregation 計算各保單類型的保費統計
    Then TERM_LIFE 類型的保費總計應為 2700
    And HEALTH 類型的保費總計應為 1700
