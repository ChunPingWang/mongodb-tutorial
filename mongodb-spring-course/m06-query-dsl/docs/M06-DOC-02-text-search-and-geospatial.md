# M06-DOC-02: 全文檢索與地理空間查詢

## 前言

除了基本的 Criteria 查詢，MongoDB 還提供兩大進階查詢功能：**全文檢索（Text Search）** 和 **地理空間查詢（Geospatial Queries）**。本文件透過電商產品搜尋和台北商店位置查詢兩個實務場景，示範如何在 Spring Data MongoDB 中使用這些功能。

## Part 1: 全文檢索

### 1.1 Text Index 基礎

MongoDB 的全文檢索需要先建立 **Text Index**。每個 collection 只能有一個 Text Index，但可以涵蓋多個欄位。

```java
// 程式化建立 Text Index
TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
    .onField("name", 3F)        // weight = 3（名稱匹配權重較高）
    .onField("description")     // weight = 1（預設）
    .build();
mongoTemplate.indexOps(Product.class).ensureIndex(textIndex);
```

也可以在實體類別上使用註解：
```java
@TextIndexed(weight = 3)
private String name;

@TextIndexed
private String description;
```

> **注意**：每個 collection 只能有一個 Text Index。如果需要搜尋多個欄位，把它們全部加入同一個 Text Index。

### 1.2 TextCriteria 基本搜尋

```java
// 搜尋包含 "wireless" 的產品
TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny("wireless");
Query query = TextQuery.queryText(textCriteria);
List<Product> results = mongoTemplate.find(query, Product.class);
```

### 1.3 搜尋模式

| 方法 | 用途 | 範例 |
|------|------|------|
| `matchingAny(terms)` | 匹配任一關鍵字 | `matchingAny("wireless", "bluetooth")` |
| `matchingPhrase(phrase)` | 完整片語匹配 | `matchingPhrase("noise cancelling")` |
| `matching(term)` | 匹配單一關鍵字 | `matching("keyboard")` |
| `notMatching(term)` | 排除關鍵字 | `notMatching("mouse")` |

### 1.4 組合搜尋

可以鏈式組合多種搜尋模式：
```java
// 搜尋 "wireless" 但排除 "mouse"
TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
    .matching("wireless")
    .notMatching("mouse");
```

### 1.5 依相關性排序

```java
TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny("MongoDB");
Query query = TextQuery.queryText(textCriteria).sortByScore();
```

MongoDB 會根據 **text score** 計算每筆結果的相關性。欄位的 weight 越高，該欄位的匹配得分越高。例如 `name` 欄位 weight = 3，同樣的關鍵字出現在名稱中，得分是出現在描述中的 3 倍。

### 1.6 TextCriteria + 一般 Criteria 組合

全文檢索可以與一般 Criteria 組合使用：

```java
// 在 "Books" 分類中搜尋 "guide"
TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching("guide");
Query query = TextQuery.queryText(textCriteria)
    .addCriteria(Criteria.where("category").is("Books"));
```

## Part 2: 地理空間查詢

### 2.1 GeoJSON 與 2dsphere Index

MongoDB 支援 GeoJSON 格式的地理資料。Spring Data MongoDB 提供了 `GeoJsonPoint` 類別：

```java
@Document("m06_stores")
public class Store {
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
    // ...
}
```

程式化建立索引：
```java
mongoTemplate.indexOps(Store.class).ensureIndex(
    new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
```

> **重要**：GeoJSON 使用 `[longitude, latitude]` 順序（經度在前、緯度在後），與 Google Maps 的 `(lat, lng)` 順序相反。

### 2.2 NearQuery — 附近查詢

```java
// 查詢台北 101 附近 3 公里內的商店
NearQuery nearQuery = NearQuery.near(new Point(121.5654, 25.0330))  // lng, lat
    .maxDistance(new Distance(3, Metrics.KILOMETERS))
    .spherical(true);

GeoResults<Store> results = mongoTemplate.geoNear(nearQuery, Store.class);
```

