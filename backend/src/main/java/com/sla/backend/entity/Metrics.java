package com.sla.backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Metrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "metricname")
    private String metricName;
    private double value;
    private LocalDateTime dateTime;

    public Metrics(){
    }

    public Metrics(String metricName,double value,LocalDateTime dateTime){
        this.metricName=metricName;
        this.value=value;
        this.dateTime=dateTime;
    }
    public Long getId() {
        return id;
    }

    public String getMetricName() {
        return metricName;
    }

    public double getValue() {
        return value;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
