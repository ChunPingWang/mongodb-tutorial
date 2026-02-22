package com.mongodb.course.m15.index;

import org.bson.Document;

public record ExplainResult(
        String stage,
        String indexName,
        long keysExamined,
        long docsExamined,
        long nReturned,
        boolean isIndexOnly
) {

    public static ExplainResult from(Document explainDoc) {
        Document executionStats = explainDoc.get("executionStats", Document.class);
        long nReturned = getlong(executionStats, "nReturned");
        long totalKeysExamined = getlong(executionStats, "totalKeysExamined");
        long totalDocsExamined = getlong(executionStats, "totalDocsExamined");

        Document queryPlanner = explainDoc.get("queryPlanner", Document.class);
        Document winningPlan = queryPlanner.get("winningPlan", Document.class);

        // SBE format wraps the plan inside "queryPlan"
        Document plan = winningPlan.get("queryPlan", Document.class);
        if (plan == null) {
            plan = winningPlan;
        }

        Document scanStage = findScanStage(plan);

        String stage = scanStage != null ? scanStage.getString("stage") : plan.getString("stage");
        String indexName = scanStage != null ? scanStage.getString("indexName") : plan.getString("indexName");

        boolean isIndexOnly = "IXSCAN".equals(stage) && totalDocsExamined == 0;

        return new ExplainResult(stage, indexName, totalKeysExamined, totalDocsExamined, nReturned, isIndexOnly);
    }

    private static Document findScanStage(Document plan) {
        if (plan == null) {
            return null;
        }
        String stage = plan.getString("stage");
        if ("IXSCAN".equals(stage) || "COLLSCAN".equals(stage)) {
            return plan;
        }
        Document inputStage = plan.get("inputStage", Document.class);
        if (inputStage != null) {
            return findScanStage(inputStage);
        }
        return null;
    }

    private static long getlong(Document doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