`GeoResults` 包含每筆結果及其與查詢點的距離：
```java
for (GeoResult<Store> result : results) {
    Store store = result.getContent();
    Distance distance = result.getDistance();  // 包含距離值和單位
    System.out.printf("%s: %.2f km%n", store.getName(), distance.getValue());
}
```

結果會自動依距離由近到遠排序。

### 2.3 withinSphere — 圓形範圍查詢

```java
// 圓形範圍：以座標為圓心，半徑 6km
Query query = new Query(
    Criteria.where("location").withinSphere(
        new Circle(new Point(121.5654, 25.0330),
                   new Distance(6, Metrics.KILOMETERS))
    )
);
List<Store> stores = mongoTemplate.find(query, Store.class);
```

> **NearQuery vs withinSphere 的差異**：
> - `NearQuery` + `geoNear()` → 回傳 `GeoResults`（包含距離資訊），自動排序
> - `withinSphere` + `find()` → 回傳 `List`（不含距離），不排序

### 2.4 within(Box) — 矩形範圍查詢

```java
// 矩形範圍：指定左下角和右上角
Point lowerLeft = new Point(121.53, 25.02);   // 西南角
Point upperRight = new Point(121.58, 25.04);  // 東北角
Query query = new Query(
    Criteria.where("location").within(new Box(lowerLeft, upperRight))
);
```

### 2.5 組合地理查詢與一般條件

NearQuery 支援在地理查詢中加入額外條件：

```java
// 附近 6km 且營業中的 cafe
NearQuery nearQuery = NearQuery.near(new Point(121.5654, 25.0330))
    .maxDistance(new Distance(6, Metrics.KILOMETERS))
    .spherical(true)
    .query(new Query(Criteria.where("category").is("cafe")
                              .and("open").is(true)));

GeoResults<Store> results = mongoTemplate.geoNear(nearQuery, Store.class);
```

### 2.6 台北範例座標參考

本模組使用以下台北地標座標作為測試資料：

| 地點 | 經度 (lng) | 緯度 (lat) | 與 101 距離 |
|------|-----------|-----------|------------|
| 台北 101 | 121.5654 | 25.0330 | — |
| 信義區 Cafe | 121.5675 | 25.0365 | ~0.5 km |
| 大安區書店 | 121.5434 | 25.0260 | ~2.5 km |
| 中山區餐廳 | 121.5225 | 25.0530 | ~5 km |
| 板橋商場 | 121.4722 | 25.0145 | ~11 km |

## 3. 陣列與 Map 查詢

### 3.1 陣列查詢

```java
// $all：必須包含所有指定標籤
Criteria.where("tags").all(List.of("wireless", "bluetooth"))

// $size：陣列長度等於指定值
Criteria.where("tags").size(3)
```

### 3.2 Map（嵌套文件）查詢

使用 dot-notation 查詢 Map 中的特定 key-value：

```java
// specifications.color = "black"
Criteria.where("specifications.color").is("black")
```

## 小結

| 查詢類型 | Spring Data API | 回傳型別 |
|---------|----------------|---------|
| 全文檢索 | `TextCriteria` + `TextQuery` | `List<T>` |
| 附近查詢 | `NearQuery` + `geoNear()` | `GeoResults<T>` |
| 圓形範圍 | `withinSphere(Circle)` + `find()` | `List<T>` |
| 矩形範圍 | `within(Box)` + `find()` | `List<T>` |
| 陣列查詢 | `all()`, `size()` | `List<T>` |
| Map 查詢 | dot-notation | `List<T>` |

這些查詢都可以與一般 Criteria 條件自由組合，構成更精確的查詢。下一篇文件將介紹 Aggregation Pipeline 入門，以及 M06 四大查詢領域的回顧與決策矩陣。
