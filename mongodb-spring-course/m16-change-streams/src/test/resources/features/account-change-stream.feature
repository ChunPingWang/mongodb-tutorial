Feature: 銀行帳戶變更串流通知

  Scenario: 新增帳戶觸發變更通知
    Given 帳戶變更串流已啟動
    When 新增帳戶持有人 "Alice" 餘額 10000 元
    Then 應收到 1 筆變更通知
    And 最新通知操作類型為 "insert"
    And 最新通知帳戶持有人為 "Alice"

  Scenario: 更新帳戶餘額捕獲完整文件
    Given 已存在帳戶持有人 "Bob" 餘額 5000 元
    And 帳戶變更串流已啟動
    When 更新該帳戶餘額為 8000 元
    Then 應收到 1 筆變更通知
    And 最新通知操作類型為 "update"
    And 最新通知餘額為 8000 元

  Scenario: 刪除帳戶觸發刪除通知
    Given 已存在帳戶持有人 "Charlie" 餘額 3000 元
    And 帳戶變更串流已啟動
    When 刪除該帳戶
    Then 應收到 1 筆變更通知
    And 最新通知操作類型為 "delete"

  Scenario: 篩選僅接收新增事件
    Given 已存在帳戶持有人 "Diana" 餘額 7000 元
    And 帳戶變更串流已啟動僅監聽新增事件
    When 更新該帳戶餘額為 9000 元
    And 新增帳戶持有人 "Eve" 餘額 2000 元
    Then 應收到 1 筆變更通知
    And 最新通知帳戶持有人為 "Eve"

  Scenario: 停止監聽後不再接收通知
    Given 帳戶變更串流已啟動
    When 新增帳戶持有人 "Frank" 餘額 4000 元
    Then 應收到 1 筆變更通知
    When 帳戶變更串流已停止
    And 新增帳戶持有人 "Grace" 餘額 5000 元
    And 等待 500 毫秒
    Then 仍然只有 1 筆變更通知
