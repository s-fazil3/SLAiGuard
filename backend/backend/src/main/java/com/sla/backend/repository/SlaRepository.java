package com.sla.backend.repository;

import com.sla.backend.entity.Sla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SlaRepository extends JpaRepository<Sla,Long> {
    Optional<Sla> findByMetricNameAndActiveTrue(String metricName);
}
