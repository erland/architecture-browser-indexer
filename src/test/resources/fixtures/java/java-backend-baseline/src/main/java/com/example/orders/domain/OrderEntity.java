package com.example.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;

    @Column(name = "external_id")
    private String externalId;

    private String description;

    public String getId() {
        return id;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
