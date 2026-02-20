Feature: 保險多險種保單管理

  Scenario: 建立不同險種保單到同一 Collection
    When 建立車險保單 "POL-A01" 持有人 "王小明" 基本保費 12000
    And 建立壽險保單 "POL-L01" 持有人 "李小花" 基本保費 20000
    And 建立健康險保單 "POL-H01" 持有人 "張大山" 基本保費 8000
    Then 統一查詢返回 3 張保單
    And 篩選車險返回 1 張

  Scenario: 年輕駕駛車險加費
    Given 車險保單基本保費 12000 駕駛年齡 22 車型 "sedan"
    When 計算保費
    Then 保費為 18000

  Scenario: 卡車車險加費
    Given 車險保單基本保費 12000 駕駛年齡 35 車型 "truck"
    When 計算保費
    Then 保費為 15600

  Scenario: 壽險年齡因子保費
    Given 壽險保單基本保費 20000 被保人年齡 40 保期 20 年
    When 計算保費
    Then 保費為 36000

  Scenario: 健康險附加選項保費
    Given 健康險保單基本保費 8000 含牙科 含眼科
    When 計算保費
    Then 保費為 8800
