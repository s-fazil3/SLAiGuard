package com.sla.backend.aspect;
import com.sla.backend.service.PerformanceMetricService;
import com.sla.backend.service.MetricService;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

// @Aspect  // TEMPORARILY DISABLED - Database schema conflicts
@Component
public class PerformanceMonitoringAspect_DISABLED {

    private final PerformanceMetricService metricService;
    private final MetricService metricServiceForSla;

    public PerformanceMonitoringAspect_DISABLED(PerformanceMetricService metricService, MetricService metricServiceForSla) {
        this.metricService = metricService;
        this.metricServiceForSla = metricServiceForSla;
    }

    @Pointcut("execution(* com.sla.backend.controller..*(..))")
    public void controllerMethods() {}

    // @Around("controllerMethods()")  // TEMPORARILY DISABLED - Database schema conflicts
    /* 
    public Object logPerformanceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        // ENTIRE METHOD DISABLED - Database schema conflicts
        return null;
    }
    */
}
