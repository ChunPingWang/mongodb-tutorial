Feature: 電商訂單生命週期

  Scenario: 下單並驗證事件
    Given 商品 "LAPTOP-001" 名稱 "筆記型電腦" 類別 "Electronics" 價格 35000 元庫存 50 件
    And 商品 "MOUSE-001" 名稱 "無線滑鼠" 類別 "Electronics" 價格 500 元庫存 100 件
    When 客戶 "CUST-001" 下單購買 1 件 "LAPTOP-001" 和 2 件 "MOUSE-001" 寄送至 "台北市"
    Then 訂單狀態為 "PLACED"
    And 訂單總金額為 36000 元
    And 事件數量為 1

  Scenario: 訂單確認後出貨
    Given 已建立並確認訂單 "ORD-001"
    When 訂單 "ORD-001" 以追蹤號碼 "TW123456789" 出貨
    Then 訂單 "ORD-001" 狀態為 "SHIPPED"
    And 事件數量為 5

  Scenario: 取消訂單並記錄原因
    Given 已下單訂單 "ORD-002" 狀態為 "PLACED"
    When 取消訂單 "ORD-002" 原因 "客戶要求取消"
    Then 訂單 "ORD-002" 狀態為 "CANCELLED"

  Scenario: 從事件重播重建訂單
    Given 訂單 "ORD-003" 已經歷下單到確認的 4 個事件
    When 從事件重播訂單 "ORD-003"
    Then 重建訂單狀態為 "CONFIRMED"
