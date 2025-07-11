package com.example.worktime.model;

import javax.persistence.*;

@Entity
public class WorkTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer plannedQty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPlannedQty() {
        return plannedQty;
    }

    public void setPlannedQty(Integer plannedQty) {
        this.plannedQty = plannedQty;
    }
}
