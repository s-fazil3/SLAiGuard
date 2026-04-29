package com.sla.backend.dto;

public class SlaComplianceDTO {
    private String metricName;
    private Double responseTime;
    private Double errorRate;
    private Double compliancePercentage;
    private Double riskScore;         // ← Added for RF alignment
    private Double predictedValue;    // ← Added for RF alignment
    private String status;

    public SlaComplianceDTO() {
    }

    public SlaComplianceDTO(String metricName, Double responseTime, Double errorRate, Double compliancePercentage, Double riskScore, Double predictedValue, String status) {
        this.metricName = metricName;
        this.responseTime = responseTime;
        this.errorRate = errorRate;
        this.compliancePercentage = compliancePercentage;
        this.riskScore = riskScore;
        this.predictedValue = predictedValue;
        this.status = status;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Double responseTime) {
        this.responseTime = responseTime;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Double getCompliancePercentage() {
        return compliancePercentage;
    }

    public void setCompliancePercentage(Double compliancePercentage) {
        this.compliancePercentage = compliancePercentage;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Double getPredictedValue() {
        return predictedValue;
    }

    public void setPredictedValue(Double predictedValue) {
        this.predictedValue = predictedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
