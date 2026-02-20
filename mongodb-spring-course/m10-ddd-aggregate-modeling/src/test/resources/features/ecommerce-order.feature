Feature: 電商訂單 Aggregate

  Scenario: 建立訂單計算總金額
    When 建立訂單包含以下商品:
      | 商品名稱 | 數量 | 單價  |
      | 手機     | 1    | 30000 |
      | 保護殼   | 2    | 500   |
    Then 訂單狀態為 "CREATED"
    And 訂單總金額為 31000 元

  Scenario: 訂單完整生命週期
    Given 已建立包含商品總額 50000 元的訂單
    When 執行付款交易編號 "TXN-001"
    And 執行出貨物流編號 "SHIP-001"
    And 確認送達
    And 完成訂單
    Then 訂單狀態為 "COMPLETED"

  Scenario: 已付款訂單不可新增商品
    Given 已建立包含商品總額 50000 元的訂單
    And 訂單已付款
    When 嘗試新增商品 "耳機" 單價 3000 元
    Then 操作失敗並回傳訂單不可修改錯誤

  Scenario: 已送達訂單申請退貨
    Given 已建立包含商品總額 50000 元的訂單
    And 訂單已付款已出貨已送達
    When 申請退貨
    Then 訂單狀態為 "RETURNED"
