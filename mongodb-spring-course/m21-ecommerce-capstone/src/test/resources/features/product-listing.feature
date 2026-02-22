Feature: 商品上架管理

  Scenario: 高利潤類別商品上架成功
    Given 類別 "Electronics" 有 10 筆訂單平均金額 2000 元取消率 10%
    When 提交商品上架 SKU "NEW-001" 名稱 "新商品" 類別 "Electronics" 價格 1500 元庫存 50 件
    And 執行上架審核
    Then 上架申請狀態為 "APPROVED"
    And 商品集合中存在 SKU "NEW-001"

  Scenario: 低利潤類別商品上架被拒
    Given 類別 "Food" 有 3 筆訂單平均金額 100 元取消率 40%
    When 提交商品上架 SKU "FOOD-001" 名稱 "零食" 類別 "Food" 價格 50 元庫存 100 件
    And 執行上架審核
    Then 上架申請狀態為 "REJECTED"

  Scenario: Schema 驗證拒絕不完整商品資料
    When 直接插入缺少必填欄位的商品文件
    Then 操作失敗並拋出 MongoWriteException
