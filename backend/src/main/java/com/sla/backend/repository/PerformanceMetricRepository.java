package com.sla.backend.repository;

import com.sla.backend.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceMetricRepository extends JpaRepository<Metric, Long> {
}
