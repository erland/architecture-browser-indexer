package com.example.work.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "archivedFromProject")
    private Set<Task> archivedTasks = new LinkedHashSet<>();
}
