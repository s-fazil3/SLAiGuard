package com.sla.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class AlertResponse {
    private Long id;
    @JsonProperty("metricName")
    private String metricname;
    @JsonProperty("currentValue")
    private Double actualValue;
    @JsonProperty("actualCurrentValue")  // New field for the actual current metric value
    private Double currentValue; // The actual current metric value
    private double threshold;
    private String severity;
    private String status;
    @JsonProperty("createdAt")
    private LocalDateTime timeStamp;

    public AlertResponse(Long id, String metricname, Double actualValue, double threshold, String severity, String status, LocalDateTime timeStamp) {
        this.id = id;
        this.metricname = metricname;
        this.actualValue = actualValue;
        this.currentValue = null; // Default for non-predicted alerts
        this.threshold = threshold;
        this.severity = severity;
        this.status = status;
        this.timeStamp = timeStamp;
    }

    public AlertResponse(Long id, String metricname, Double currentValue, Double predictedValue, double threshold, String severity, String status, LocalDateTime timeStamp) {
        this.id = id;
        this.metricname = metricname;
        this.actualValue = predictedValue; // For PREDICTED_RISK, this is the predicted value
        this.currentValue = currentValue; // The actual current metric value
        this.threshold = threshold;
        this.severity = severity;
        this.status = status;
        this.timeStamp = timeStamp;
    }

    public Long getId() {
        return id;
    }

    public String getMetricname() {
        return metricname;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }
}
