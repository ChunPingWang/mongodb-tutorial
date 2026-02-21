Feature: 貸款申請審核

  Scenario: 符合條件的貸款申請通過審核
    Given 已開立帳戶 "LOAN-001" 初始存款 200000 元
    When 申請人 "王小明" 年收入 1800000 以帳戶 "LOAN-001" 申請貸款 1000000 元期限 240 個月
    And 執行貸款審核
    Then 貸款申請狀態為 "APPROVED"

  Scenario: 帳戶餘額不足被拒絕
    Given 已開立帳戶 "LOAN-002" 初始存款 10000 元
    When 申請人 "李小華" 年收入 2000000 以帳戶 "LOAN-002" 申請貸款 500000 元期限 120 個月
    And 執行貸款審核
    Then 貸款申請狀態為 "REJECTED"

  Scenario: 收入不足被拒絕
    Given 已開立帳戶 "LOAN-003" 初始存款 500000 元
    When 申請人 "張大為" 年收入 300000 以帳戶 "LOAN-003" 申請貸款 5000000 元期限 360 個月
    And 執行貸款審核
    Then 貸款申請狀態為 "REJECTED"
