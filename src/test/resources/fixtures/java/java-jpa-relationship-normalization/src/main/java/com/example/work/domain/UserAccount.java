package com.example.work.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

    @ManyToMany(mappedBy = "members")
    private Set<Team> teams = new LinkedHashSet<>();
}
