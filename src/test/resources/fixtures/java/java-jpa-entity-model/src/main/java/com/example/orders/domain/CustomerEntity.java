package com.example.orders.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity extends BaseEntity {
}
