package com.sla.backend.repository;

import com.sla.backend.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert,Long> {
    List<Alert> findByAcknowledgedFalse();
    List<Alert> findByAcknowledgedTrue();
    Page<Alert> findByAcknowledgedFalse(Pageable pageable);
    Page<Alert> findByAcknowledgedFalseAndSeverity(
            String severity,
            Pageable pageable
    );
    List<Alert> findByAcknowledgedFalseAndMetricname(String metricname);
    boolean existsByAcknowledgedFalseAndMetricname(String metricname);
    
    @Query("SELECT a FROM Alert a WHERE a.metricname = ?1 AND a.timestamp >= ?2 ORDER BY a.timestamp DESC")
    List<Alert> findRecentAlertsByMetricnameAfter(String metricname, LocalDateTime since);

    Optional<Alert> findTopByMetricnameAndTimestampAfterOrderByTimestampDesc(String metricname, LocalDateTime since);
    
    Page<Alert> findByStatusNot(String status, Pageable pageable);
    
    List<Alert> findByMetricnameAndStatusNot(String metricname, String status);
    List<Alert> findByStatusNot(String status);
}
