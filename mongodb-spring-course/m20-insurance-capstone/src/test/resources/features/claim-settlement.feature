Feature: 理賠結算 Saga 流程

  Scenario: 成功結算並更新保單
    Given 已建立汽車保單 "POL-S01" 持有人 "王小明" 保額 500000 元
    And 已提出理賠 "CLM-S01" 保單 "POL-S01" 金額 50000 元
    And 理賠 "CLM-S01" 已調查評估為 30000 元風險 "LOW"
    When 執行理賠結算 Saga 理賠 "CLM-S01"
    Then 結算 Saga 狀態為 "COMPLETED"
    And 保單 "POL-S01" 累計理賠金額為 30000 元
    And 理賠 "CLM-S01" 狀態為 "PAID"

  Scenario: 詐欺檢查失敗觸發補償
    Given 已建立健康保單 "POL-S02" 持有人 "李小華" 保額 1000000 元
    And 類別 "HEALTH" 已有 6 件理賠且核准率低於 30%
    And 已提出理賠 "CLM-S02" 保單 "POL-S02" 金額 100000 元
    And 理賠 "CLM-S02" 已調查評估為 80000 元風險 "MEDIUM"
    When 執行理賠結算 Saga 理賠 "CLM-S02"
    Then 結算 Saga 狀態為 "COMPENSATED"

  Scenario: Saga 日誌記錄四個步驟
    Given 已建立壽險保單 "POL-S03" 持有人 "張大為" 保額 2000000 元
    And 已提出理賠 "CLM-S03" 保單 "POL-S03" 金額 300000 元
    And 理賠 "CLM-S03" 已調查評估為 250000 元風險 "LOW"
    When 執行理賠結算 Saga 理賠 "CLM-S03"
    Then 結算 Saga 日誌包含 4 個步驟
