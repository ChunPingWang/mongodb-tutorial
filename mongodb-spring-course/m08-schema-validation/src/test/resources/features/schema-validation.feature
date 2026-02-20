Feature: Schema Validation
  MongoDB $jsonSchema 資料庫層驗證與 Jakarta Bean Validation 應用層驗證

  Scenario: 建立帶有 Schema 驗證的集合並拒絕無效文件
    Given 建立 "m08_bdd_bank_accounts" 集合帶有嚴格的銀行帳戶 Schema 驗證
    When 嘗試插入缺少 "holderName" 的銀行帳戶文件
    Then 應該拋出 MongoDB 寫入錯誤

  Scenario: Bean Validation 攔截無效保險保單
    Given 系統已啟用 Bean Validation
    When 嘗試儲存保費為 "-500" 的保險保單
    Then 應該拋出 ConstraintViolationException
    When 嘗試儲存持有人姓名為空白的保險保單
    Then 應該拋出 ConstraintViolationException
