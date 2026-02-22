# M06-DOC-03: Aggregation 入門與查詢模式總結

## 前言

本文件分為兩個部分：第一部分簡要介紹 MongoDB Aggregation Pipeline 的基本概念；第二部分回顧 M06 涵蓋的四大查詢領域，並提供查詢方式決策矩陣，幫助你在實務中選擇最適合的查詢方法。

> **深入的 Aggregation Pipeline 內容將在 M07 完整展開**。本模組僅提供入門級的 2 個測試案例作為預覽。

## Part 1: Aggregation Pipeline 入門

### 1.1 Pipeline 概念

Aggregation Pipeline 是 MongoDB 的資料處理管線，由多個**階段（Stage）** 組成。資料依序通過每個階段進行轉換：

```
文件 → [match] → [group] → [sort] → [project] → 結果
```

### 1.2 常用階段

| 階段 | 用途 | 對應 SQL |
|------|------|---------|
| `$match` | 過濾文件 | `WHERE` |
| `$group` | 分組聚合 | `GROUP BY` |
| `$sort` | 排序 | `ORDER BY` |
| `$project` | 投影/欄位轉換 | `SELECT` |
| `$unwind` | 展開陣列 | `LATERAL` |
| `$lookup` | 關聯查詢 | `JOIN` |

### 1.3 Spring Data MongoDB 的 Aggregation API

```java
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

// 範例：統計各帳戶類型的數量（僅 ACTIVE）
Aggregation aggregation = newAggregation(
    match(Criteria.where("status").is(AccountStatus.ACTIVE)),  // $match
    group("type").count().as("count")                          // $group
);

AggregationResults<Map> results = mongoTemplate.aggregate(
    aggregation,
    "m06_bank_accounts",   // collection 名稱
    Map.class              // 結果型別
);

List<Map> mappedResults = results.getMappedResults();
```

### 1.4 分組加總

```java
// 依保單類型加總保費
Aggregation aggregation = newAggregation(
    group("policyType").sum("premium").as("totalPremium")
);

AggregationResults<Map> results = mongoTemplate.aggregate(
    aggregation,
    "m06_insurance_policies",
    Map.class
);
```

### 1.5 Decimal128 在 Aggregation 中的行為

在 Aggregation 的 `$sum` 運算中，Decimal128 欄位的加總結果也是 Decimal128（`org.bson.types.Decimal128`）。取出結果時需要轉換：

```java
BigDecimal total = new BigDecimal(result.get("totalPremium").toString());
```

### 1.6 M07 預告

M07 將深入探討以下主題：
- 多階段 Pipeline 設計
- `$unwind` + `$group` 組合
- `$lookup` 跨 collection 關聯
- `$bucket` 分桶統計
- `$facet` 多面向聚合
- 自定義結果型別（取代 `Map.class`）
- Pipeline 效能最佳化

## Part 2: M06 查詢模式總結

### 2.1 四大查詢領域回顧

| 領域 | Domain | 核心查詢技術 | 測試數 |
|------|--------|-------------|--------|
| 基礎 Criteria | Banking | `is`, `and`, `or`, `gte`, `lte`, `regex`, Sort, Pagination | 10 |
| 組合 Criteria | Insurance | `andOperator`, `orOperator`, `in`, `nin`, 動態查詢, 日期範圍 | 10 |
| 全文檢索 + 陣列/Map | Product | `TextCriteria`, `TextQuery`, `all`, `size`, dot-notation | 8 |
| 地理空間 | Store | `NearQuery`, `withinSphere`, `within(Box)`, `GeoResults` | 6 |
| Projection + Distinct | Banking + Product | `fields().include()`, `findDistinct()`, `limit()` | 6 |
| Aggregation 入門 | Banking + Insurance | `match` + `group().count()`, `group().sum()` | 2 |

### 2.2 查詢方式決策矩陣

根據查詢需求選擇最適合的方式：

```
需求是什麼？
│
├─ 固定條件、簡單查詢
│   └─ ✅ Derived Query（M05）
│
├─ 固定條件、需要自訂 MongoDB 查詢語法
│   └─ ✅ @Query（M05）
│
├─ 動態條件組合
│   └─ ✅ Criteria API（M06）
│
├─ 全文關鍵字搜尋
│   └─ ✅ TextCriteria + TextQuery（M06）
│
├─ 地理位置附近搜尋
│   └─ ✅ NearQuery / withinSphere（M06）
│
├─ 分組統計、聚合運算
│   └─ ✅ Aggregation Pipeline（M06 入門 / M07 深入）
│
├─ 需要距離資訊的位置查詢
│   └─ ✅ geoNear → GeoResults（M06）
│
└─ 複雜的多階段資料轉換
    └─ ✅ Aggregation Pipeline（M07）
```

### 2.3 MongoTemplate vs MongoRepository

| 維度 | MongoRepository | MongoTemplate + Criteria |
|------|----------------|------------------------|
| 學習曲線 | 低（宣告式） | 中（程式化） |
| 簡單 CRUD | ⭐⭐⭐ | ⭐⭐ |
| 動態查詢 | ❌ 不支援 | ⭐⭐⭐ |
| 投影控制 | 有限 | ⭐⭐⭐ |
| 全文檢索 | 有限 | ⭐⭐⭐ |
| 地理空間 | 基本支援 | ⭐⭐⭐ |
| Aggregation | ❌ | ⭐⭐⭐ |
| 測試便利性 | 高 | 中 |

**實務建議**：在同一個專案中，可以混合使用 Repository 和 MongoTemplate：
- Repository 處理標準 CRUD 和簡單查詢
- MongoTemplate 處理進階查詢和聚合操作

### 2.4 效能考量

1. **索引**：確保查詢欄位有適當的索引（Text Index、2dsphere Index）
2. **Projection**：只回傳需要的欄位，減少網路傳輸
3. **分頁**：大量資料時使用 `skip/limit` 或 cursor-based pagination
4. **Covered Query**：查詢和投影都在索引範圍內時，MongoDB 不需讀取文件本身
5. **Aggregation 順序**：`$match` 盡量放在 Pipeline 前段，提早過濾資料

## 小結

M06 完整涵蓋了 Spring Data MongoDB 的程式化查詢能力：

- **Criteria API**：靈活的查詢條件建構，支援動態組合
- **TextCriteria**：全文檢索，支援關鍵字、片語、排除、評分排序
- **NearQuery / withinSphere**：地理空間查詢，支援距離計算
- **Aggregation**（入門）：分組統計的基本框架

這些工具搭配 M05 的 Repository 查詢，構成了完整的 MongoDB 查詢技術棧。接下來的 M07 將深入 Aggregation Pipeline，探索更複雜的資料處理與分析場景。
