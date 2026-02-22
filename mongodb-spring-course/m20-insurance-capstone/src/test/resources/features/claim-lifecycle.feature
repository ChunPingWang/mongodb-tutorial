Feature: 保險理賠生命週期

  Scenario: 提出理賠申請並進行調查
    Given 已建立汽車保單 "POL-001" 持有人 "王小明" 保額 500000 元
    When 以保單 "POL-001" 提出理賠 "CLM-001" 金額 50000 元類別 "AUTO"
    And 理賠 "CLM-001" 完成調查結果為 "LOW" 風險
    Then 理賠 "CLM-001" 狀態為 "UNDER_INVESTIGATION"
    And 理賠事件數量為 2

  Scenario: 評估理賠金額
    Given 已建立汽車保單 "POL-002" 持有人 "李小華" 保額 300000 元
    And 已提出理賠 "CLM-002" 保單 "POL-002" 金額 80000 元
    And 理賠 "CLM-002" 完成調查結果為 "LOW" 風險
    When 理賠 "CLM-002" 評估金額為 60000 元
    Then 理賠 "CLM-002" 狀態為 "ASSESSED"
    And 理賠儀表板 "CLM-002" 評估金額為 60000 元

  Scenario: 高風險理賠被拒絕
    Given 已建立健康保單 "POL-003" 持有人 "張大為" 保額 1000000 元
    And 已提出理賠 "CLM-003" 保單 "POL-003" 金額 200000 元
    When 理賠 "CLM-003" 完成調查結果為 "HIGH" 風險
    And 拒絕理賠 "CLM-003" 原因 "調查發現詐欺跡象"
    Then 理賠 "CLM-003" 狀態為 "REJECTED"

  Scenario: 完整理賠流程至付款
    Given 已建立壽險保單 "POL-004" 持有人 "陳美玲" 保額 2000000 元
    And 已提出理賠 "CLM-004" 保單 "POL-004" 金額 500000 元
    And 理賠 "CLM-004" 完成調查結果為 "LOW" 風險
    And 理賠 "CLM-004" 評估金額為 450000 元
    When 核准理賠 "CLM-004" 金額 450000 元
    And 支付理賠 "CLM-004" 金額 450000 元參考號 "PAY-20240101"
    Then 理賠 "CLM-004" 狀態為 "PAID"
    And 理賠事件數量為 5
