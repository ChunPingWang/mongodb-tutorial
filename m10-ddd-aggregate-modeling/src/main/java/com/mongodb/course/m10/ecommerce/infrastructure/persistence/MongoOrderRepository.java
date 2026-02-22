package com.mongodb.course.m10.ecommerce.infrastructure.persistence;

import com.mongodb.course.m10.ecommerce.domain.model.Order;
import com.mongodb.course.m10.ecommerce.domain.port.OrderRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private final MongoTemplate mongoTemplate;
    private final OrderMapper mapper;

    public MongoOrderRepository(MongoTemplate mongoTemplate, OrderMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderMongoDocument doc = mapper.toDocument(order);
        OrderMongoDocument saved = mongoTemplate.save(doc);
        order.setId(saved.getId());
        order.clearDomainEvents();
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        OrderMongoDocument doc = mongoTemplate.findById(id, OrderMongoDocument.class);
        return Optional.ofNullable(doc).map(mapper::toDomain);
    }
}
