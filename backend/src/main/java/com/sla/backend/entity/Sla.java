package com.sla.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "sla_rules")
public class Sla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name")
    private String metricName;
    private double threshold;
    private boolean active;
    private double max_cpu_usage;
    private double max_error_rate;

    public Sla() {
    }

    public Sla(String metricName, double threshold, boolean active) {
        this.metricName = metricName;
        this.threshold = threshold;
        this.active = active;
        this.max_cpu_usage = 100.0;
        this.max_error_rate = 5.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("metricname")
    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    public double getThreshold() {
        return threshold;
    }
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public double getMax_cpu_usage() {
        return max_cpu_usage;
    }

    public void setMax_cpu_usage(double max_cpu_usage) {
        this.max_cpu_usage = max_cpu_usage;
    }
    public double getMax_error_rate() {
        return max_error_rate;
    }

    public void setMax_error_rate(double max_error_rate) {
        this.max_error_rate = max_error_rate;
    }
    }
