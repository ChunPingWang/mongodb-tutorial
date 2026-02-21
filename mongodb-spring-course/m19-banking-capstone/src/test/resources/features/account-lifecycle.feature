Feature: 個人銀行帳戶生命週期

  Scenario: 開戶並存入資金
    When 開立帳戶 "ACC-001" 持有人 "王小明" 初始存款 10000 元
    And 存入 5000 元到帳戶 "ACC-001" 備註 "薪資入帳"
    Then 帳戶 "ACC-001" 餘額為 15000 元
    And 帳戶摘要 "ACC-001" 交易次數為 2

  Scenario: 提款並驗證餘額更新
    Given 已開立帳戶 "ACC-002" 初始存款 50000 元
    When 從帳戶 "ACC-002" 提款 20000 元
    Then 帳戶 "ACC-002" 餘額為 30000 元

  Scenario: 帳戶加計利息
    Given 已開立帳戶 "ACC-003" 初始存款 100000 元
    When 帳戶 "ACC-003" 加計利息 500 元
    Then 帳戶 "ACC-003" 餘額為 100500 元
    And 帳戶摘要 "ACC-003" 累計利息為 500 元

  Scenario: 結清帳戶
    Given 已開立帳戶 "ACC-005" 初始存款 10000 元
    And 從帳戶 "ACC-005" 提款 10000 元
    When 關閉帳戶 "ACC-005"
    Then 帳戶摘要 "ACC-005" 已標記關閉
