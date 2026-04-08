package com.example.work.domain;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private Long id;

    @Version
    private long version;
}
