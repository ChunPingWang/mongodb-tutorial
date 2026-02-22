Feature: 保險理賠事件溯源

  Scenario: 提出理賠並完成調查
    When 提出理賠 "CLM-001" 保單 "POL-001" 理賠人 "李小花" 金額 200000 類別 "Medical"
    And 調查理賠 "CLM-001" 調查員 "張調查" 結果 "事故屬實"
    Then 理賠 "CLM-001" 狀態為 "UNDER_INVESTIGATION"
    And 理賠 "CLM-001" 事件數量為 2

  Scenario: 理賠完整生命週期
    Given 已提出理賠 "CLM-002" 金額 300000
    And 已完成調查理賠 "CLM-002"
    And 已評估理賠 "CLM-002" 金額 250000
    When 核准理賠 "CLM-002" 金額 250000
    And 支付理賠 "CLM-002" 金額 250000
    Then 理賠 "CLM-002" 狀態為 "PAID"
    And 理賠 "CLM-002" 事件數量為 5

  Scenario: 理賠拒絕中斷流程
    Given 已提出理賠 "CLM-003" 金額 500000
    And 已完成調查理賠 "CLM-003"
    When 拒絕理賠 "CLM-003" 原因 "證據不足"
    Then 理賠 "CLM-003" 狀態為 "REJECTED"

  Scenario: 事件溯源完整稽核軌跡
    Given 已提出理賠 "CLM-004" 金額 100000
    And 已完成調查理賠 "CLM-004"
    And 已評估理賠 "CLM-004" 金額 80000
    And 已核准理賠 "CLM-004" 金額 80000
    When 查詢理賠 "CLM-004" 稽核軌跡
    Then 稽核軌跡包含 4 筆事件
    And 稽核軌跡事件類型依序為 "ClaimFiled,ClaimInvestigated,ClaimAssessed,ClaimApproved"
