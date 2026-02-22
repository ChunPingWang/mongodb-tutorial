package com.mongodb.course.m21.fulfillment;

import com.mongodb.course.m21.fulfillment.step.ConfirmOrderStep;
import com.mongodb.course.m21.fulfillment.step.ProcessPaymentStep;
import com.mongodb.course.m21.fulfillment.step.ReserveInventoryStep;
import com.mongodb.course.m21.fulfillment.step.ValidateStockStep;
import com.mongodb.course.m21.infrastructure.saga.SagaContext;
import com.mongodb.course.m21.infrastructure.saga.SagaOrchestrator;
import com.mongodb.course.m21.infrastructure.saga.SagaStep;
import com.mongodb.course.m21.order.model.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderFulfillmentSagaService {

    private final SagaOrchestrator sagaOrchestrator;
    private final ValidateStockStep validateStockStep;
    private final ReserveInventoryStep reserveInventoryStep;
    private final ProcessPaymentStep processPaymentStep;
    private final ConfirmOrderStep confirmOrderStep;

    public OrderFulfillmentSagaService(SagaOrchestrator sagaOrchestrator,
                                        ValidateStockStep validateStockStep,
                                        ReserveInventoryStep reserveInventoryStep,
                                        ProcessPaymentStep processPaymentStep,
                                        ConfirmOrderStep confirmOrderStep) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.validateStockStep = validateStockStep;
        this.reserveInventoryStep = reserveInventoryStep;
        this.processPaymentStep = processPaymentStep;
        this.confirmOrderStep = confirmOrderStep;
    }

    public String executeFulfillment(Order order) {
        var context = new SagaContext();
        context.put("order", order);
        context.put("orderId", order.getOrderId());

        List<SagaStep> steps = List.of(
                validateStockStep,
                reserveInventoryStep,
                processPaymentStep,
                confirmOrderStep
        );

        return sagaOrchestrator.execute("ORDER_FULFILLMENT", steps, context);
    }
}
