package com.sla.backend.service;

import com.sla.backend.dto.RiskScore;
import com.sla.backend.entity.Metrics;
import com.sla.backend.repository.MetricRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskPredictionService {
    
    private final MetricRepository metricRepository;
    
    public RiskPredictionService(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }
    
    public RiskScore calculateRiskScore(String serviceName) {
        // Updated to 10-tick window for stability and system-wide consistency
        List<Metrics> recentMetrics = metricRepository.findTop10ByMetricNameOrderByDateTimeDesc(serviceName);
        
        if (recentMetrics.isEmpty()) {
            return new RiskScore(serviceName, 0.0, 0.0, "LOW");
        }
        
        double currentValue = recentMetrics.get(0).getValue();
        double riskScore = calculateTrendBasedRisk(recentMetrics);
        String severityLevel = classifyRisk(riskScore);
        
        RiskScore riskScoreDto = new RiskScore(serviceName, currentValue, riskScore, severityLevel);
        // Risk score is calculated for monitoring only - alerts are triggered by SLA violations in MetricService
        
        return riskScoreDto;
    }
    
    private double calculateTrendBasedRisk(List<Metrics> metrics) {
        if (metrics.size() < 2) {
            return 0.0;
        }
        
        double[] values = metrics.stream()
                .mapToDouble(Metrics::getValue)
                .toArray();
        
        double movingAverage = calculateMovingAverage(values);
        double slope = calculateSlope(values);
        double volatility = calculateVolatility(values, movingAverage);
        
        double trendScore = Math.min(slope * 10, 50);
        double volatilityScore = Math.min(volatility * 20, 30);
        double levelScore = Math.min(movingAverage / 100 * 20, 20);
        
        return Math.min(Math.max(trendScore + volatilityScore + levelScore, 0), 100);
    }
    
    private double calculateMovingAverage(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    private double calculateSlope(double[] values) {
        int n = values.length;
        if (n < 2) return 0;
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += i * values[i];
            sumX2 += i * i;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    private double calculateVolatility(double[] values, double mean) {
        double sumSquaredDiffs = 0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / values.length);
    }
    
    private String classifyRisk(double riskScore) {
        if (riskScore <= 40) {
            return "LOW";
        } else if (riskScore <= 70) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}
