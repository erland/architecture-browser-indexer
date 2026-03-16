package com.example.orders.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderCreatedEvent> orderCreatedEvents;

    public void createOrder(String orderId) {
        orderCreatedEvents.fire(new OrderCreatedEvent(orderId));
    }
}
