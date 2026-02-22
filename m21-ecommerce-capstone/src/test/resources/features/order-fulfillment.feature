Feature: 訂單履約 Saga 流程

  Scenario: 成功履約並更新庫存
    Given 商品 "PHONE-001" 名稱 "手機" 類別 "Electronics" 價格 25000 元庫存 20 件
    And 已下單訂單 "ORD-F01" 購買 2 件 "PHONE-001"
    When 執行訂單履約 Saga "ORD-F01"
    Then Saga 狀態為 "COMPLETED"
    And 商品 "PHONE-001" 庫存為 18 件
    And 訂單 "ORD-F01" 狀態為 "CONFIRMED"

  Scenario: 庫存不足觸發補償
    Given 商品 "TABLET-001" 名稱 "平板" 類別 "Electronics" 價格 15000 元庫存 3 件
    And 已下單訂單 "ORD-F02" 購買 5 件 "TABLET-001"
    When 執行訂單履約 Saga "ORD-F02"
    Then Saga 狀態為 "COMPENSATED"
    And 商品 "TABLET-001" 庫存恢復為 3 件

  Scenario: Saga 日誌記錄四個步驟
    Given 商品 "CAMERA-001" 名稱 "相機" 類別 "Electronics" 價格 30000 元庫存 10 件
    And 已下單訂單 "ORD-F03" 購買 1 件 "CAMERA-001"
    When 執行訂單履約 Saga "ORD-F03"
    Then Saga 日誌包含 4 個步驟
