Feature: 銀行交易查詢索引優化

  Background:
    Given 系統已產生 2000 筆交易資料涵蓋 10 個帳戶
    And 已建立複合索引 accountId_type_transactionDate

  Scenario: 帳戶交易查詢使用索引掃描
    When 查詢帳戶 "ACC-000001" 的所有交易
    Then 查詢計畫應使用 "IXSCAN"
    And 檢查的索引鍵數應接近回傳文件數

  Scenario: ESR 完整條件查詢效率最高
    When 以帳戶 "ACC-000001" 類型 "DEPOSIT" 日期範圍查詢
    Then 查詢計畫應使用 "IXSCAN"
    And 檢查的文件數應等於回傳文件數

  Scenario: 覆蓋查詢避免回表
    Given 已建立覆蓋查詢索引 accountId_amount
    When 僅投影帳戶與金額欄位查詢帳戶 "ACC-000001"
    Then 查詢計畫應為覆蓋查詢
    And 檢查的文件數應為 0

  Scenario: TTL 索引自動清除過期資料
    Given 已建立 TTL 索引過期秒數為 2
    And 系統已產生 5 筆暫存交易資料
    When 等待 5 秒讓 TTL 監控器執行
    Then 暫存交易資料應已被自動清除

  Scenario: 無索引欄位查詢使用全集合掃描
    When 以 description 欄位查詢交易
    Then 查詢計畫應使用 "COLLSCAN"
