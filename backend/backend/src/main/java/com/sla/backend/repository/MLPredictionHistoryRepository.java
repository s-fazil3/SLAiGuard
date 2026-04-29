package com.sla.backend.repository;

import com.sla.backend.entity.MLPredictionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MLPredictionHistoryRepository extends JpaRepository<MLPredictionHistory, Long> {

    // Find latest prediction for a specific service
    @Query("SELECT mlh FROM MLPredictionHistory mlh WHERE mlh.serviceName = :serviceName ORDER BY mlh.timestamp DESC")
    List<MLPredictionHistory> findLatestByServiceName(String serviceName);

    // Find the most recent prediction across all services
    @Query("SELECT mlh FROM MLPredictionHistory mlh ORDER BY mlh.timestamp DESC")
    List<MLPredictionHistory> findLatestAll();

    // Find predictions in the last N minutes
    @Query("SELECT mlh FROM MLPredictionHistory mlh WHERE mlh.timestamp >= :since ORDER BY mlh.timestamp DESC")
    List<MLPredictionHistory> findRecent(LocalDateTime since);

    // Find the single most recent prediction
    @Query("SELECT mlh FROM MLPredictionHistory mlh ORDER BY mlh.timestamp DESC LIMIT 1")
    Optional<MLPredictionHistory> findMostRecent();
}
