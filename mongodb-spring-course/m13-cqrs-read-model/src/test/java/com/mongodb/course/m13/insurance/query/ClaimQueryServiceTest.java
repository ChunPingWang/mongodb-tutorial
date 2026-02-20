package com.mongodb.course.m13.insurance.query;

import com.mongodb.course.m13.SharedContainersConfig;
import com.mongodb.course.m13.insurance.command.ClaimCommandService;
import com.mongodb.course.m13.insurance.readmodel.ClaimStatisticsDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class ClaimQueryServiceTest {

    @Autowired
    private ClaimQueryService queryService;

    @Autowired
    private ClaimCommandService commandService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanup() {
        mongoTemplate.remove(new Query(), "m13_claim_events");
        mongoTemplate.remove(new Query(), "m13_claim_dashboards");
        mongoTemplate.remove(new Query(), "m13_claim_statistics");
    }

    @Test
    void getClaimsByStatus_filtersCorrectly() {
        commandService.fileClaim("CQ-01", "POL-01", "Alice", new BigDecimal("100000"), "Medical");
        commandService.fileClaim("CQ-02", "POL-02", "Bob", new BigDecimal("200000"), "Accident");
        commandService.investigate("CQ-02", "Inspector", "Valid");

        var filed = queryService.getClaimsByStatus("FILED");
        var investigating = queryService.getClaimsByStatus("UNDER_INVESTIGATION");

        assertThat(filed).hasSize(1);
        assertThat(filed.getFirst().claimId()).isEqualTo("CQ-01");
        assertThat(investigating).hasSize(1);
        assertThat(investigating.getFirst().claimId()).isEqualTo("CQ-02");
    }

    @Test
    void getAllStatistics_returnsPerCategoryAggregations() {
        commandService.fileClaim("CQ-03", "POL-03", "Carol", new BigDecimal("100000"), "Medical");
        commandService.fileClaim("CQ-04", "POL-04", "Dave", new BigDecimal("200000"), "Medical");
        commandService.fileClaim("CQ-05", "POL-05", "Eve", new BigDecimal("300000"), "Accident");

        var allStats = queryService.getAllStatistics();

        assertThat(allStats).hasSize(2);
        var medical = allStats.stream()
                .filter(s -> "Medical".equals(s.category()))
                .findFirst().orElseThrow();
        assertThat(medical.totalClaims()).isEqualTo(2);
        assertThat(medical.totalClaimedAmount()).isEqualByComparingTo(new BigDecimal("300000"));
    }
}
