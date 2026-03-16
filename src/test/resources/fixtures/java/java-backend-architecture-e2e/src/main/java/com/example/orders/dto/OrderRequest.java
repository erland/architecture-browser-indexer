package com.example.orders.dto;

public class OrderRequest {
    private String externalId;
    private String description;
    private String customerId;
    private String street;

    public String getExternalId() {
        return externalId;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStreet() {
        return street;
    }
}
