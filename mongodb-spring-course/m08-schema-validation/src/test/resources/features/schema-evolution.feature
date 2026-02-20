Feature: Schema Evolution
  Schema 演進、雙層驗證與文件遷移

  Scenario: 雙層驗證分別在不同層攔截錯誤
    Given 建立 "m08_bdd_products" 集合帶有產品 Schema 驗證
    When 透過 Java 儲存 SKU 為空白的產品
    Then 應該被 Bean Validation 攔截
    When 透過原始 BSON 插入缺少 "sku" 的產品
    Then 應該被 MongoDB Schema 驗證攔截

  Scenario: 從 V1 逐步遷移到 V3
    Given 在 "m08_product_versions" 集合中插入 3 筆 V1 產品
    When 執行遷移到版本 3
    Then 所有產品的 schemaVersion 應該為 3
    And 所有產品應該包含 "category" 和 "tags" 和 "rating" 欄位
