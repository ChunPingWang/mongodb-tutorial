package com.mongodb.course.m10.ecommerce.domain.specification;

import com.mongodb.course.m10.ecommerce.domain.model.OrderStatus;

/**
 * Specification: order can only be modified when in CREATED status.
 */
public class OrderModificationAllowedSpec {

    public boolean isSatisfiedBy(OrderStatus status) {
        return status == OrderStatus.CREATED;
    }
}
