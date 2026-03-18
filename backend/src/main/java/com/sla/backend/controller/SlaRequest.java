package com.sla.backend.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SlaRequest {
    @NotBlank(message = "Metric name is required")
    private String metricname;

    @NotNull(message = "Threshold is required")
    @Positive(message = "Threshold must be positive")
    private double threshold;

    public String getMetricname() {
        return metricname;
    }

    public void setMetricname(String metricname) {
        this.metricname = metricname;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
