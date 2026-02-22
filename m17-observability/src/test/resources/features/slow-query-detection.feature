Feature: 銀行交易慢查詢偵測與自動指標

  Scenario: 低門檻值捕獲所有查詢
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 慢查詢偵測器已清除歷史紀錄
    When 新增一筆帳戶 "ACC-001" 金額 5000 元的交易
    Then 慢查詢偵測器應捕獲至少 1 筆紀錄

  Scenario: 高門檻值不捕獲任何查詢
    Given 慢查詢偵測器門檻值設定為 999999 毫秒
    And 慢查詢偵測器已清除歷史紀錄
    When 新增一筆帳戶 "ACC-002" 金額 3000 元的交易
    And 查詢帳戶 "ACC-002" 的所有交易
    Then 慢查詢偵測器應捕獲 0 筆紀錄

  Scenario: 捕獲的慢查詢包含指令名稱
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 慢查詢偵測器已清除歷史紀錄
    When 查詢帳戶 "ACC-003" 的所有交易
    Then 捕獲的慢查詢應包含指令名稱 "find"

  Scenario: 捕獲的慢查詢包含資料庫名稱
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 慢查詢偵測器已清除歷史紀錄
    When 新增一筆帳戶 "ACC-004" 金額 8000 元的交易
    Then 捕獲的慢查詢資料庫名稱不為空

  Scenario: 自動指標包含 MongoDB 指令計時器
    When 新增一筆帳戶 "ACC-005" 金額 1000 元的交易
    Then MeterRegistry 應包含 "mongodb.driver.commands" 計時器
