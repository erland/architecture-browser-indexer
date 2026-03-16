package com.example.orders.service;

import com.example.orders.domain.OrderEntity;
import com.example.orders.dto.OrderRequest;
import com.example.orders.events.OrderCreatedEvent;
import com.example.orders.repo.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    Event<OrderCreatedEvent> orderCreatedEvents;

    public List<OrderEntity> listOrders() {
        return orderRepository.findAll();
    }

    public OrderEntity createOrder(OrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.setExternalId(request.getExternalId());
        orderCreatedEvents.fire(new OrderCreatedEvent(request.getExternalId()));
        orderRepository.save(entity);
        return entity;
    }
}
