package com.sla.backend.repository;

import com.sla.backend.entity.AggregatedMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedMetricsRepository extends JpaRepository<AggregatedMetrics, Long> {

    // Find latest aggregated metrics for a specific service
    @Query("SELECT am FROM AggregatedMetrics am WHERE am.serviceName = :serviceName ORDER BY am.timestamp DESC")
    List<AggregatedMetrics> findLatestByServiceName(String serviceName);

    // Find the most recent aggregated metrics across all services
    @Query("SELECT am FROM AggregatedMetrics am ORDER BY am.timestamp DESC")
    List<AggregatedMetrics> findLatestAll();

    // Find aggregated metrics in the last N minutes
    @Query("SELECT am FROM AggregatedMetrics am WHERE am.timestamp >= :since ORDER BY am.timestamp DESC")
    List<AggregatedMetrics> findRecent(LocalDateTime since);

    // Find aggregated metrics for a specific service in the last N minutes
    @Query("SELECT am FROM AggregatedMetrics am WHERE am.serviceName = :serviceName AND am.timestamp >= :since ORDER BY am.timestamp DESC")
    List<AggregatedMetrics> findRecentByServiceName(String serviceName, LocalDateTime since);

    // Find the single most recent aggregated metric
    @Query("SELECT am FROM AggregatedMetrics am ORDER BY am.timestamp DESC LIMIT 1")
    Optional<AggregatedMetrics> findMostRecent();
}
