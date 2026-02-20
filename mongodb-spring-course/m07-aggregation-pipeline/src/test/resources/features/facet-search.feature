Feature: $facet 多面向查詢
  使用 $facet 實現平行多面向分析與搜尋

  Scenario: 商品搜尋含總數與統計資訊
    Given 系統中有以下商品可供搜尋
      | sku     | name           | category    | price  | inStock |
      | SKU-001 | Wireless Mouse | Electronics | 29.99  | true    |
      | SKU-002 | Keyboard       | Electronics | 89.99  | true    |
      | SKU-003 | Monitor        | Electronics | 350.00 | false   |
      | SKU-004 | Java Book      | Books       | 45.00  | true    |
    When 我以 $facet 搜尋 "Electronics" 類別商品
    Then 搜尋結果總數應為 3
    And 搜尋結果應包含分頁資料與價格統計

  Scenario: 銀行帳戶多面向儀表板
    Given 系統中有以下銀行帳戶可供分析
      | accountNumber | holderName | type     | balance | status |
      | ACC-001       | Alice      | SAVINGS  | 50000   | ACTIVE |
      | ACC-002       | Bob        | CHECKING | 15000   | ACTIVE |
      | ACC-003       | Charlie    | SAVINGS  | 80000   | CLOSED |
      | ACC-004       | David      | CHECKING | 25000   | ACTIVE |
    When 我以 $facet 產生銀行帳戶儀表板
    Then 儀表板應包含狀態統計與類型統計
    And 儀表板應包含餘額統計資訊
