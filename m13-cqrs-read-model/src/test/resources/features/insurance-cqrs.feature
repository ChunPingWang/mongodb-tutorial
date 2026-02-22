Feature: 保險理賠 CQRS 讀取模型

  Scenario: 提出理賠後儀表板自動建立
    When 提出理賠 "CLM-001" 保單 "POL-001" 理賠人 "李小花" 金額 200000 類別 "Medical"
    Then 理賠儀表板 "CLM-001" 狀態為 "FILED"
    And 理賠儀表板 "CLM-001" 時間線包含 1 筆記錄

  Scenario: 理賠流程進行中儀表板即時更新
    Given 已提出理賠 "CLM-002" 金額 300000 類別 "Accident"
    When 調查理賠 "CLM-002"
    And 評估理賠 "CLM-002" 金額 250000
    And 核准理賠 "CLM-002" 金額 250000
    Then 理賠儀表板 "CLM-002" 狀態為 "APPROVED"
    And 理賠儀表板 "CLM-002" 時間線包含 4 筆記錄

  Scenario: 依狀態查詢理賠
    Given 已提出理賠 "CLM-QS1" 金額 100000 類別 "Medical"
    And 已提出理賠 "CLM-QS2" 金額 200000 類別 "Accident"
    And 理賠 "CLM-QS2" 已調查並拒絕
    When 查詢狀態為 "FILED" 的理賠
    Then 查詢結果包含 1 筆理賠

  Scenario: 理賠統計依類別彙總
    Given 已提出理賠 "CLM-ST1" 金額 100000 類別 "Medical"
    And 已提出理賠 "CLM-ST2" 金額 200000 類別 "Medical"
    And 已提出理賠 "CLM-ST3" 金額 300000 類別 "Accident"
    When 查詢 "Medical" 類別統計
    Then 該類別理賠總數為 2
    And 該類別理賠總金額為 300000

  Scenario: 重建保險讀取模型恢復一致性
    Given 已提出理賠 "CLM-RB1" 金額 200000 類別 "Medical"
    And 已調查理賠 "CLM-RB1"
    When 清除保險讀取模型
    Then 理賠儀表板 "CLM-RB1" 不存在
    When 重建保險讀取模型
    Then 理賠儀表板 "CLM-RB1" 狀態為 "UNDER_INVESTIGATION"
    And 理賠統計 "Medical" 總數為 1
