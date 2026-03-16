package com.example.orders.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class OrderCreatedAuditObserver {

    public void onOrderCreated(@Observes OrderCreatedEvent event) {
    }
}
