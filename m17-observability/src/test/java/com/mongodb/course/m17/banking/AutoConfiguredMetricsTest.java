package com.mongodb.course.m17.banking;

import com.mongodb.course.m17.SharedContainersConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SharedContainersConfig.class)
class AutoConfiguredMetricsTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void commandMetrics_timerExists() {
        transactionService.create("ACC-METRIC-001", 1000, "DEPOSIT");
        transactionService.findAll();

        var timers = meterRegistry.find("mongodb.driver.commands").timers();
        assertThat(timers).isNotEmpty();
    }

    @Test
    void commandMetrics_hasCommandTag() {
        transactionService.findAll();

        var timer = meterRegistry.find("mongodb.driver.commands")
                .tag("command", "find")
                .timer();
        assertThat(timer).isNotNull();
    }

    @Test
    void connectionPoolMetrics_gaugeExists() {
        var gauge = meterRegistry.find("mongodb.driver.pool.size").gauge();
        assertThat(gauge).isNotNull();
    }
}
