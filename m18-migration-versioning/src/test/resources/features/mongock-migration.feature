Feature: Mongock 批次遷移

  Scenario: Mongock 執行 V1 到 V2 保單遷移
    Given 已插入 1000 筆 V1 保單文件
    When 執行 V002 遷移加入 riskScore 欄位
    Then 所有保單的 schemaVersion 應為 2
    And 所有保單應包含 riskScore 欄位

  Scenario: riskScore 根據保單類型設定預設值
    Given 已插入 1000 筆 V1 保單文件
    And 已執行 V002 遷移
    Then AUTO 類型保單的 riskScore 應為 50
    And HOME 類型保單的 riskScore 應為 30
    And LIFE 類型保單的 riskScore 應為 20

  Scenario: V002 遷移的回滾移除新欄位
    Given 已插入 1000 筆 V1 保單文件
    And 已執行 V002 遷移
    When 執行 V002 的 rollback
    Then 所有保單的 schemaVersion 應為 1
    And 保單不應包含 riskScore 欄位

  Scenario: Mongock 變更紀錄提供稽核軌跡
    Given 已插入 100 筆 V1 保單文件
    When 執行 V002 遷移加入 riskScore 欄位
    Then 應可查詢到 V002 的遷移執行紀錄
