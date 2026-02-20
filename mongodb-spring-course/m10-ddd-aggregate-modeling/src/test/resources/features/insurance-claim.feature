Feature: 保險理賠 Aggregate

  Scenario: 提出理賠申請
    Given 保單 "POL-001" 保額 500000 元自負額 10000 元
    When 提出理賠項目清單總額 200000 元
    Then 理賠狀態為 "FILED"

  Scenario: 理賠金額超過保額
    Given 保單 "POL-002" 保額 500000 元自負額 10000 元
    When 提出理賠項目清單總額 600000 元
    Then 理賠申請失敗並回傳超過保額錯誤

  Scenario: 審核通過含自負額扣除
    Given 保單 "POL-003" 保額 500000 元自負額 10000 元
    And 已提出理賠總額 200000 元
    When 審核人員核定金額 190000 元
    Then 理賠狀態為 "APPROVED"
    And 核定金額為 190000 元

  Scenario: 審核金額超出限制
    Given 保單 "POL-004" 保額 500000 元自負額 10000 元
    And 已提出理賠總額 200000 元
    When 審核人員核定金額 250000 元
    Then 審核失敗並回傳金額超出限制錯誤
