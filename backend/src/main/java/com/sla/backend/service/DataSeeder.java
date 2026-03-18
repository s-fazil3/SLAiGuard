package com.sla.backend.service;

import com.sla.backend.entity.Metrics;
import com.sla.backend.entity.User;
import com.sla.backend.repository.MetricRepository;
import com.sla.backend.repository.SlaRepository;
import com.sla.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MetricRepository metricRepository;
    private final SlaRepository slaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public DataSeeder(MetricRepository metricRepository, SlaRepository slaRepository, 
                     UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.metricRepository = metricRepository;
        this.slaRepository = slaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        // seedSampleSlas(); // DISABLED - Admin should create SLAs manually
        // seedSampleMetrics(); // DISABLED - Let schedulers generate data
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            // Create admin user
            String adminPassword = passwordEncoder.encode("admin123");
            User adminUser = new User("Admin User", adminPassword, "ROLE_ADMIN", "admin@sla.com");
            
            // Create regular user
            String userPassword = passwordEncoder.encode("user123");
            User regularUser = new User("Regular User", userPassword, "ROLE_USER", "user@sla.com");
            
            userRepository.save(adminUser);
            userRepository.save(regularUser);
            
            System.out.println("Test users created:");
            System.out.println("Admin: admin@sla.com / admin123");
            System.out.println("User: user@sla.com / user123");
        }
    }

    private void seedSampleSlas() {
        long existingCount = slaRepository.count();
        System.out.println("[DataSeeder] Existing SLA count: " + existingCount);
        
        if (existingCount == 0) {
            System.out.println("[DataSeeder] Creating SLAs for all dashboard metrics...");
            
            // Create SLAs aligned with metrics.csv breach thresholds
            // Based on training data where breach=1 occurs at higher values
            
            // Avg Response Time - breach occurs > 250ms in training data
            com.sla.backend.entity.Sla responseTimeSla = new com.sla.backend.entity.Sla();
            responseTimeSla.setMetricName("response_time");
            responseTimeSla.setThreshold(250.0);
            responseTimeSla.setActive(true);
            slaRepository.save(responseTimeSla);
            
            // P95 Latency - breach occurs > 350ms in training data  
            com.sla.backend.entity.Sla p95LatencySla = new com.sla.backend.entity.Sla();
            p95LatencySla.setMetricName("p95_latency");
            p95LatencySla.setThreshold(350.0);
            p95LatencySla.setActive(true);
            slaRepository.save(p95LatencySla);
            
            // Error Rate - breach occurs > 5% in training data
            com.sla.backend.entity.Sla errorRateSla = new com.sla.backend.entity.Sla();
            errorRateSla.setMetricName("error_rate");
            errorRateSla.setThreshold(5.0);
            errorRateSla.setActive(true);
            slaRepository.save(errorRateSla);
            
            // CPU Usage - breach occurs > 80% in training data
            com.sla.backend.entity.Sla cpuUsageSla = new com.sla.backend.entity.Sla();
            cpuUsageSla.setMetricName("cpu_usage");
            cpuUsageSla.setThreshold(80.0);
            cpuUsageSla.setActive(true);
            slaRepository.save(cpuUsageSla);
            
            // Memory Usage - breach occurs > 700MB in training data
            com.sla.backend.entity.Sla memoryUsageSla = new com.sla.backend.entity.Sla();
            memoryUsageSla.setMetricName("memory_usage");
            memoryUsageSla.setThreshold(700.0);
            memoryUsageSla.setActive(true);
            slaRepository.save(memoryUsageSla);
            
            // Requests per Minute - breach occurs > 250 in training data
            com.sla.backend.entity.Sla requestsPerMinSla = new com.sla.backend.entity.Sla();
            requestsPerMinSla.setMetricName("requests_per_minute");
            requestsPerMinSla.setThreshold(250.0);
            requestsPerMinSla.setActive(true);
            slaRepository.save(requestsPerMinSla);
            
            System.out.println("[DataSeeder] Created 6 SLAs for dashboard metrics");
        } else {
            // Print existing SLAs
            slaRepository.findAll().forEach(sla -> {
                System.out.println("[DataSeeder] Existing SLA: " + sla.getMetricName() + " (active=" + sla.isActive() + ", threshold=" + sla.getThreshold() + ")");
            });
        }
    }

    private void seedSampleMetrics() {
        // Get existing metric names
        java.util.Set<String> existingMetricNames = metricRepository.findAll().stream()
            .map(Metrics::getMetricName)
            .collect(java.util.stream.Collectors.toSet());
        
        // Define required metrics aligned with dashboard and CSV data
        java.util.List<String> requiredMetrics = java.util.List.of(
            "response_time", "p95_latency", "error_rate", "cpu_usage", 
            "memory_usage", "requests_per_minute"
        );
        
        boolean hasAllMetrics = existingMetricNames.containsAll(requiredMetrics);
        
        if (metricRepository.count() == 0 || !hasAllMetrics) {
            System.out.println("[DataSeeder] Seeding metrics. Has all types: " + hasAllMetrics + ", Total count: " + metricRepository.count());
            
            // Generate sample data aligned with metrics.csv ranges
            for (int i = 0; i < 50; i++) {
                LocalDateTime timestamp = LocalDateTime.now().minusMinutes((50 - i) * 5);
                
                // Response Time - CSV range: 130-470ms, normal: 130-280ms
                if (!existingMetricNames.contains("response_time") || metricRepository.count() == 0) {
                    double responseValue = 130 + random.nextDouble() * 150; // 130-280ms normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        responseValue = 290 + random.nextDouble() * 180; // 290-470ms
                    }
                    metricRepository.save(new Metrics("response_time", responseValue, timestamp));
                }

                // P95 Latency - CSV range: 180-575ms, normal: 180-350ms
                if (!existingMetricNames.contains("p95_latency") || metricRepository.count() == 0) {
                    double p95Value = 180 + random.nextDouble() * 170; // 180-350ms normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        p95Value = 360 + random.nextDouble() * 215; // 360-575ms
                    }
                    metricRepository.save(new Metrics("p95_latency", p95Value, timestamp));
                }

                // Error Rate - CSV range: 0.3-8.7%, normal: 0.3-3.5%
                if (!existingMetricNames.contains("error_rate") || metricRepository.count() == 0) {
                    double errorRate = 0.3 + random.nextDouble() * 3.2; // 0.3-3.5% normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        errorRate = 3.6 + random.nextDouble() * 5.1; // 3.6-8.7%
                    }
                    metricRepository.save(new Metrics("error_rate", errorRate, timestamp));
                }

                // CPU Usage - CSV range: 38-97%, normal: 38-75%
                if (!existingMetricNames.contains("cpu_usage") || metricRepository.count() == 0) {
                    double cpuValue = 38 + random.nextDouble() * 37; // 38-75% normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        cpuValue = 76 + random.nextDouble() * 21; // 76-97%
                    }
                    metricRepository.save(new Metrics("cpu_usage", cpuValue, timestamp));
                }

                // Memory Usage - CSV range: 360-925MB, normal: 360-650MB
                if (!existingMetricNames.contains("memory_usage") || metricRepository.count() == 0) {
                    double memoryValue = 360 + random.nextDouble() * 290; // 360-650MB normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        memoryValue = 651 + random.nextDouble() * 274; // 651-925MB
                    }
                    metricRepository.save(new Metrics("memory_usage", memoryValue, timestamp));
                }

                // Requests per Minute - CSV range: 165-282, normal: 165-230
                if (!existingMetricNames.contains("requests_per_minute") || metricRepository.count() == 0) {
                    double requestsValue = 165 + random.nextDouble() * 65; // 165-230 normal range
                    if (random.nextDouble() < 0.1) { // 10% chance of high values
                        requestsValue = 231 + random.nextDouble() * 51; // 231-282
                    }
                    metricRepository.save(new Metrics("requests_per_minute", requestsValue, timestamp));
                }
            }

            System.out.println("[DataSeeder] Sample metric data seeded");
        } else {
            System.out.println("[DataSeeder] All metric types already exist, skipping metric seeding");
        }
    }
}
