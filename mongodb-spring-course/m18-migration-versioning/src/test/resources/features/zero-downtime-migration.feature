Feature: 零停機遷移策略

  Scenario: 多版本文件共存且均可正確讀取
    Given 資料庫中存在 3 筆 V1 和 2 筆 V2 和 1 筆 V3 客戶
    When 透過 CustomerService 讀取所有客戶
    Then 應回傳 6 筆客戶資料
    And 每筆資料的 loyaltyTier 均不為空

  Scenario: 背景批次遷移將所有文件升級至最新版本
    Given 資料庫中存在 5 筆 V1 和 3 筆 V2 客戶
    When 執行背景批次遷移至 V3
    Then 遷移數量應為 8
    And 版本統計應顯示全部為 V3

  Scenario: 遷移前後資料內容一致
    Given 資料庫中存在一筆 V1 客戶 "Diana" email 為 "diana@test.com"
    When 執行背景批次遷移至 V3
    Then "Diana" 的 email 仍為 "diana@test.com"
    And "Diana" 的地址資料保持完整
