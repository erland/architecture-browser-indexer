package com.example.orders.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLineEntity extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;
}
