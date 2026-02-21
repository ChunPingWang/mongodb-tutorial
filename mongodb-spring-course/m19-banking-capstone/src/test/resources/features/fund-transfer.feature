Feature: 跨帳戶資金轉帳

  Scenario: 成功轉帳並記錄完整軌跡
    Given 已開立帳戶 "TRF-001" 初始存款 50000 元
    And 已開立帳戶 "TRF-002" 初始存款 10000 元
    When 從帳戶 "TRF-001" 轉帳 20000 元到帳戶 "TRF-002"
    Then 帳戶 "TRF-001" 餘額為 30000 元
    And 帳戶 "TRF-002" 餘額為 30000 元
    And 轉帳 Saga 狀態為 "COMPLETED"

  Scenario: 餘額不足轉帳失敗觸發補償
    Given 已開立帳戶 "TRF-003" 初始存款 5000 元
    And 已開立帳戶 "TRF-004" 初始存款 10000 元
    When 從帳戶 "TRF-003" 轉帳 20000 元到帳戶 "TRF-004"
    Then 轉帳 Saga 狀態為 "COMPENSATED"
    And 帳戶 "TRF-003" 餘額為 5000 元

  Scenario: Saga 日誌記錄完整步驟
    Given 已開立帳戶 "TRF-005" 初始存款 100000 元
    And 已開立帳戶 "TRF-006" 初始存款 10000 元
    When 從帳戶 "TRF-005" 轉帳 5000 元到帳戶 "TRF-006"
    Then 轉帳 Saga 日誌包含 3 個步驟
