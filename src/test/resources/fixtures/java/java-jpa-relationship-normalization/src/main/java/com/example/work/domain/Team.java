package com.example.work.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @ManyToMany
    @JoinTable(name = "team_members")
    private Set<UserAccount> members = new LinkedHashSet<>();
}
