import { useEffect, useState } from "react";
import api from "../api/api";
import "../styles/Alerts.css";

export default function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filterSeverity, setFilterSeverity] = useState("all");
  const [sortBy, setSortBy] = useState("timestamp"); // "timestamp", "severity", "metric"

  useEffect(() => {
    fetchAlerts();
    const interval = setInterval(fetchAlerts, 30000); // Refresh every 30 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchAlerts = async () => {
    try {
      const res = await api.get("/api/alerts");
      const alertData = Array.isArray(res.data) ? res.data : res.data.content || [];
      // Deduplicate alerts by ID and filter out invalid alerts
      // Invalid = threshold 0 (not a real SLA) OR prediction alerts without threshold
      const uniqueAlerts = alertData
        .filter((alert) => {
          // Only include alerts with valid thresholds
          if (alert.threshold === 0) return false;
          return true;
        })
        .filter((alert, index, self) =>
          index === self.findIndex((a) => a.id === alert.id)
        );
      setAlerts(uniqueAlerts);
    } catch (err) {
      setError("Failed to load alerts");
    } finally {
      setLoading(false);
    }
  };

  const filteredAlerts = filterSeverity === "all" 
    ? alerts 
    : alerts.filter(a => a.severity?.toLowerCase().includes(filterSeverity.toLowerCase()));

  const sortedAlerts = [...filteredAlerts].sort((a, b) => {
    if (sortBy === "timestamp") {
      return new Date(b.createdAt) - new Date(a.createdAt);
    } else if (sortBy === "severity") {
      const severityOrder = { "CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "WARNING": 4, "HIGH_RISK_PREDICTION": 5, "PREDICTED_RISK": 5 };
      return (severityOrder[a.severity] || 999) - (severityOrder[b.severity] || 999);
    } else if (sortBy === "metric") {
      return (a.metricName || "").localeCompare(b.metricName || "");
    }
    return 0;
  });

  const criticalCount = alerts.filter(a => a.severity?.toLowerCase().includes('critical') || a.severity?.toLowerCase().includes('high')).length;
  const warningCount = alerts.filter(a => a.severity?.toLowerCase().includes('warning') || a.severity?.toLowerCase().includes('predicted')).length;
  const severityBreakdown = {
    critical: alerts.filter(a => a.severity === "CRITICAL").length,
    high: alerts.filter(a => a.severity === "HIGH").length,
    warning: alerts.filter(a => a.severity === "WARNING").length,
    predicted: alerts.filter(a => a.severity?.includes("PREDICTED")).length,
    low: alerts.filter(a => a.severity === "LOW").length,
  };

  if (loading) {
    return (
      <div className="alerts-container">
        <div className="loading-skeleton">
          <div className="skeleton-header"></div>
          <div className="skeleton-cards">
            {[1, 2, 3].map(i => <div key={i} className="skeleton-card"></div>)}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="alerts-container error-state">
        <div className="error-box">
          <span className="error-icon">⚠️</span>
          <p>{error}</p>
          <button onClick={fetchAlerts} className="retry-btn">Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="alerts-container">
      <div className="alerts-header">
        <div className="alerts-title-section">
          <h1>System Alerts</h1>
          <p className="alerts-subtitle">Monitor and manage all system alerts</p>
        </div>
        <div className="alerts-stats">
          <div className="stat-item critical">
            <div className="stat-number">{criticalCount}</div>
            <div className="stat-label">Critical</div>
          </div>
          <div className="stat-item warning">
            <div className="stat-number">{warningCount}</div>
            <div className="stat-label">Warnings</div>
          </div>
          <div className="stat-item total">
            <div className="stat-number">{alerts.length}</div>
            <div className="stat-label">Total</div>
          </div>
        </div>
      </div>

      {/* Severity Breakdown Chart */}
      {alerts.length > 0 && (
        <div className="severity-breakdown">
          <h3>Alert Distribution</h3>
          <div className="breakdown-bars">
            {Object.entries(severityBreakdown).map(([level, count]) => (
              count > 0 && (
                <div key={level} className="breakdown-item">
                  <div className="breakdown-label">{level.charAt(0).toUpperCase() + level.slice(1)}</div>
                  <div className="breakdown-bar-container">
                    <div 
                      className={`breakdown-bar ${level}`}
                      style={{ width: `${(count / alerts.length) * 100}%` }}
                    >
                      <span className="breakdown-count">{count}</span>
                    </div>
                  </div>
                </div>
              )
            ))}
          </div>
        </div>
      )}

      <div className="alerts-filter-section">
        <div className="filter-controls">
          <div className="filter-buttons">
            {['all', 'critical', 'high', 'warning', 'low', 'predicted_risk'].map(severity => (
              <button
                key={severity}
                className={`filter-btn ${filterSeverity === severity ? 'active' : ''}`}
                onClick={() => setFilterSeverity(severity)}
              >
                {severity.charAt(0).toUpperCase() + severity.slice(1).replace('_', ' ')}
              </button>
            ))}
          </div>
          
          <div className="view-controls">
            <div className="sort-dropdown">
              <label>Sort by:</label>
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
                <option value="timestamp">Latest First</option>
                <option value="severity">Severity</option>
                <option value="metric">Metric Name</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {filteredAlerts.length === 0 ? (
        <div className="no-alerts-state">
          <div className="no-alerts-icon">🎉</div>
          <h3>No {filterSeverity === 'all' ? '' : filterSeverity} Alerts</h3>
          <p>All systems are operating normally</p>
        </div>
      ) : (
        <div className="alerts-table-container">
          <table className="alerts-table">
            <thead>
              <tr>
                <th>Metric</th>
                <th>Severity</th>
                <th>Current Value</th>
                <th>Threshold</th>
                <th>Time</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {sortedAlerts.map((alert) => (
                <tr key={alert.id} className={`severity-${alert.severity?.toLowerCase()}`}>
                  <td className="metric-cell">
                    <span className="metric-name">{alert.metricName}</span>
                  </td>
                  <td>
                    <span className={`severity-badge ${alert.severity?.toLowerCase()}`}>
                      {alert.severity}
                    </span>
                  </td>
                  <td className="value-cell">{alert.currentValue?.toFixed(2)}</td>
                  <td className="threshold-cell">{alert.threshold?.toFixed(2)}</td>
                  <td className="time-cell">
                    {new Date(alert.createdAt).toLocaleString()}
                  </td>
                  <td>
                    <span className={`status-badge ${alert.status?.toLowerCase()}`}>
                      {alert.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
