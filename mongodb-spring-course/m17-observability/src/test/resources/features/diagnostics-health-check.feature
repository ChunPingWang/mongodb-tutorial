Feature: 電商系統診斷與健康檢查

  Scenario: 伺服器狀態包含版本資訊
    When 查詢 MongoDB 伺服器狀態
    Then 伺服器版本應以 "8.0" 開頭
    And 目前連線數應大於 0

  Scenario: 資料庫統計包含集合與文件數量
    Given 已新增 3 筆商品資料
    When 查詢資料庫統計
    Then 集合數量應大於 0
    And 文件總數應大於 0

  Scenario: 集合統計匹配已插入文件數
    Given 已清除商品集合
    And 已新增 5 筆商品資料
    When 查詢商品集合統計
    Then 文件數量應為 5

  Scenario: 健康檢查回報 UP 狀態
    When 執行 MongoDB 健康檢查
    Then 健康狀態應為 "UP"
    And 健康詳細資訊應包含 "databaseName"

  Scenario: 健康檢查包含伺服器版本
    When 執行 MongoDB 健康檢查
    Then 健康詳細資訊應包含 "version"
    And 版本資訊應以 "8.0" 開頭
