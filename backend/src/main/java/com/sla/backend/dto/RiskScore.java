package com.sla.backend.dto;

public class RiskScore {
    private String serviceName;
    private double currentValue;
    private double predictedRisk;
    private String severityLevel;

    public RiskScore() {
    }

    public RiskScore(String serviceName, double currentValue, double predictedRisk, String severityLevel) {
        this.serviceName = serviceName;
        this.currentValue = currentValue;
        this.predictedRisk = predictedRisk;
        this.severityLevel = severityLevel;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getPredictedRisk() {
        return predictedRisk;
    }

    public void setPredictedRisk(double predictedRisk) {
        this.predictedRisk = predictedRisk;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }
}
