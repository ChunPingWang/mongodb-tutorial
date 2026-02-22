Feature: 銀行帳戶 CQRS 讀取模型

  Scenario: 開戶後讀取模型自動建立
    When 開立帳戶 "ACC-001" 持有人 "王小明" 初始餘額 10000 元
    Then 帳戶摘要 "ACC-001" 餘額為 10000 元
    And 帳戶摘要 "ACC-001" 交易次數為 1

  Scenario: 存提款後讀取模型即時更新
    Given 已開立帳戶 "ACC-002" 初始餘額 50000 元
    When 存入 20000 元到帳戶 "ACC-002"
    And 提款 10000 元從帳戶 "ACC-002"
    Then 帳戶摘要 "ACC-002" 餘額為 60000 元
    And 帳戶摘要 "ACC-002" 存款次數為 1
    And 帳戶摘要 "ACC-002" 提款次數為 1

  Scenario: 交易歷史完整記錄
    Given 已開立帳戶 "ACC-003" 初始餘額 10000 元
    And 存入 5000 元到帳戶 "ACC-003"
    And 提款 2000 元從帳戶 "ACC-003"
    When 查詢帳戶 "ACC-003" 交易歷史
    Then 交易歷史包含 3 筆記錄
    And 最後一筆交易餘額為 13000 元

  Scenario: 依餘額排行查詢帳戶
    Given 已開立帳戶 "ACC-R01" 初始餘額 50000 元
    And 已開立帳戶 "ACC-R02" 初始餘額 80000 元
    And 已開立帳戶 "ACC-R03" 初始餘額 30000 元
    When 查詢餘額前 2 名帳戶
    Then 排行結果依序為 "ACC-R02,ACC-R01"

  Scenario: 重建讀取模型恢復一致性
    Given 已開立帳戶 "ACC-RB1" 初始餘額 10000 元
    And 存入 5000 元到帳戶 "ACC-RB1"
    When 清除銀行讀取模型
    Then 帳戶摘要 "ACC-RB1" 不存在
    When 重建銀行讀取模型
    Then 帳戶摘要 "ACC-RB1" 餘額為 15000 元
