package com.mongodb.course.m10.ecommerce.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("m10_orders")
public class OrderMongoDocument {

    @Id
    private String id;
    private String orderNumber;
    private String customerId;
    private List<LineDoc> lines;
    private AddressDoc shippingAddress;
    private PaymentDoc paymentInfo;
    private String trackingNumber;
    private String status;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;
    private String totalAmountCurrency;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Map<String, Object>> domainEvents;

    public OrderMongoDocument() {
    }

    // ── Embedded document types ─────────────────────────────────────────
    public static class LineDoc {
        private String productId;
        private String productName;
        private int quantity;
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal unitPrice;
        private String unitPriceCurrency;

        public LineDoc() {}

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public String getUnitPriceCurrency() { return unitPriceCurrency; }
        public void setUnitPriceCurrency(String c) { this.unitPriceCurrency = c; }
    }

    public static class AddressDoc {
        private String recipientName;
        private String street;
        private String city;
        private String zipCode;

        public AddressDoc() {}

        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    }

    public static class PaymentDoc {
        private String paymentMethod;
        private String transactionId;
        private Instant paidAt;

        public PaymentDoc() {}

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public Instant getPaidAt() { return paidAt; }
        public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    }

    // ── Accessors ───────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<LineDoc> getLines() { return lines; }
    public void setLines(List<LineDoc> lines) { this.lines = lines; }
    public AddressDoc getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDoc shippingAddress) { this.shippingAddress = shippingAddress; }
    public PaymentDoc getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(PaymentDoc paymentInfo) { this.paymentInfo = paymentInfo; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getTotalAmountCurrency() { return totalAmountCurrency; }
    public void setTotalAmountCurrency(String c) { this.totalAmountCurrency = c; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<Map<String, Object>> getDomainEvents() { return domainEvents; }
    public void setDomainEvents(List<Map<String, Object>> domainEvents) { this.domainEvents = domainEvents; }
}
