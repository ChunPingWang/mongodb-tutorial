Feature: 讀取時期惰性遷移

  Scenario: 讀取 V1 客戶自動轉換為嵌入式地址
    Given 資料庫中存在一筆 V1 客戶 "Alice" 地址為 "台北市信義區"
    When 透過 CustomerService 讀取 "Alice" 的資料
    Then 回傳的客戶地址應為嵌入式 Address 物件
    And 地址街道應為 "台北市信義區"
    And loyaltyTier 應為 "BRONZE"

  Scenario: V2 客戶讀取時自動補上 V3 新欄位
    Given 資料庫中存在一筆 V2 客戶 "Bob" 含嵌入式地址
    When 透過 CustomerService 讀取 "Bob" 的資料
    Then loyaltyTier 應為 "BRONZE"
    And registeredAt 不為空

  Scenario: 修改後儲存的客戶自動升級為 V3
    Given 資料庫中存在一筆 V1 客戶 "Charlie" 地址為 "高雄市前鎮區"
    When 讀取 "Charlie" 並儲存回資料庫
    Then 資料庫中 "Charlie" 的 schemaVersion 應為 3
    And 原始扁平地址欄位 "street" 應已移除
