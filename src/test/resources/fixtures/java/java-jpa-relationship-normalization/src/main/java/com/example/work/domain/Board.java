package com.example.work.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
public class Board extends BaseEntity {

    @OneToMany
    private List<ColumnEntity> columns = new ArrayList<>();
}
