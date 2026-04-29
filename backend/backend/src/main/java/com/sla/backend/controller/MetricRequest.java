package com.sla.backend.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MetricRequest {
    @NotBlank(message = "metricname should not be empty")
    String metricname;

    @NotNull(message = "metric value is required")
    @Positive(message = "value must be positive")
    double value;

    public String getMetricname() {
        return metricname;
    }

    public double getValue() {
        return value;
    }

    public void setMetricname(String metricname) {
        this.metricname = metricname;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
