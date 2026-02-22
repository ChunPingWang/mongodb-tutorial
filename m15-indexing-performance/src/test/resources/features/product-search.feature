Feature: 電商產品搜尋索引優化

  Background:
    Given 系統已產生 500 筆產品資料涵蓋 5 個分類
    And 已建立文字索引於 name 與 description 欄位
    And 已建立部分索引僅索引有庫存產品
    And 已建立複合索引 category_price

  Scenario: 全文檢索使用文字索引
    When 以關鍵字 "wireless" 搜尋產品
    Then 應搜尋到至少 1 個產品
    And 查詢使用文字索引

  Scenario: 分類加價格範圍使用複合索引
    When 查詢分類 "Electronics" 價格在 1000 到 5000 之間的產品
    Then 查詢計畫應使用索引掃描
    And 回傳產品皆屬於 "Electronics" 分類

  Scenario: 部分索引僅掃描有庫存產品
    When 查詢有庫存的 "Books" 分類產品
    Then 查詢計畫應使用部分索引
    And 回傳產品皆為有庫存狀態

  Scenario: 多標籤篩選使用多鍵索引
    Given 已建立標籤多鍵索引
    When 以標籤 "portable" 搜尋產品
    Then 查詢計畫應使用標籤索引

  Scenario: 有庫存產品依價格排序
    When 查詢有庫存產品並依價格升冪排序取前 10 筆
    Then 應回傳 10 筆產品
    And 產品價格應為升冪排列
    And 所有產品皆為有庫存
