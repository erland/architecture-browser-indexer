package com.example.orders.repo;

import com.example.orders.domain.OrderEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderRepository {

    public List<OrderEntity> findAll() {
        return List.of();
    }

    public void save(OrderEntity entity) {
    }
}
