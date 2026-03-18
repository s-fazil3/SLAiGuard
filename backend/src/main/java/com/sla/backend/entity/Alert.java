package com.sla.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String metricname;
    private double actualValue;  // For PREDICTED_RISK, this is the predicted value
    private double currentValue; // The actual current metric value
    private double threshold;
    private String severity;
    private LocalDateTime timestamp;
    private boolean acknowledged;
    
    @Column(name = "status")
    private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED

    public Alert() {

    }

    public Alert(String metricname,double actualValue, String severity,double threshold, boolean acknowledged, LocalDateTime timestamp) {
        this.metricname=metricname;
        this.actualValue = actualValue;
        this.severity = severity;
        this.threshold=threshold;
        this.timestamp = timestamp;
        this.acknowledged = false;
    }

    public Alert(String metricname, double actualValue, String severity, double threshold, LocalDateTime now) {
        this.metricname=metricname;
        this.actualValue = actualValue;
        this.severity = severity;
        this.threshold=threshold;
        this.timestamp = now;
        this.acknowledged = false;
        this.status = "ACTIVE";
    }

    public Alert(String metricname, double currentValue, double predictedValue, String severity, double threshold, LocalDateTime now) {
        this.metricname=metricname;
        this.currentValue = currentValue;
        this.actualValue = predictedValue; // For PREDICTED_RISK, actualValue stores predicted value
        this.severity = severity;
        this.threshold=threshold;
        this.timestamp = now;
        this.acknowledged = false;
        this.status = "ACTIVE";
    }

    public Long getId() {
        return id;
    }

    public String getMetricname() {
        return metricname;
    }

    public double getActualValue() {
        return actualValue;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
        if (acknowledged && !"RESOLVED".equals(this.status)) {
            this.status = "ACKNOWLEDGED";
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void resolve() {
        this.status = "RESOLVED";
        this.acknowledged = true;
    }

    public double getThreshold() {
        return threshold;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
