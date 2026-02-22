Feature: 銀行營運儀表板

  Scenario: 依餘額排名查詢帳戶
    Given 已開立帳戶 "DASH-001" 初始存款 50000 元
    And 已開立帳戶 "DASH-002" 初始存款 80000 元
    And 已開立帳戶 "DASH-003" 初始存款 30000 元
    When 查詢餘額前 2 名帳戶
    Then 排行結果依序為 "DASH-002" 和 "DASH-001"

  Scenario: 統計各類型交易筆數
    Given 已開立帳戶 "DASH-004" 初始存款 100000 元
    And 存入 10000 元到帳戶 "DASH-004" 備註 "獎金"
    And 從帳戶 "DASH-004" 提款 5000 元
    When 查詢帳戶 "DASH-004" 各類型交易統計
    Then 存款筆數為 1
    And 提款筆數為 1

  Scenario: 慢查詢偵測器捕獲營運查詢
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 已開立帳戶 "DASH-006" 初始存款 10000 元
    When 查詢餘額前 10 名帳戶
    Then 慢查詢偵測器應捕獲至少 1 筆紀錄
