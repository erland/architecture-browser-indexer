package com.example.work.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_details")
public class TaskDetails extends BaseEntity {

    @OneToOne(optional = false)
    @MapsId
    @PrimaryKeyJoinColumn
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
}
