package com.sla.backend.controller;
import com.sla.backend.dto.SlaComplianceDTO;
import com.sla.backend.entity.Sla;
import com.sla.backend.service.SlaService;
import com.sla.backend.controller.SlaRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/sla")
public class SlaController {
    private final SlaService slaService;
    
    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }
    
    @PostMapping
    public Sla createSla(@Valid @RequestBody SlaRequest slaRequest) {
        return slaService.createSla(slaRequest.getMetricname(), slaRequest.getThreshold());
    }
    
    @GetMapping
    public List<Sla> getAllSlas() {
        return slaService.getAllSlas();
    }
    
    @PutMapping("/{id}")
    public Sla updateSla(@PathVariable Long id, @Valid @RequestBody SlaRequest slaRequest) {
        return slaService.updateSla(id, slaRequest.getMetricname(), slaRequest.getThreshold());
    }
    
    @DeleteMapping("/{id}")
    public void deleteSla(@PathVariable Long id) {
        slaService.deleteSla(id);
    }
    @PatchMapping("/{id}/toggle")
    public Sla toggleSla(@PathVariable Long id) {
        return slaService.toggleSlaStatus(id);
    }

    @GetMapping("/compliance/{metricname}")
    public SlaComplianceDTO getSlaCompliance(@PathVariable String metricname) {
        return slaService.calculateSlaCompliance(metricname);
    }

    @PostMapping("/create-enterprise-standard")
    public ResponseEntity<String> createEnterpriseStandardSlas() {
        try {
            slaService.createEnterpriseStandardSlas();
            return ResponseEntity.ok("Enterprise-standard SLAs created successfully based on current metric values and ML predictions");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to create enterprise-standard SLAs: " + e.getMessage());
        }
    }
}