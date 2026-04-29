import React, { useState, useEffect } from 'react';
import api from '../api/api';
import RiskGauge from '../components/RiskGauge';
import TrendChart from '../components/TrendChart';
import AlertCard from '../components/AlertCard';
import { useToast } from '../components/ToastProvider';
import '../styles/AdminSla.css';

export default function AdminSla() {
  const [slas, setSlas] = useState([]);
  const [riskScores, setRiskScores] = useState({});
  const [alerts, setAlerts] = useState([]);
  const [metrics, setMetrics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingSla, setUpdatingSla] = useState(null);
  const [togglingSla, setTogglingSla] = useState(null);
  const [error, setError] = useState(null);
  const [editingSla, setEditingSla] = useState(null);
  const [editThreshold, setEditThreshold] = useState("");
  const [resolvingAlert, setResolvingAlert] = useState(null);
  const [resolvingAll, setResolvingAll] = useState(false);

  // SLA Creation state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newSlaMetric, setNewSlaMetric] = useState("");
  const [newSlaThreshold, setNewSlaThreshold] = useState("");
  const [creatingSla, setCreatingSla] = useState(false);

  const { showSuccess, showError, showInfo } = useToast();

  // Map display names to actual metric names
  const metricMapping = {
    "Response Time": "response_time",
    "P95 Latency": "p95_latency",
    "API Error Rate": "error_rate",
    "CPU Usage": "cpu_usage",
    "Memory Usage": "memory_usage",
    "Network Latency": "network_latency",
    "Disk Usage": "disk_usage"
  };

  // Reverse mapping for backend to display names
  const reverseMetricMapping = Object.fromEntries(
    Object.entries(metricMapping).map(([display, backend]) => [backend, display])
  );

  const getDisplayName = (backendName) => {
    return reverseMetricMapping[backendName] || backendName?.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()) || 'Unknown';
  };

  const getPredictionSuggestion = (metricName) => {
    const suggestions = {
      'cpu_usage': 'Consider scaling CPU resources or optimizing heavy processes',
      'memory_usage': 'Monitor for memory leaks and consider increasing RAM allocation',
      'disk_usage': 'Free up disk space or expand storage capacity',
      'network_latency': 'Check network connectivity and optimize routing',
      'response_time': 'Optimize database queries and consider caching strategies',
      'error_rate': 'Review error logs and fix failing components',
      'api_error_rate': 'Check API endpoints and fix failing services',
      'api_success_rate': 'Monitor API health and ensure proper error handling',
      'api_throughput': 'Consider load balancing or scaling API servers',
      'requests_per_min': 'Prepare for increased traffic or implement rate limiting',
      'uptime': 'Check service health and ensure proper monitoring'
    };
    return suggestions[metricName] || 'Monitor this metric closely and take preventive action';
  };

  useEffect(() => {
    const loadData = async () => {
      await Promise.all([fetchSlas(), fetchAlerts(), fetchRiskScores(), fetchMetrics()]);
      setLoading(false);
    };
    loadData();

    // Auto-refresh every 30 seconds
    const interval = setInterval(() => {
      fetchRiskScores();
      fetchAlerts();
      fetchMetrics();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  const fetchSlas = async () => {
    try {
      const res = await api.get("/api/sla");
      setSlas(res.data);
      setError(null); // Clear any previous errors
    } catch (err) {
      console.error("Failed to fetch SLAs:", err);
      setError("Failed to load SLA configurations. Please try again.");
    }
  };

  const fetchRiskScores = async () => {
    try {
      const res = await api.get("/api/risk/scores");
      const riskMap = {};
      res.data.forEach(risk => {
        riskMap[risk.serviceName] = risk;
      });
      setRiskScores(riskMap);
    } catch (err) {
      console.error("Failed to fetch risk scores:", err);
      // Don't set main error for risk scores as they're secondary data
    }
  };

  const fetchAlerts = async () => {
    try {
      const res = await api.get("/api/alerts");
      // Deduplicate alerts by ID
      const uniqueAlerts = res.data.content ? res.data.content.filter((alert, index, self) =>
        index === self.findIndex((a) => a.id === alert.id)
      ) : [];
      setAlerts(uniqueAlerts);
    } catch (err) {
      console.error("Failed to fetch alerts:", err);
      // Don't set main error for alerts as they're secondary data
    }
  };

  const fetchMetrics = async () => {
    try {
      const res = await api.get("/api/metrics");
      setMetrics(res.data);
    } catch (err) {
      console.error("Failed to fetch metrics:", err);
      // Don't set main error for metrics as they're secondary data
    }
  };

  const updateSla = async (id) => {
    if (!editThreshold) {
      showError("Please enter a threshold value");
      return;
    }

    setUpdatingSla(id);
    try {
      await api.put(`/api/sla/${id}`, {
        metricname: editingSla.metricname,
        threshold: parseFloat(editThreshold),
      });
      setEditingSla(null);
      setEditThreshold("");
      fetchSlas();
      showSuccess("SLA threshold updated successfully!");
    } catch (err) {
      console.error("Failed to update SLA:", err);
      showError("Failed to update SLA. Please try again.");
    } finally {
      setUpdatingSla(null);
    }
  };

  const toggleSlaStatus = async (id) => {
    setTogglingSla(id);
    try {
      await api.patch(`/api/sla/${id}/toggle`);
      fetchSlas();
      showSuccess("SLA status updated successfully!");
    } catch (err) {
      console.error("Failed to toggle SLA status:", err);
      showError("Failed to toggle SLA status. Please try again.");
    } finally {
      setTogglingSla(null);
    }
  };

  const deleteSla = async (id) => {
    if (!window.confirm('Are you sure you want to delete this SLA? This action cannot be undone.')) {
      return;
    }

    setTogglingSla(id); // Reuse the loading state
    try {
      await api.delete(`/api/sla/${id}`);
      fetchSlas();
      showSuccess("SLA deleted successfully!");
    } catch (err) {
      console.error("Failed to delete SLA:", err);
      showError("Failed to delete SLA. Please try again.");
    } finally {
      setTogglingSla(null);
    }
  };

  const resolveAlert = async (alertId) => {
    setResolvingAlert(alertId);
    try {
      await api.put(`/api/alerts/${alertId}/resolve`);
      fetchAlerts();
      showSuccess("Alert resolved successfully!");
    } catch (err) {
      console.error("Failed to resolve alert:", err);
      showError("Failed to resolve alert. Please try again.");
    } finally {
      setResolvingAlert(null);
    }
  };

  const resolveAllAlerts = async () => {
    if (!window.confirm('Are you sure you want to resolve ALL active alerts?')) {
      return;
    }
    setResolvingAll(true);
    try {
      await api.put('/api/alerts/resolve-all');
      fetchAlerts();
      showSuccess("All alerts resolved successfully!");
    } catch (err) {
      console.error("Failed to resolve all alerts:", err);
      showError("Failed to resolve all alerts. Please try again.");
    } finally {
      setResolvingAll(false);
    }
  };

  const createSla = async () => {
    if (!newSlaMetric || !newSlaThreshold) {
      showError("Please enter both metric name and threshold value");
      return;
    }

    const actualMetricName = metricMapping[newSlaMetric] || newSlaMetric;

    setCreatingSla(true);
    try {
      await api.post("/api/sla", {
        metricname: actualMetricName,
        threshold: parseFloat(newSlaThreshold),
      });
      setShowCreateForm(false);
      setNewSlaMetric("");
      setNewSlaThreshold("");
      fetchSlas();
      showSuccess("SLA created successfully!");
    } catch (err) {
      console.error("Failed to create SLA:", err);
      showError("Failed to create SLA. Please try again.");
    } finally {
      setCreatingSla(false);
    }
  };

  const getSeverityColor = (severity) => {
    switch (severity?.toUpperCase()) {
      case "HIGH":
        return "linear-gradient(135deg, #ff6b6b, #ee5a24)";
      case "MEDIUM":
        return "linear-gradient(135deg, #ffd93d, #ffb142)";
      case "LOW":
        return "linear-gradient(135deg, #6bcf7f, #4ecdc4)";
      default:
        return "linear-gradient(135deg, #a8a8a8, #6c757d)";
    }
  };

  const getRiskScore = (serviceName) => {
    const risk = riskScores[serviceName];
    return risk ? risk.predictedRisk.toFixed(1) : "N/A";
  };

  const getSeverityLevel = (serviceName) => {
    const risk = riskScores[serviceName];
    return risk ? risk.severityLevel : "Unknown";
  };

  const getTrendData = (serviceName) => {
    return metrics
      .filter(metric => metric.metricName === serviceName)
      .slice(-10)
      .map(metric => ({
        value: metric.value,
        timestamp: metric.dateTime
      }));
  };

  // Get unique alerts by metric (latest alert per metric)
  const getUniqueAlerts = () => {
    const alertMap = new Map();
    alerts.forEach(alert => {
      const metricKey = alert.metricName || 'unknown';
      if (!alertMap.has(metricKey) ||
        new Date(alert.createdAt) > new Date(alertMap.get(metricKey).createdAt)) {
        alertMap.set(metricKey, {
          ...alert,
          displayName: getDisplayName(alert.metricName)
        });
      }
    });
    return Array.from(alertMap.values());
  };

  const criticalCount = alerts.filter(a => a.severity === "CRITICAL").length;
  const predictedCount = alerts.filter(a => a.severity === "PREDICTED_RISK").length;

  return (
    <div className="admin-sla-container">
      <div className="admin-header">
        <h1>🔧 SLA Administration</h1>
        <p className="subtitle">Manage Service Level Agreements & Monitor Risk Scores</p>
      </div>

      {/* Error State */}
      {error && (
        <div className="error-state">
          <div className="error-icon">⚠️</div>
          <div className="error-message">Connection Error</div>
          <div className="error-description">{error}</div>
          <button className="retry-btn" onClick={() => {
            setError(null);
            setLoading(true);
            const loadData = async () => {
              await Promise.all([fetchSlas(), fetchAlerts(), fetchRiskScores(), fetchMetrics()]);
              setLoading(false);
            };
            loadData();
          }}>
            🔄 Retry
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading && !error && (
        <div className="loading-overlay">
          <div className="loading-spinner"></div>
          <p className="loading-text">Loading SLA configurations...</p>
        </div>
      )}

      {/* Main Content */}
      {!loading && !error && (
        <div className="admin-content">
          {/* KPI Cards */}
          <div className="kpi-section">
            <div className="kpi-card glassmorphic">
              <div className="kpi-icon">📊</div>
              <div className="kpi-content">
                <h3>Total SLAs</h3>
                <p className="kpi-value">{slas.length}</p>
              </div>
            </div>

            <div className="kpi-card glassmorphic critical">
              <div className="kpi-icon">🚨</div>
              <div className="kpi-content">
                <h3>Critical Alerts</h3>
                <p className="kpi-value">{criticalCount}</p>
              </div>
            </div>

            <div className="kpi-card glassmorphic predicted">
              <div className="kpi-icon">🤖</div>
              <div className="kpi-content">
                <h3>AI Predictions</h3>
                <p className="kpi-value">{predictedCount}</p>
              </div>
            </div>
          </div>

          {/* SLA Cards Grid */}
          <div className="sla-section">
            <div className="sla-section-header">
              <h2 className="section-title">Service Level Agreements</h2>
              <button
                className="btn-primary create-sla-btn"
                onClick={() => setShowCreateForm(true)}
              >
                ➕ Create SLA
              </button>
            </div>
            <div className="sla-grid">
              {slas.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">📋</div>
                  <div className="empty-title">No SLA Configurations</div>
                  <div className="empty-description">
                    Get started by creating your first Service Level Agreement to monitor system performance and receive intelligent alerts.
                  </div>
                  <button
                    className="btn-primary"
                    onClick={() => setShowCreateForm(true)}
                  >
                    ➕ Create First SLA
                  </button>
                </div>
              ) : (
                slas.map((sla) => (
                  <div key={sla.id} className={`sla-card glassmorphic ${!sla.active ? 'inactive' : ''}`}>
                    <div className="sla-header">
                      <h3 className="metric-name">{getDisplayName(sla.metricname)}</h3>
                      <div
                        className="severity-badge"
                        style={{ background: getSeverityColor(getSeverityLevel(sla.metricname)) }}
                      >
                        {getSeverityLevel(sla.metricname) || 'Unknown'}
                      </div>
                    </div>

                    <div className="sla-body">
                      {editingSla?.id === sla.id ? (
                        <div className="edit-form">
                          <input
                            type="number"
                            value={editThreshold}
                            onChange={(e) => setEditThreshold(e.target.value)}
                            placeholder="New threshold"
                            className="edit-input"
                          />
                          <div className="edit-actions">
                            <button
                              className="save-btn"
                              onClick={() => updateSla(sla.id)}
                              disabled={updatingSla === sla.id}
                            >
                              {updatingSla === sla.id ? (
                                <>
                                  <div className="btn-spinner"></div>
                                  Saving...
                                </>
                              ) : (
                                '✓ Save'
                              )}
                            </button>
                            <button
                              className="cancel-btn"
                              onClick={() => {
                                setEditingSla(null);
                                setEditThreshold("");
                              }}
                            >
                              ✕ Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="sla-info">
                          <div className="threshold-info">
                            <span className="label">Threshold:</span>
                            <span className="value">{sla.threshold}</span>
                          </div>
                          <div className="risk-info">
                            <span className="label">Risk Score:</span>
                            <span className="value">{getRiskScore(sla.metricname)}%</span>
                          </div>
                          <div className="status-info">
                            <span className="label">Status:</span>
                            <span className={`status ${sla.active ? 'active' : 'inactive'}`}>
                              {sla.active ? '● ACTIVE' : '● INACTIVE'}
                            </span>
                          </div>

                          {/* Risk Visualization Section */}
                          <div className="risk-visualization">
                            <div className="visualization-row">
                              <RiskGauge
                                riskScore={parseFloat(getRiskScore(sla.metricname)) || 0}
                                serviceName={getDisplayName(sla.metricname).toLowerCase()}
                                showLabel={false}
                              />
                              <TrendChart
                                data={getTrendData(sla.metricname)}
                                width={120}
                                height={60}
                                color={getSeverityColor(getSeverityLevel(sla.metricname))}
                              />
                            </div>
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="sla-actions">
                      <button
                        className="action-btn edit"
                        onClick={() => {
                          setEditingSla(sla);
                          setEditThreshold(sla.threshold.toString());
                        }}
                        disabled={editingSla !== null}
                      >
                        ✏️ Edit
                      </button>
                      <button
                        className={`action-btn toggle ${sla.active ? 'disable' : 'enable'}`}
                        onClick={() => toggleSlaStatus(sla.id)}
                      >
                        {sla.active ? '🔴 Disable' : '🟢 Enable'}
                      </button>
                      <button
                        className="action-btn delete"
                        onClick={() => deleteSla(sla.id)}
                        disabled={togglingSla === sla.id}
                      >
                        {togglingSla === sla.id ? (
                          <div className="btn-spinner"></div>
                        ) : (
                          '🗑️ Delete'
                        )}
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

          </div>

          {/* Recent Alerts Section */}
          <div className="alerts-section">
            <div className="alerts-section-header">
              <h2>Recent Alerts</h2>
              {alerts.length > 0 && (
                <button
                  className="btn-resolve-all"
                  onClick={resolveAllAlerts}
                  disabled={resolvingAll}
                >
                  {resolvingAll ? (
                    <>
                      <div className="btn-spinner"></div>
                      Resolving...
                    </>
                  ) : (
                    '✅ Resolve All'
                  )}
                </button>
              )}
            </div>
            {alerts.length === 0 ? (
              <div className="no-alerts glassmorphic">
                <p>🎉 No active alerts! All systems operational.</p>
              </div>
            ) : (
              <div className="alerts-grid">
                {alerts.slice(0, 6).map((alert) => {
                  // Calculate time string
                  const alertTime = new Date(alert.createdAt || alert.timestamp);
                  const now = new Date();
                  const diffMins = Math.floor((now - alertTime) / 60000);
                  const timeStr = diffMins === 0 ? 'Just now' : `${diffMins}m ago`;

                  // Get severity styling
                  const getSeverityColor = (sev) => {
                    const sevLower = sev?.toLowerCase() || 'unknown';
                    switch (sevLower) {
                      case 'predicted_risk':
                        return { bg: 'linear-gradient(135deg, #e3f2fd, #bbdefb)', border: '#90caf9', textColor: '#1565c0' };
                      case 'critical':
                        return { bg: 'linear-gradient(135deg, #ffebee, #ffcdd2)', border: '#ef9a9a', textColor: '#c62828' };
                      case 'warning':
                        return { bg: 'linear-gradient(135deg, #fff3e0, #ffe0b2)', border: '#ffcc80', textColor: '#e65100' };
                      default:
                        return { bg: 'linear-gradient(135deg, #e3f2fd, #bbdefb)', border: '#90caf9', textColor: '#0d47a1' };
                    }
                  };

                  const getSeverityIcon = (sev) => {
                    const sevLower = sev?.toLowerCase() || 'unknown';
                    switch (sevLower) {
                      case 'predicted_risk': return '🔮';
                      case 'critical': return '🚨';
                      case 'warning': return '⚠️';
                      default: return '📌';
                    }
                  };

                  const colors = getSeverityColor(alert.severity);
                  const isPredictedRisk = alert.severity === 'PREDICTED_RISK';

                  return (
                    <div key={alert.id} className={`alert-card severity-${alert.severity?.toLowerCase().replace('_', '-')}`} style={{ background: colors.bg, borderColor: colors.border }}>
                      <div className="alert-header">
                        <div className="alert-meta">
                          <span className="severity-icon">{getSeverityIcon(alert.severity)}</span>
                          <div className="alert-title-group">
                            <h3 className="alert-metric" style={{ color: colors.textColor }}>{getDisplayName(alert.metricName)}</h3>
                            <span className="alert-time" style={{ color: colors.textColor }}>{timeStr}</span>
                          </div>
                        </div>
                        <span className={`severity-badge`} style={{ color: colors.textColor }}>
                          {alert.severity}
                        </span>
                      </div>

                      <div className="alert-divider" style={{ borderColor: colors.border }}></div>

                      <div className="alert-body">
                        {isPredictedRisk ? (
                          <div className="prediction-details">
                            <div className="prediction-row">
                              <span className="prediction-label">📊 Current Value:</span>
                              <span className="prediction-value current">{alert.actualCurrentValue?.toFixed(2) || alert.currentValue?.toFixed(2) || 'N/A'}</span>
                            </div>
                            <div className="prediction-row">
                              <span className="prediction-label">🔮 Predicted Value:</span>
                              <span className="prediction-value predicted">{alert.actualValue?.toFixed(2) || alert.currentValue?.toFixed(2) || 'N/A'}</span>
                            </div>
                            <div className="prediction-row">
                              <span className="prediction-label">🎯 Threshold:</span>
                              <span className="prediction-value">{alert.threshold || 'N/A'}</span>
                            </div>
                            <div className="prediction-row">
                              {(alert.actualValue || alert.currentValue) >= alert.threshold ? (
                                <>
                                  <span className="prediction-label">📈 Exceeds By:</span>
                                  <span className="prediction-value exceeds">
                                    {`+${((alert.actualValue || alert.currentValue) - alert.threshold).toFixed(2)} (${(((alert.actualValue || alert.currentValue) / alert.threshold - 1) * 100).toFixed(1)}%)`}
                                  </span>
                                </>
                              ) : (
                                <>
                                  <span className="prediction-label">📉 Gap to Threshold:</span>
                                  <span className="prediction-value" style={{ color: '#2ecc71', fontWeight: 'bold' }}>
                                    {`${((alert.actualValue || alert.currentValue) - alert.threshold).toFixed(2)} (${(((alert.actualValue || alert.currentValue) / alert.threshold - 1) * 100).toFixed(1)}%)`}
                                  </span>
                                </>
                              )}
                            </div>
                            <div className="prediction-info">
                              <span className="info-icon">💡</span>
                              <span>{getPredictionSuggestion(alert.metricName)}</span>
                            </div>
                          </div>
                        ) : (
                          <div className="alert-row">
                            <div className="alert-stat">
                              <span className="stat-label" style={{ color: colors.textColor }}>Current Value</span>
                              <span className="stat-value" style={{ color: colors.textColor }}>{(alert.actualCurrentValue || alert.currentValue || alert.actualValue || 'N/A')?.toFixed ? (alert.actualCurrentValue || alert.currentValue || alert.actualValue)?.toFixed(2) : (alert.actualCurrentValue || alert.currentValue || alert.actualValue || 'N/A')}</span>
                            </div>
                            <div className="alert-stat">
                              <span className="stat-label" style={{ color: colors.textColor }}>Threshold</span>
                              <span className="stat-value" style={{ color: colors.textColor }}>{alert.threshold || 'N/A'}</span>
                            </div>
                          </div>
                        )}
                      </div>

                      <div className="alert-footer">
                        <span className="alert-timestamp" style={{ color: colors.textColor }}>{alertTime.toLocaleString()}</span>
                        {(alert.status === 'ACTIVE' || alert.status === 'ACKNOWLEDGED' || !alert.status) && (
                          <button
                            className="btn-resolve"
                            onClick={() => resolveAlert(alert.id)}
                            disabled={resolvingAlert === alert.id}
                            style={{ backgroundColor: colors.textColor }}
                          >
                            {resolvingAlert === alert.id ? (
                              <div className="btn-spinner"></div>
                            ) : (
                              '✅ Resolve'
                            )}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* SLA Creation Modal */}
      {showCreateForm && (
        <div className="modal-overlay" onClick={() => setShowCreateForm(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create New SLA</h3>
              <button
                className="modal-close"
                onClick={() => setShowCreateForm(false)}
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label htmlFor="metricName">Metric Name</label>
                <select
                  id="metricName"
                  value={newSlaMetric}
                  onChange={(e) => setNewSlaMetric(e.target.value)}
                  className="form-input"
                  required
                >
                  <option value="">Select a metric...</option>
                  <optgroup label="Performance Metrics">
                    <option value="Response Time">Response Time (ms)</option>
                    <option value="P95 Latency">P95 Latency (ms)</option>
                    <option value="API Error Rate">API Error Rate (%)</option>
                  </optgroup>
                  <optgroup label="System Metrics">
                    <option value="CPU Usage">CPU Usage (%)</option>
                    <option value="Memory Usage">Memory Usage (MB)</option>
                    <option value="Network Latency">Network Latency (ms)</option>
                    <option value="Disk Usage">Disk Usage (%)</option>
                  </optgroup>
                </select>
                <small className="form-hint">
                  Select from metrics displayed in the user dashboard
                </small>
              </div>
              <div className="form-group">
                <label htmlFor="threshold">Threshold Value</label>
                <input
                  id="threshold"
                  type="number"
                  value={newSlaThreshold}
                  onChange={(e) => setNewSlaThreshold(e.target.value)}
                  placeholder="e.g., 80, 90, 500"
                  className="form-input"
                  step="0.1"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn-secondary"
                onClick={() => setShowCreateForm(false)}
              >
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={createSla}
                disabled={creatingSla}
              >
                {creatingSla ? (
                  <>
                    <div className="btn-spinner"></div>
                    Creating...
                  </>
                ) : (
                  'Create SLA'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
