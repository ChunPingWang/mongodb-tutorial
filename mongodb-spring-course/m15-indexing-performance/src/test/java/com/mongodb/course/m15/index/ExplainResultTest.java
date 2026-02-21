package com.mongodb.course.m15.index;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainResultTest {

    @Test
    void from_classicIxscanPlan_parsesCorrectly() {
        // Classic format: queryPlanner.winningPlan has FETCH -> inputStage: IXSCAN
        var ixscan = new Document("stage", "IXSCAN")
                .append("indexName", "accountId_1_type_1");
        var fetch = new Document("stage", "FETCH")
                .append("inputStage", ixscan);
        var queryPlanner = new Document("winningPlan", fetch);
        var executionStats = new Document("nReturned", 10)
                .append("totalKeysExamined", 10)
                .append("totalDocsExamined", 10);
        var explainDoc = new Document("queryPlanner", queryPlanner)
                .append("executionStats", executionStats);

        ExplainResult result = ExplainResult.from(explainDoc);

        assertThat(result.stage()).isEqualTo("IXSCAN");
        assertThat(result.indexName()).isEqualTo("accountId_1_type_1");
        assertThat(result.keysExamined()).isEqualTo(10);
        assertThat(result.docsExamined()).isEqualTo(10);
        assertThat(result.nReturned()).isEqualTo(10);
        assertThat(result.isIndexOnly()).isFalse();
    }

    @Test
    void from_sbeFormatPlan_parsesFromQueryPlan() {
        // SBE format: winningPlan.queryPlan has FETCH -> inputStage: IXSCAN
        var ixscan = new Document("stage", "IXSCAN")
                .append("indexName", "category_1_price_1");
        var fetch = new Document("stage", "FETCH")
                .append("inputStage", ixscan);
        var winningPlan = new Document("queryPlan", fetch);
        var queryPlanner = new Document("winningPlan", winningPlan);
        var executionStats = new Document("nReturned", 5)
                .append("totalKeysExamined", 5)
                .append("totalDocsExamined", 0);
        var explainDoc = new Document("queryPlanner", queryPlanner)
                .append("executionStats", executionStats);

        ExplainResult result = ExplainResult.from(explainDoc);

        assertThat(result.stage()).isEqualTo("IXSCAN");
        assertThat(result.indexName()).isEqualTo("category_1_price_1");
        assertThat(result.keysExamined()).isEqualTo(5);
        assertThat(result.docsExamined()).isZero();
        assertThat(result.nReturned()).isEqualTo(5);
        assertThat(result.isIndexOnly()).isTrue();
    }

    @Test
    void from_collscanPlan_noIndexName() {
        var collscan = new Document("stage", "COLLSCAN");
        var queryPlanner = new Document("winningPlan", collscan);
        var executionStats = new Document("nReturned", 100)
                .append("totalKeysExamined", 0)
                .append("totalDocsExamined", 100);
        var explainDoc = new Document("queryPlanner", queryPlanner)
                .append("executionStats", executionStats);

        ExplainResult result = ExplainResult.from(explainDoc);

        assertThat(result.stage()).isEqualTo("COLLSCAN");
        assertThat(result.indexName()).isNull();
        assertThat(result.keysExamined()).isZero();
        assertThat(result.docsExamined()).isEqualTo(100);
        assertThat(result.nReturned()).isEqualTo(100);
        assertThat(result.isIndexOnly()).isFalse();
    }
}
