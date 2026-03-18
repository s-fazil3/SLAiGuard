package com.sla.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_prediction_history", indexes = {
    @Index(name = "idx_ml_service_name", columnList = "service_name"),
    @Index(name = "idx_ml_timestamp", columnList = "timestamp"),
    @Index(name = "idx_ml_service_timestamp", columnList = "service_name, timestamp")
})
public class MLPredictionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "avg_response_time")
    private Double avgResponseTime;

    @Column(name = "error_rate")
    private Double errorRate;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "p95_latency")
    private Double p95Latency;

    @Column(name = "request_count")
    private Long requestCount;

    @Column(name = "breach_probability")
    private Double breachProbability;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "severity")
    private String severity;

    /**
     * JSON representation of the Random Forest per-metric predicted next-tick values.
     * Example: {"cpu_usage":52.3,"memory_usage":480.1,"avg_response_time":310.5,...}
     * Stored as-is so MetricService can look up the RF prediction for any specific metric.
     */
    @Column(name = "predicted_metrics", columnDefinition = "TEXT")
    private String predictedMetrics;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public MLPredictionHistory() {
    }

    public MLPredictionHistory(String serviceName, Double avgResponseTime, Double errorRate,
                             Double cpuUsage, Double memoryUsage, Double p95Latency,
                             Long requestCount, Double breachProbability, Double riskScore,
                             String severity, LocalDateTime timestamp) {
        this.serviceName = serviceName;
        this.avgResponseTime = avgResponseTime;
        this.errorRate = errorRate;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.p95Latency = p95Latency;
        this.requestCount = requestCount;
        this.breachProbability = breachProbability;
        this.riskScore = riskScore;
        this.severity = severity;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(Double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Double getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(Double p95Latency) {
        this.p95Latency = p95Latency;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Double getBreachProbability() {
        return breachProbability;
    }

    public void setBreachProbability(Double breachProbability) {
        this.breachProbability = breachProbability;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPredictedMetrics() {
        return predictedMetrics;
    }

    public void setPredictedMetrics(String predictedMetrics) {
        this.predictedMetrics = predictedMetrics;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
