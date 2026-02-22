Feature: 多型金融商品管理

  Scenario: 儲存不同類型的金融商品到同一 Collection
    Given 已儲存定存商品 "一年定存" 金額 100000 年利率 1.5
    And 已儲存基金商品 "全球股票" 金額 50000 淨值 15.32 風險等級 5
    And 已儲存保險商品 "年金險" 金額 200000 繳費年期 20 保額 1000000
    When 統一查詢所有金融商品
    Then 返回 3 筆商品
    And 篩選定存類型返回 1 筆

  Scenario: TypeAlias 控制 _class 欄位值
    Given 已儲存定存商品 "三年定存" 金額 100000 年利率 1.8
    When 查詢該商品的原始 BSON 文件
    Then _class 欄位值為 "deposit"

  Scenario: Pattern Matching 計算預估年報酬
    Given 已儲存定存商品 "高利定存" 金額 100000 年利率 2.0
    And 已儲存基金商品 "穩健基金" 金額 100000 淨值 50.0 風險等級 3
    When 計算各商品的預估年報酬
    Then "高利定存" 預估年報酬為 2000
    And "穩健基金" 預估年報酬為 8000
