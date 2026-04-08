package com.example.work.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "work_orders")
public class WorkOrder extends BaseEntity {

    @Embedded
    private AddressValue shippingAddress;

    @ElementCollection
    private Set<String> tags = new LinkedHashSet<>();
}
