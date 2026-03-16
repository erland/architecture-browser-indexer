package com.example.orders.domain;

import jakarta.persistence.Column;
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

    @Column(name = "external_id")
    private String externalId;

    private String description;

    @Embedded
    private AddressValue shippingAddress;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @OneToMany(mappedBy = "order")
    private List<OrderLineEntity> lines;

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setShippingAddress(AddressValue shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }
}
