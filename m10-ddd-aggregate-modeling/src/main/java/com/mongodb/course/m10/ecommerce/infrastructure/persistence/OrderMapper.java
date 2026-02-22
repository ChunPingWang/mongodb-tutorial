package com.mongodb.course.m10.ecommerce.infrastructure.persistence;

import com.mongodb.course.m10.banking.domain.model.Money;
import com.mongodb.course.m10.ecommerce.domain.model.*;
import com.mongodb.course.m10.shared.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderMapper {

    public OrderMongoDocument toDocument(Order domain) {
        var doc = new OrderMongoDocument();
        doc.setId(domain.getId());
        doc.setOrderNumber(domain.getOrderNumber());
        doc.setCustomerId(domain.getCustomerId());
        doc.setLines(domain.getLines().stream().map(this::toLineDoc).toList());
        doc.setShippingAddress(toAddressDoc(domain.getShippingAddress()));
        doc.setPaymentInfo(domain.getPaymentInfo() != null ? toPaymentDoc(domain.getPaymentInfo()) : null);
        doc.setTrackingNumber(domain.getTrackingNumber());
        doc.setStatus(domain.getStatus().name());
        doc.setTotalAmount(domain.getTotalAmount().amount());
        doc.setTotalAmountCurrency(domain.getTotalAmount().currency());
        doc.setCreatedAt(domain.getCreatedAt());
        doc.setUpdatedAt(domain.getUpdatedAt());
        doc.setDomainEvents(serializeEvents(domain.getDomainEvents()));
        return doc;
    }

    public Order toDomain(OrderMongoDocument doc) {
        var lines = doc.getLines().stream()
                .map(l -> new OrderLine(l.getProductId(), l.getProductName(),
                        l.getQuantity(), new Money(l.getUnitPrice(), l.getUnitPriceCurrency())))
                .toList();

        var address = new ShippingAddress(
                doc.getShippingAddress().getRecipientName(),
                doc.getShippingAddress().getStreet(),
                doc.getShippingAddress().getCity(),
                doc.getShippingAddress().getZipCode());

        PaymentInfo paymentInfo = null;
        if (doc.getPaymentInfo() != null) {
            var p = doc.getPaymentInfo();
            paymentInfo = new PaymentInfo(p.getPaymentMethod(), p.getTransactionId(), p.getPaidAt());
        }

        return Order.reconstitute(
                doc.getId(), doc.getOrderNumber(), doc.getCustomerId(),
                lines, address, paymentInfo, doc.getTrackingNumber(),
                OrderStatus.valueOf(doc.getStatus()),
                new Money(doc.getTotalAmount(), doc.getTotalAmountCurrency()),
                doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }

    private OrderMongoDocument.LineDoc toLineDoc(OrderLine line) {
        var doc = new OrderMongoDocument.LineDoc();
        doc.setProductId(line.productId());
        doc.setProductName(line.productName());
        doc.setQuantity(line.quantity());
        doc.setUnitPrice(line.unitPrice().amount());
        doc.setUnitPriceCurrency(line.unitPrice().currency());
        return doc;
    }

    private OrderMongoDocument.AddressDoc toAddressDoc(ShippingAddress addr) {
        var doc = new OrderMongoDocument.AddressDoc();
        doc.setRecipientName(addr.recipientName());
        doc.setStreet(addr.street());
        doc.setCity(addr.city());
        doc.setZipCode(addr.zipCode());
        return doc;
    }

    private OrderMongoDocument.PaymentDoc toPaymentDoc(PaymentInfo info) {
        var doc = new OrderMongoDocument.PaymentDoc();
        doc.setPaymentMethod(info.paymentMethod());
        doc.setTransactionId(info.transactionId());
        doc.setPaidAt(info.paidAt());
        return doc;
    }

    private List<Map<String, Object>> serializeEvents(List<DomainEvent> events) {
        return events.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("type", e.getClass().getSimpleName());
            map.put("aggregateId", e.aggregateId());
            map.put("occurredAt", e.occurredAt().toString());
            return map;
        }).toList();
    }
}
