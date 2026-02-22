package com.mongodb.course.m01.rdb;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    List<CustomerEntity> findByName(String name);

    @Query("SELECT DISTINCT c FROM CustomerEntity c JOIN FETCH c.orders o JOIN FETCH o.items WHERE c.name = :name")
    List<CustomerEntity> findByNameWithOrdersAndItems(String name);
}
