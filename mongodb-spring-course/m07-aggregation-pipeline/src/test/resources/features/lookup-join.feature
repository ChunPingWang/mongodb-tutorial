Feature: $lookup 跨集合 Join
  使用 $lookup 實現 MongoDB 跨集合關聯查詢

  Scenario: 透過 $lookup 關聯訂單與商品資料
    Given 系統中有以下商品資料
      | sku     | name           | category    | price  |
      | SKU-001 | Wireless Mouse | Electronics | 29.99  |
      | SKU-002 | Keyboard       | Electronics | 89.99  |
      | SKU-003 | Java Book      | Books       | 45.00  |
    And 系統中有以下訂單資料
      | orderNumber | customerName | totalAmount | status    | items                              |
      | ORD-001     | Alice        | 150         | DELIVERED | SKU-001:Wireless Mouse:2:29.99,SKU-002:Keyboard:1:89.99 |
      | ORD-002     | Bob          | 45          | SHIPPED   | SKU-003:Java Book:1:45.00          |
    When 我使用 $lookup 關聯訂單與商品
    Then 訂單 "ORD-001" 應包含 2 筆關聯商品資料
    And 訂單 "ORD-002" 應包含 1 筆關聯商品資料

  Scenario: 客戶訂單匯總統計
    Given 系統中有以下訂單資料
      | orderNumber | customerName | totalAmount | status    | items                     |
      | ORD-001     | Alice        | 500         | DELIVERED | SKU-001:Mouse:1:29.99     |
      | ORD-002     | Bob          | 300         | SHIPPED   | SKU-002:Keyboard:1:89.99  |
      | ORD-003     | Alice        | 200         | DELIVERED | SKU-003:Book:1:45.00      |
    When 我以 Aggregation 計算客戶訂單匯總
    Then "Alice" 的訂單數為 2 且消費總額為 700
    And "Bob" 的訂單數為 1 且消費總額為 300
