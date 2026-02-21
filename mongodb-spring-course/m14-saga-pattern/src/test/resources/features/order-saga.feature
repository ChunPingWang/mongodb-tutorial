Feature: 電商訂單 Saga 流程

  Scenario: 訂單成功完成全部步驟
    Given 商品 "LAPTOP-001" 名稱 "筆記型電腦" 庫存為 10
    When 客戶 "C001" 下單購買 "LAPTOP-001" 數量 2 單價 20000 元
    Then Saga 狀態為 "COMPLETED"
    And 訂單狀態為 "CONFIRMED"
    And 商品 "LAPTOP-001" 庫存為 8
    And 產生一筆金額 40000 元的付款記錄

  Scenario: 付款金額超過限額觸發補償
    Given 商品 "PHONE-001" 名稱 "手機" 庫存為 5
    When 客戶 "C002" 下單購買 "PHONE-001" 數量 2 單價 60000 元
    Then Saga 狀態為 "COMPENSATED"
    And 訂單狀態為 "CANCELLED"
    And 商品 "PHONE-001" 庫存為 5

  Scenario: 庫存不足觸發補償
    Given 商品 "TABLET-001" 名稱 "平板" 庫存為 1
    When 客戶 "C003" 下單購買 "TABLET-001" 數量 5 單價 15000 元
    Then Saga 狀態為 "COMPENSATED"
    And 訂單狀態為 "CANCELLED"

  Scenario: Saga 日誌記錄完整步驟
    Given 商品 "MOUSE-001" 名稱 "滑鼠" 庫存為 100
    When 客戶 "C004" 下單購買 "MOUSE-001" 數量 1 單價 500 元
    Then Saga 日誌包含 4 個步驟
    And 每個步驟狀態為 "SUCCEEDED"

  Scenario: 補償後 Saga 日誌記錄補償步驟
    Given 商品 "MONITOR-001" 名稱 "螢幕" 庫存為 10
    When 客戶 "C005" 下單購買 "MONITOR-001" 數量 2 單價 55000 元
    Then Saga 狀態為 "COMPENSATED"
    And Saga 日誌中 "PLACE_ORDER" 步驟狀態為 "COMPENSATED"
    And Saga 日誌中 "RESERVE_INVENTORY" 步驟狀態為 "COMPENSATED"
    And Saga 日誌中 "PROCESS_PAYMENT" 步驟狀態為 "FAILED"
