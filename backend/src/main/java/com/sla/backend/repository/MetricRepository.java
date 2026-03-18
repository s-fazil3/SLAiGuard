package com.sla.backend.repository;

import com.sla.backend.entity.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricRepository extends JpaRepository<Metrics,Long> {
    List<Metrics> findTop10ByMetricNameOrderByDateTimeDesc(String metricName);
    List<Metrics> findByDateTimeAfter(java.time.LocalDateTime since);
}
