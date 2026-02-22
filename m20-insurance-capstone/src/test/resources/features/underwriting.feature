Feature: 保單核保申請

  Scenario: 符合條件的核保申請通過
    When 申請人 "王小明" 年齡 35 職業 "工程師" 申請 "AUTO" 保單保額 500000 元
    And 執行核保審核
    Then 核保申請狀態為 "APPROVED"
    And 保單集合中存在保單類型 "AUTO" 持有人 "王小明"

  Scenario: 年齡不符被拒絕
    When 申請人 "李伯伯" 年齡 70 職業 "退休" 申請 "LIFE" 保單保額 1000000 元
    And 執行核保審核
    Then 核保申請狀態為 "REJECTED"

  Scenario: 理賠紀錄過多被拒絕
    Given 類別 "HEALTH" 已有 4 件已付款理賠
    When 申請人 "張大為" 年齡 40 職業 "業務" 申請 "HEALTH" 保單保額 300000 元
    And 執行核保審核
    Then 核保申請狀態為 "REJECTED"
