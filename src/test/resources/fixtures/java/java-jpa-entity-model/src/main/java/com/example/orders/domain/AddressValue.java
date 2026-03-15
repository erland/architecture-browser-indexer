package com.example.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AddressValue {

    @Column(name = "street_name")
    private String street;
}
