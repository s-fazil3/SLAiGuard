import { useEffect, useState } from "react";
import api from "../api/api";
import AlertCard from "../components/AlertCard";
import "../styles/Dashboard.css";

export default function Dashboard() {
  const [alerts, setAlerts] = useState([]);
  const [dashboardMetrics, setDashboardMetrics] = useState(null);
  const [monitoringMetrics, setMonitoringMetrics] = useState([]);
  const [performanceMetrics, setPerformanceMetrics] = useState([]);
  const [slas, setSlas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const REFRESH_INTERVAL = 15000;

  const refreshData = async () => {
    await Promise.all([fetchAlerts(), fetchDashboardMetrics(), fetchMonitoringMetrics(), fetchPerformanceMetrics(), fetchSlas()]);
    setLastUpdated(new Date());
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        await Promise.all([fetchAlerts(), fetchDashboardMetrics(), fetchMonitoringMetrics(), fetchPerformanceMetrics(), fetchSlas()]);
        setLoading(false);
      } catch (error) {
        console.error("Error loading initial data:", error);
        setLoading(false);
      }
      setLastUpdated(new Date());
    };
    loadData();
    const intervalId = setInterval(async () => {
      console.log("Auto-refresh triggered at:", new Date().toLocaleTimeString());
      try {
        await refreshData();
        console.log("Auto-refresh completed successfully");
      } catch (error) {
        console.error("Auto-refresh failed:", error);
      }
    }, REFRESH_INTERVAL);
    return () => {
      console.log("Cleaning up dashboard auto-refresh interval");
      clearInterval(intervalId);
    };
  }, []);
  const fetchAlerts = async () => {
    try {
      const timestamp = new Date().getTime();
      const res = await api.get(`/api/alerts?_t=${timestamp}`);
      const alertData = res.data.content || [];
      const uniqueAlerts = alertData.filter((alert, index, self) =>
        index === self.findIndex((a) => a.id === alert.id)
      );
      setAlerts(uniqueAlerts);
      console.log(`[Dashboard] Fetched ${uniqueAlerts.length} alerts at ${new Date().toLocaleTimeString()}`);
    } catch (err) {
      console.error("Failed to fetch alerts");
    }
  };

  const fetchDashboardMetrics = async () => {
    try {
      const timestamp = new Date().getTime();
      const res = await api.get(`/api/metrics/dashboard?_t=${timestamp}`);
      setDashboardMetrics(res.data);
      console.log(`[Dashboard] Metrics updated at ${new Date().toLocaleTimeString()}:`, {
        avgResponseTime: res.data.avgResponseTime,
        memoryUsage: res.data.memoryUsage,
        cpuUsage: res.data.cpuUsage,
        errorRate: res.data.errorRate
      });
    } catch (err) {
      console.error("Failed to fetch dashboard metrics");
      setDashboardMetrics({
        avgResponseTime: 0,
        errorRate: 0,
        requestPerMinute: 0,
        cpuUsage: 0,
        memoryUsage: 0,
        uptimePercentage: 100,
        riskScore: 0
      });
    }
  };

  const fetchMonitoringMetrics = async () => {
    try {
      const res = await api.get("/api/metrics");
      setMonitoringMetrics(res.data);
    } catch (err) {
      console.error("Failed to fetch monitoring metrics");
    }
  };

  const fetchPerformanceMetrics = async () => {
    try {
      const res = await api.get("/api/metrics/performance");
      setPerformanceMetrics(res.data);
    } catch (err) {
      console.error("Failed to fetch performance metrics");
    }
  };

  const fetchSlas = async () => {
    try {
      const res = await api.get("/api/sla");
      setSlas(res.data);
    } catch (err) {
      console.error("Failed to fetch SLAs:", err);
    }
  };

  const acknowledgeAlert = async (alertId) => {
    try {
      await api.put(`/api/alerts/${alertId}/acknowledge`);
      fetchAlerts(); // Refresh alerts
    } catch (err) {
      console.error("Failed to acknowledge alert:", err);
    }
  };

  const getSlaThreshold = (metricName) => {
    const sla = slas.find(s => s.metricname === metricName && s.active);
    return sla ? sla.threshold : null;
  };

  const criticalCount = alerts.filter(
    (a) => a.severity === "CRITICAL"
  ).length;

  const predictedCount = alerts.filter(
    (a) => a.severity === "PREDICTED_RISK"
  ).length;

  // Get unique alerts by metric (latest alert per metric)
  const getUniqueAlerts = () => {
    const alertMap = new Map();
    alerts.forEach(alert => {
      const metricKey = alert.metricName || 'unknown';
      if (!alertMap.has(metricKey) ||
        new Date(alert.createdAt) > new Date(alertMap.get(metricKey).createdAt)) {
        alertMap.set(metricKey, alert);
      }
    });
    return Array.from(alertMap.values());
  };

  // Get metric value with null safety
  const getMetricValue = (value, unit = '', decimals = 0) => {
    if (value === undefined || value === null) return '--';
    if (decimals > 0) return `${Number(value).toFixed(decimals)}${unit}`;
    return `${Math.round(value)}${unit}`;
  };

  return (
    <div className="dashboard">
      {/* Header Section */}
      <div className="dashboard-header">
        <div className="header-content">
          <div>
            <h1 className="dashboard-title">System Dashboard</h1>
            <p className="dashboard-subtitle">
              Real-time monitoring and analytics for your infrastructure
            </p>
            <p className="last-updated">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </p>
          </div>
          <button className="refresh-button" onClick={refreshData}>
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
            </svg>
            Refresh
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p className="loading-text">Loading dashboard data...</p>
        </div>
      ) : (
        <>
          {/* KPI Cards Section */}
          <div className="kpi-section">
            <div className="kpi-card interactive-card">
              <div className="kpi-card-content">
                <h3>Monitored Metrics</h3>
                <p className="kpi-value">{monitoringMetrics.length || '0'}</p>
                <p className="kpi-hint">Total data points being tracked</p>
                <div className="kpi-trend">
                  <span className="trend-up">↗ +12%</span>
                </div>
              </div>
            </div>

            <div className="kpi-card critical interactive-card">
              <div className="kpi-card-content">
                <h3>Critical Issues</h3>
                <p className="kpi-value">{criticalCount || '0'}</p>
                <div className="kpi-trend">
                  <span className="trend-down">↘ -8%</span>
                </div>
              </div>
            </div>

            <div className="kpi-card predicted interactive-card">
              <div className="kpi-card-content">
                <h3>AI Predictions</h3>
                <p className="kpi-value">{predictedCount || '0'}</p>
                <div className="kpi-trend">
                  <span className="trend-up">↗ +24%</span>
                </div>
              </div>
            </div>

            <div className="kpi-card success interactive-card">
              <div className="kpi-card-content">
                <h3>System Health</h3>
                <p className="kpi-value">
                  {dashboardMetrics?.uptimePercentage !== undefined ? `${Math.round(dashboardMetrics.uptimePercentage)}%` : '98.5%'}
                </p>
                <div className="kpi-trend">
                  <span className="trend-up">↗ +2.1%</span>
                </div>
              </div>
            </div>

            {/* Additional structured metrics cards */}
            <div className="kpi-card glassmorphic">
              <div className="kpi-card-content">
                <h3>Response Time</h3>
                <p className="kpi-value">
                  {dashboardMetrics?.avgResponseTime !== undefined ? `${Math.round(dashboardMetrics.avgResponseTime)}ms` : 'Loading...'}
                </p>
                {slas.some(s => s.metricname === 'response_time' && s.active) && (
                  <div className="kpi-trend">
                    <span className="trend-neutral sla-threshold">SLA: {getSlaThreshold('response_time')}ms</span>
                  </div>
                )}
                {!slas.some(s => s.metricname === 'response_time' && s.active) && (
                  <div className="kpi-trend">
                    <span className="trend-up">↗ Target: &lt;100ms</span>
                  </div>
                )}
              </div>
            </div>

            <div className="kpi-card glassmorphic">
              <div className="kpi-card-content">
                <h3>Memory Usage</h3>
                <p className="kpi-value">
                  {dashboardMetrics?.memoryUsage !== undefined ? `${Math.round(dashboardMetrics.memoryUsage)}MB` : 'Loading...'}
                </p>
                {slas.some(s => s.metricname === 'memory_usage' && s.active) && (
                  <div className="kpi-trend">
                    <span className="trend-neutral sla-threshold">SLA: {getSlaThreshold('memory_usage')}MB</span>
                  </div>
                )}
                {!slas.some(s => s.metricname === 'memory_usage' && s.active) && (
                  <div className="kpi-trend">
                    <span className="trend-neutral">→ Stable</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* API Performance Section */}
          <div className="metrics-section">
            <div className="section-header">
              <span className="section-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z" />
                  <path d="M12.5 7H11v6l5.25 3.15.75-1.23-4.5-2.67z" />
                </svg>
              </span>
              <h2 className="section-title">API Performance</h2>
            </div>
            <div className="metrics-grid">
              {/* Avg Response Time */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Avg Response Time</h3>
                  <span className="metric-badge active">PERFORMANCE</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.avgResponseTime, 'ms')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    Last 20 API calls
                  </span>
                </div>
              </div>

              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">P95 Latency</h3>
                  <span className="metric-badge active">PERFORMANCE</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.p95Latency, 'ms')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    95th percentile
                  </span>
                  {slas.some(s => s.metricname === 'p95_latency' && s.active) && (
                    <span className="sla-indicator"> | SLA: {getSlaThreshold('p95_latency')}ms</span>
                  )}
                </div>
              </div>

              {/* API Error Rate */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">API Error Rate</h3>
                  <span className="metric-badge active">PERFORMANCE</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.errorRate, '%')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    Failed requests %
                  </span>
                </div>
              </div>

              {/* Requests per Minute */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Requests per Minute (RPM)</h3>
                  <span className="metric-badge active">PERFORMANCE</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.requestPerMinute)}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    Current throughput
                  </span>
                </div>
              </div>

              {/* API Success Rate */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Success Rate</h3>
                  <span className="metric-badge active">PERFORMANCE</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.uptimePercentage, '%')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    Successful responses
                  </span>
                </div>
              </div>

              {/* Total API Calls */}
              {performanceMetrics.length > 0 && (
                <div className="metric-card">
                  <div className="metric-header">
                    <h3 className="metric-name">Total API Calls</h3>
                    <span className="metric-badge active">PERFORMANCE</span>
                  </div>
                  <span className="metric-value">{performanceMetrics.length}</span>
                  <div className="metric-meta">
                    <span className="metric-timestamp">
                      Monitored endpoints
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
          <div className="metrics-section">
            <div className="section-header">
              <span className="section-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z" />
                </svg>
              </span>
              <h2 className="section-title">System Metrics</h2>
            </div>
            <div className="metrics-grid">
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">CPU Usage</h3>
                  <span className="metric-badge active">SYSTEM</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.cpuUsage, '%')}
                </span>
                {getSlaThreshold('cpu_usage') !== null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp sla-threshold">SLA: {getSlaThreshold('cpu_usage')}%</span>
                  </div>
                )}
                {getSlaThreshold('cpu_usage') === null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp">
                      {new Date().toLocaleString()}
                    </span>
                  </div>
                )}
              </div>
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Memory Usage</h3>
                  <span className="metric-badge active">SYSTEM</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.memoryUsage, 'MB')}
                </span>
                {getSlaThreshold('memory_usage') !== null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp sla-threshold">SLA: {getSlaThreshold('memory_usage')}MB</span>
                  </div>
                )}
                {getSlaThreshold('memory_usage') === null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp">
                      {new Date().toLocaleString()}
                    </span>
                  </div>
                )}
              </div>

              {/* Network Latency */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Network Latency</h3>
                  <span className="metric-badge active">SYSTEM</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.networkLatency, 'ms')}
                </span>
                {getSlaThreshold('network_latency') !== null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp sla-threshold">SLA: {getSlaThreshold('network_latency')}ms</span>
                  </div>
                )}
                {getSlaThreshold('network_latency') === null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp">
                      {new Date().toLocaleString()}
                    </span>
                  </div>
                )}
              </div>

              {/* Disk Usage */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Disk Usage</h3>
                  <span className="metric-badge active">SYSTEM</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.diskUsage, '%')}
                </span>
                {getSlaThreshold('disk_usage') !== null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp sla-threshold">SLA: {getSlaThreshold('disk_usage')}%</span>
                  </div>
                )}
                {getSlaThreshold('disk_usage') === null && (
                  <div className="metric-meta">
                    <span className="metric-timestamp">
                      {new Date().toLocaleString()}
                    </span>
                  </div>
                )}
              </div>

              {/* Uptime */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Uptime</h3>
                  <span className="metric-badge active">SYSTEM</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.uptimePercentage, '%')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    {new Date().toLocaleString()}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Intelligence Layer Section */}
          <div className="metrics-section">
            <div className="section-header">
              <span className="section-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                </svg>
              </span>
              <h2 className="section-title">Intelligence Layer</h2>
            </div>
            <div className="metrics-grid">
              {/* Rule-Based Risk Score */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">Rule-Based Risk Score</h3>
                  <span className="metric-badge active">AI</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.riskScore, '%')}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    Calculated from thresholds
                  </span>
                </div>
              </div>

              {/* ML Breach Probability */}
              <div className="metric-card">
                <div className="metric-header">
                  <h3 className="metric-name">ML Breach Probability</h3>
                  <span className="metric-badge active">AI</span>
                </div>
                <span className="metric-value">
                  {getMetricValue(dashboardMetrics?.mlRiskScore, '%', 1)}
                </span>
                <div className="metric-meta">
                  <span className="metric-timestamp">
                    ML model prediction
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Alerts Section */}
          <div className="alerts-section">
            <div className="section-header">
              <span className="section-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z" />
                </svg>
              </span>
              <h2 className="section-title">Active Alerts</h2>
            </div>
            <div className="alerts-grid">
              {alerts.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">🎉</div>
                  <h3 className="empty-title">All Systems Operational</h3>
                  <p className="empty-description">
                    No alerts to display. Your systems are running smoothly!
                  </p>
                </div>
              ) : (
                getUniqueAlerts().slice(0, 6).map((alert) => (
                  <AlertCard key={alert.id} alert={alert} onAcknowledge={acknowledgeAlert} />
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
