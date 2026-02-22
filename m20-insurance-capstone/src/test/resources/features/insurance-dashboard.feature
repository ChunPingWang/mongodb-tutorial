Feature: 保險營運儀表板

  Scenario: 依類別查詢理賠統計
    Given 已建立汽車保單 "POL-D01" 持有人 "王小明" 保額 500000 元
    And 已提出理賠 "CLM-D01" 保單 "POL-D01" 金額 30000 元
    And 已提出理賠 "CLM-D02" 保單 "POL-D01" 金額 20000 元
    And 已建立健康保單 "POL-D02" 持有人 "李小華" 保額 300000 元
    And 已提出理賠 "CLM-D03" 保單 "POL-D02" 金額 50000 元
    When 查詢類別 "AUTO" 的理賠統計
    Then 該類別總理賠件數為 2
    And 該類別總理賠金額為 50000 元

  Scenario: 理賠儀表板顯示時間軸
    Given 已建立汽車保單 "POL-D03" 持有人 "張大為" 保額 500000 元
    And 已提出理賠 "CLM-D04" 保單 "POL-D03" 金額 40000 元
    And 理賠 "CLM-D04" 完成調查結果為 "LOW" 風險
    And 理賠 "CLM-D04" 評估金額為 35000 元
    When 查詢理賠儀表板 "CLM-D04"
    Then 時間軸包含 3 筆紀錄

  Scenario: 慢查詢偵測器捕獲查詢
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 已建立汽車保單 "POL-D04" 持有人 "陳美玲" 保額 500000 元
    And 已提出理賠 "CLM-D05" 保單 "POL-D04" 金額 10000 元
    When 查詢類別 "AUTO" 的理賠統計
    Then 慢查詢偵測器應捕獲至少 1 筆紀錄
