Feature: 貸款申請 Aggregate

  Scenario: 提交貸款申請
    Given 申請人 "Alice" 年收入 1200000 元
    When 提交貸款金額 1000000 元期限 20 年利率 2.5%
    Then 貸款申請狀態為 "SUBMITTED"
    And 產生 "LoanApplicationSubmitted" 領域事件

  Scenario: 初審通過
    Given 申請人 "Bob" 年收入 1800000 元
    And 已提交貸款金額 1000000 元期限 20 年利率 2.5%
    When 執行自動初審
    Then 貸款申請狀態為 "PRELIMINARY_PASSED"

  Scenario: 初審拒絕
    Given 申請人 "Charlie" 年收入 150000 元
    And 已提交貸款金額 1000000 元期限 20 年利率 2.5%
    When 執行自動初審
    Then 貸款申請狀態為 "PRELIMINARY_REJECTED"

  Scenario: 非 SUBMITTED 狀態不可初審
    Given 申請人 "Diana" 年收入 2000000 元
    And 已提交並完成初審的貸款申請
    When 再次執行自動初審
    Then 操作失敗並回傳狀態錯誤
