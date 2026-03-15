package com.example.orders.domain;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private String id;

    @Version
    private long version;
}
