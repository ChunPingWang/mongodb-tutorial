Feature: 地理空間查詢
  使用 NearQuery 與 withinSphere 進行地理空間查詢

  Scenario: 查詢台北 101 附近的商店
    Given 系統中有以下商店資料
      | name              | city       | category   | longitude  | latitude  | open  |
      | Xinyi Cafe        | Taipei     | cafe       | 121.5675   | 25.0365   | true  |
      | Da'an Bookstore   | Taipei     | bookstore  | 121.5434   | 25.0260   | true  |
      | Zhongshan Rest    | Taipei     | restaurant | 121.5225   | 25.0530   | false |
      | Banqiao Mall      | New Taipei | mall       | 121.4722   | 25.0145   | true  |
    When 我查詢台北 101 附近 3 公里內的商店
    Then 應該找到至少 2 間商店
    And 結果不包含 "Banqiao Mall"

  Scenario: 查詢附近且營業中的特定類別商店
    Given 系統中有以下商店資料
      | name              | city       | category   | longitude  | latitude  | open  |
      | Xinyi Cafe        | Taipei     | cafe       | 121.5675   | 25.0365   | true  |
      | Da'an Cafe        | Taipei     | cafe       | 121.5434   | 25.0260   | false |
      | Zhongshan Cafe    | Taipei     | cafe       | 121.5225   | 25.0530   | true  |
      | 101 Restaurant    | Taipei     | restaurant | 121.5650   | 25.0340   | true  |
    When 我查詢台北 101 附近 6 公里內營業中的 "cafe"
    Then 應該找到至少 1 間商店
    And 結果中所有商店都是營業中的 cafe
