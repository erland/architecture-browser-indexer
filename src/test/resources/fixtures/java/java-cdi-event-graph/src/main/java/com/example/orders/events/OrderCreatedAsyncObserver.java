package com.example.orders.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

@ApplicationScoped
public class OrderCreatedAsyncObserver {

    public void onOrderCreatedAsync(@ObservesAsync OrderCreatedEvent event) {
    }
}
