Feature: 訂單狀態變更稽核日誌

  Background:
    Given 訂單變更監視器已啟動

  Scenario: 訂單狀態變更記錄稽核日誌
    Given 已建立客戶 "CUST-001" 金額 10000 元的訂單
    When 將訂單狀態轉換為 "CONFIRMED"
    Then 稽核日誌應有 1 筆記錄
    And 最新稽核記錄狀態為 "CONFIRMED"

  Scenario: 多次狀態轉換全部追蹤
    Given 已建立客戶 "CUST-002" 金額 20000 元的訂單
    When 將訂單狀態轉換為 "CONFIRMED"
    And 將訂單狀態轉換為 "SHIPPED"
    And 將訂單狀態轉換為 "DELIVERED"
    Then 稽核日誌應有 3 筆記錄

  Scenario: 恢復令牌自動儲存
    Given 已建立客戶 "CUST-003" 金額 15000 元的訂單
    When 將訂單狀態轉換為 "CONFIRMED"
    Then 恢復令牌應已儲存

  Scenario: 從恢復令牌繼續監聽
    Given 已建立客戶 "CUST-004" 金額 30000 元的訂單
    And 將訂單狀態轉換為 "CONFIRMED"
    And 稽核日誌應有 1 筆記錄
    When 訂單變更監視器已停止
    And 將訂單狀態轉換為 "SHIPPED"
    And 從恢復令牌重新啟動監視器
    Then 稽核日誌應有 2 筆記錄

  Scenario: 僅追蹤狀態變更忽略其他更新
    Given 已建立客戶 "CUST-005" 金額 25000 元的訂單
    When 更新訂單金額為 30000 元
    And 將訂單狀態轉換為 "CONFIRMED"
    Then 稽核日誌應有 1 筆記錄
    And 最新稽核記錄狀態為 "CONFIRMED"
