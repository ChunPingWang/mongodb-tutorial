Feature: 保險核保交易
  驗證多文件交易在保險核保場景中的原子性行為

  Scenario: 核保通過建立保單與收費排程
    Given 保險客戶 "C001" 名稱 "Alice" 狀態為 "PROSPECT"
    When 進行核保建立保單類型 "LIFE" 保費 1200 元
    Then 保單成功建立狀態為 "ACTIVE"
    And 收費排程成功建立每月 1200 元
    And 客戶 "C001" 狀態更新為 "ACTIVE"

  Scenario: 已停權客戶核保失敗全部回滾
    Given 保險客戶 "C002" 名稱 "Bob" 狀態為 "SUSPENDED"
    When 進行核保建立保單類型 "AUTO" 保費 800 元
    Then 核保失敗並回傳客戶停權錯誤
    And 沒有新保單被建立
    And 沒有新收費排程被建立
