Feature: 全文檢索
  使用 TextCriteria 進行 MongoDB 全文檢索

  Scenario: 依關鍵字搜尋產品
    Given 系統中有以下產品資料
      | sku     | name                     | description                              | category    |
      | SKU-001 | Wireless Bluetooth Mouse | compact wireless mouse with Bluetooth    | Electronics |
      | SKU-002 | Mechanical Keyboard      | RGB mechanical keyboard for gaming       | Electronics |
      | SKU-003 | Java Programming Guide   | guide to Java and Spring Boot            | Books       |
    When 我以關鍵字 "wireless" 進行全文檢索
    Then 應該搜尋到至少 1 個產品
    And 搜尋結果中包含 "Wireless Bluetooth Mouse"

  Scenario: 搜尋時排除特定關鍵字
    Given 系統中有以下產品資料
      | sku     | name                     | description                              | category    |
      | SKU-001 | Wireless Bluetooth Mouse | compact wireless mouse with Bluetooth    | Electronics |
      | SKU-002 | Wireless Headphones      | premium wireless headphones              | Electronics |
      | SKU-003 | Mechanical Keyboard      | RGB mechanical keyboard for gaming       | Electronics |
    When 我搜尋 "wireless" 但排除 "mouse"
    Then 搜尋結果不包含 "Wireless Bluetooth Mouse"
    And 搜尋結果中包含 "Wireless Headphones"
