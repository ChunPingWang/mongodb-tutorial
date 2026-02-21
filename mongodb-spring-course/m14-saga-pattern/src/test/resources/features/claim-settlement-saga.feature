Feature: 保險理賠結算 Saga 流程

  Scenario: 理賠結算成功完成
    Given 保單 "POL-001" 持有人 "王小明" 保額 500000 已理賠 100000
    And 理賠案 "CLM-001" 保單 "POL-001" 理賠人 "王小明" 金額 50000
    When 執行理賠結算 "CLM-001"
    Then 結算 Saga 狀態為 "COMPLETED"
    And 理賠案 "CLM-001" 結算狀態為 "NOTIFIED"
    And 保單 "POL-001" 已理賠總額為 150000

  Scenario: 超出保額觸發補償
    Given 保單 "POL-002" 持有人 "李小華" 保額 200000 已理賠 180000
    And 理賠案 "CLM-002" 保單 "POL-002" 理賠人 "李小華" 金額 50000
    When 執行理賠結算 "CLM-002"
    Then 結算 Saga 狀態為 "COMPENSATED"
    And 理賠案 "CLM-002" 結算狀態為 "PENDING"
    And 保單 "POL-002" 已理賠總額為 180000

  Scenario: 通知失敗觸發補償
    Given 保單 "POL-003" 持有人 "FAIL_張三" 保額 500000 已理賠 0
    And 理賠案 "CLM-003" 保單 "POL-003" 理賠人 "FAIL_張三" 金額 30000
    When 執行理賠結算 "CLM-003"
    Then 結算 Saga 狀態為 "COMPENSATED"
    And 理賠案 "CLM-003" 結算狀態為 "PENDING"
    And 保單 "POL-003" 已理賠總額為 0

  Scenario: 結算 Saga 日誌完整記錄
    Given 保單 "POL-004" 持有人 "陳小美" 保額 500000 已理賠 0
    And 理賠案 "CLM-004" 保單 "POL-004" 理賠人 "陳小美" 金額 40000
    When 執行理賠結算 "CLM-004"
    Then 結算 Saga 日誌包含 4 個步驟
    And 結算 Saga 每個步驟狀態為 "SUCCEEDED"

  Scenario: 補償日誌記錄反向補償
    Given 保單 "POL-005" 持有人 "FAIL_林小明" 保額 500000 已理賠 0
    And 理賠案 "CLM-005" 保單 "POL-005" 理賠人 "FAIL_林小明" 金額 20000
    When 執行理賠結算 "CLM-005"
    Then 結算 Saga 狀態為 "COMPENSATED"
    And 結算 Saga 日誌中 "APPROVE_CLAIM" 步驟狀態為 "COMPENSATED"
    And 結算 Saga 日誌中 "NOTIFY_CUSTOMER" 步驟狀態為 "FAILED"
