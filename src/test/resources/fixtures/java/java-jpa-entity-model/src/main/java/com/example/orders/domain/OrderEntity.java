package com.example.orders.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Embedded
    private AddressValue shippingAddress;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @OneToMany(mappedBy = "order")
    private List<OrderLineEntity> lines;
}
