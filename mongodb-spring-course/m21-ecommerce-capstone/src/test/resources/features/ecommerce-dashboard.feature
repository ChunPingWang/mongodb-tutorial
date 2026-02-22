Feature: 電商營運儀表板

  Scenario: 依類別查詢銷售統計
    Given 商品 "SHIRT-001" 名稱 "T恤" 類別 "Clothing" 價格 500 元庫存 100 件
    And 商品 "JACKET-001" 名稱 "外套" 類別 "Clothing" 價格 2000 元庫存 50 件
    And 已下單訂單 "ORD-D01" 購買 2 件 "SHIRT-001"
    And 已下單訂單 "ORD-D02" 購買 1 件 "JACKET-001"
    When 查詢類別 "Clothing" 的銷售統計
    Then 該類別總訂單數為 2
    And 該類別總營收為 3000 元

  Scenario: 訂單儀表板顯示時間軸
    Given 商品 "WATCH-001" 名稱 "手錶" 類別 "Electronics" 價格 8000 元庫存 30 件
    And 已下單訂單 "ORD-D03" 購買 1 件 "WATCH-001"
    And 訂單 "ORD-D03" 已保留庫存
    And 訂單 "ORD-D03" 已處理付款
    When 查詢訂單儀表板 "ORD-D03"
    Then 時間軸包含 3 筆紀錄

  Scenario: 慢查詢偵測器捕獲查詢
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 商品 "PEN-001" 名稱 "鋼筆" 類別 "Electronics" 價格 200 元庫存 200 件
    And 已下單訂單 "ORD-D04" 購買 1 件 "PEN-001"
    When 查詢類別 "Electronics" 的銷售統計
    Then 慢查詢偵測器應捕獲至少 1 筆紀錄
