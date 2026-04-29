package com.sla.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_metrics", indexes = {
    @Index(name = "idx_perf_metric_service_name", columnList = "service_name"),
    @Index(name = "idx_perf_metric_timestamp", columnList = "timestamp"),
    @Index(name = "idx_perf_metric_service_timestamp", columnList = "service_name, timestamp")
})
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String serviceName;
    private Long responseTime;
    private Boolean errorOccurred;
    private Long memoryUsage;
    private Double errorRate;
    private Long requestCount;
    private Double cpuUsage;
    private Double uptimePercentage;
    private LocalDateTime timestamp;

    public Metric() {
    }

    public Metric(String serviceName, Long responseTime, Boolean errorOccurred, Long memoryUsage, LocalDateTime timestamp) {
        this.serviceName = serviceName;
        this.responseTime = responseTime;
        this.errorOccurred = errorOccurred;
        this.memoryUsage = memoryUsage;
        this.timestamp = timestamp;
    }

    // Getters and setters
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

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public Boolean getErrorOccurred() {
        return errorOccurred;
    }

    public void setErrorOccurred(Boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }

    public Long getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(Double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
