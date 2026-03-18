package com.sla.backend.dto;

public class DashboardMetricsDTO {
    private Double avgResponseTime;
    private Double p95Latency;
    private Double errorRate;
    private Long requestPerMinute;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double networkLatency;
    private Double diskUsage;
    private Double uptimePercentage;
    private Double riskScore;
    private Double mlRiskScore;

    public DashboardMetricsDTO() {
    }

    public DashboardMetricsDTO(Double avgResponseTime, Double p95Latency, Double errorRate, Long requestPerMinute,
                              Double cpuUsage, Double memoryUsage, Double networkLatency, Double diskUsage, Double uptimePercentage, Double riskScore, Double mlRiskScore) {
        this.avgResponseTime = avgResponseTime;
        this.p95Latency = p95Latency;
        this.errorRate = errorRate;
        this.requestPerMinute = requestPerMinute;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.networkLatency = networkLatency;
        this.diskUsage = diskUsage;
        this.uptimePercentage = uptimePercentage;
        this.riskScore = riskScore;
        this.mlRiskScore = mlRiskScore;
    }

    public Double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(Double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public Double getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(Double p95Latency) {
        this.p95Latency = p95Latency;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Long getRequestPerMinute() {
        return requestPerMinute;
    }

    public void setRequestPerMinute(Long requestPerMinute) {
        this.requestPerMinute = requestPerMinute;
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

    public Double getNetworkLatency() {
        return networkLatency;
    }

    public void setNetworkLatency(Double networkLatency) {
        this.networkLatency = networkLatency;
    }

    public Double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(Double diskUsage) {
        this.diskUsage = diskUsage;
    }

    public Double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(Double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Double getMlRiskScore() {
        return mlRiskScore;
    }

    public void setMlRiskScore(Double mlRiskScore) {
        this.mlRiskScore = mlRiskScore;
    }
}
