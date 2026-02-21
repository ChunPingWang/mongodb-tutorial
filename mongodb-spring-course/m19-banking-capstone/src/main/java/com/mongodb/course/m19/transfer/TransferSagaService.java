package com.mongodb.course.m19.transfer;

import com.mongodb.course.m19.infrastructure.saga.SagaContext;
import com.mongodb.course.m19.infrastructure.saga.SagaOrchestrator;
import com.mongodb.course.m19.infrastructure.saga.SagaStep;
import com.mongodb.course.m19.transfer.step.CreditTargetAccountStep;
import com.mongodb.course.m19.transfer.step.DebitSourceAccountStep;
import com.mongodb.course.m19.transfer.step.RecordTransferStep;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class TransferSagaService {

    private final SagaOrchestrator orchestrator;
    private final DebitSourceAccountStep debitStep;
    private final CreditTargetAccountStep creditStep;
    private final RecordTransferStep recordStep;

    public TransferSagaService(SagaOrchestrator orchestrator,
                               DebitSourceAccountStep debitStep,
                               CreditTargetAccountStep creditStep,
                               RecordTransferStep recordStep) {
        this.orchestrator = orchestrator;
        this.debitStep = debitStep;
        this.creditStep = creditStep;
        this.recordStep = recordStep;
    }

    public String transfer(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        var context = new SagaContext(Map.of(
                "sourceAccountId", sourceAccountId,
                "targetAccountId", targetAccountId,
                "amount", amount.toPlainString()
        ));

        List<SagaStep> steps = List.of(debitStep, creditStep, recordStep);
        return orchestrator.execute("FUND_TRANSFER", steps, context);
    }
}
