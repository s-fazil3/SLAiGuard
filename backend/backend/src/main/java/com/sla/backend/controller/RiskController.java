package com.sla.backend.controller;

import com.sla.backend.dto.RiskScore;
import com.sla.backend.service.RiskPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    
    private final RiskPredictionService riskPredictionService;
    
    public RiskController(RiskPredictionService riskPredictionService) {
        this.riskPredictionService = riskPredictionService;
    }
    
    @GetMapping("/score/{serviceName}")
    public ResponseEntity<RiskScore> getRiskScore(@PathVariable String serviceName) {
        RiskScore riskScore = riskPredictionService.calculateRiskScore(serviceName);
        return ResponseEntity.ok(riskScore);
    }
    
    @GetMapping("/scores")
    public ResponseEntity<List<RiskScore>> getAllRiskScores() {
        // Calculate risk scores for all metrics that could have SLAs
        List<String> serviceNames = List.of(
            "cpu_usage", "memory_usage", "disk_usage", "network_latency",
            "response_time", "error_rate", "api_error_rate", "api_success_rate",
            "api_throughput", "requests_per_min", "uptime", "requests_per_second",
            "database_connections", "cache_hit_rate", "io_operations"
        );
        List<RiskScore> riskScores = serviceNames.stream()
                .map(serviceName -> {
                    try {
                        return riskPredictionService.calculateRiskScore(serviceName);
                    } catch (Exception e) {
                        System.out.println("[RiskController] Failed to calculate risk score for " + serviceName + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(riskScore -> riskScore != null)
                .toList();
        return ResponseEntity.ok(riskScores);
    }
}
