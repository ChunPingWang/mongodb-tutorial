package com.mongodb.course.m14.ecommerce.service;

import com.mongodb.course.m14.ecommerce.step.ConfirmOrderStep;
import com.mongodb.course.m14.ecommerce.step.PlaceOrderStep;
import com.mongodb.course.m14.ecommerce.step.ProcessPaymentStep;
import com.mongodb.course.m14.ecommerce.step.ReserveInventoryStep;
import com.mongodb.course.m14.saga.SagaContext;
import com.mongodb.course.m14.saga.SagaOrchestrator;
import com.mongodb.course.m14.saga.SagaStep;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderSagaService {

    private final SagaOrchestrator orchestrator;
    private final MongoTemplate mongoTemplate;

    public OrderSagaService(SagaOrchestrator orchestrator, MongoTemplate mongoTemplate) {
        this.orchestrator = orchestrator;
        this.mongoTemplate = mongoTemplate;
    }

    public String placeOrder(String customerId, String productId, String productName, int quantity, long unitPrice) {
        List<SagaStep> steps = List.of(
                new PlaceOrderStep(mongoTemplate),
                new ReserveInventoryStep(mongoTemplate),
                new ProcessPaymentStep(mongoTemplate),
                new ConfirmOrderStep(mongoTemplate)
        );

        var context = new SagaContext();
        context.put("customerId", customerId);
        context.put("productId", productId);
        context.put("productName", productName);
        context.put("quantity", quantity);
        context.put("unitPrice", unitPrice);

        return orchestrator.execute("ORDER_SAGA", steps, context);
    }
}
