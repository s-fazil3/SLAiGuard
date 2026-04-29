export default function AlertCard({ alert, onAcknowledge }) {
  const { metricName, currentValue, threshold, severity, createdAt, status } = alert;

  // Debug logging for PREDICTED_RISK alerts
  if (severity === 'PREDICTED_RISK') {
    console.log('AlertCard PREDICTED_RISK:', {
      metricName,
      currentValue,
      alertCurrentValue: alert.currentValue,
      alertActualCurrentValue: alert.actualCurrentValue,
      alertActualValue: alert.actualValue,
      threshold,
      severity
    });
  }

  // For PREDICTED_RISK alerts, use the actual current value from alert.actualCurrentValue
  const actualCurrentValue = severity === 'PREDICTED_RISK' ? alert.actualCurrentValue : currentValue;

  const getSeverityIcon = (sev) => {
    const sevLower = sev?.toLowerCase() || 'unknown';
    switch (sevLower) {
      case 'predicted_risk':
        return '🔮';
      case 'critical':
        return '🚨';
      case 'high':
        return '⚠️';
      case 'medium':
        return '⚡';
      case 'low':
        return 'ℹ️';
      default:
        return '📌';
    }
  };

  const getSeverityColor = (sev) => {
    const sevLower = sev?.toLowerCase() || 'unknown';
    switch (sevLower) {
      case 'predicted_risk':
        return { bg: 'linear-gradient(135deg, #e3f2fd, #bbdefb)', border: '#90caf9', textColor: '#1565c0' };
      case 'critical':
        return { bg: 'linear-gradient(135deg, #ffebee, #ffcdd2)', border: '#ef9a9a', textColor: '#c62828' };
      case 'high':
        return { bg: 'linear-gradient(135deg, #fff3e0, #ffe0b2)', border: '#ffcc80', textColor: '#e65100' };
      case 'medium':
        return { bg: 'linear-gradient(135deg, #fffde7, #fff9c4)', border: '#fff59d', textColor: '#f57f17' };
      case 'low':
        return { bg: 'linear-gradient(135deg, #e8f5e9, #c8e6c9)', border: '#a5d6a7', textColor: '#1b5e20' };
      default:
        return { bg: 'linear-gradient(135deg, #e3f2fd, #bbdefb)', border: '#90caf9', textColor: '#0d47a1' };
    }
  };

  const getPredictionSuggestion = (metricName) => {
    const suggestions = {
      'cpu_usage': 'Consider scaling CPU resources or optimizing heavy processes',
      'memory_usage': 'Monitor for memory leaks and consider increasing RAM allocation',
      'disk_usage': 'Free up disk space or expand storage capacity',
      'network_latency': 'Check network connectivity and optimize routing',
      'response_time': 'Optimize database queries and consider caching strategies',
      'p95_latency': 'Investigate tail latency spikes and optimize thread pool settings',
      'error_rate': 'Review error logs and fix failing components',
      'request_count': 'Monitor traffic spikes and check load balancer capacity',
      'api_error_rate': 'Check API endpoints and fix failing services',
      'api_success_rate': 'Monitor API health and ensure proper error handling',
      'api_throughput': 'Consider load balancing or scaling API servers',
      'requests_per_min': 'Prepare for increased traffic or implement rate limiting',
      'uptime': 'Check service health and ensure proper monitoring'
    };
    return suggestions[metricName] || 'Monitor this metric closely and take preventive action';
  };

  const colors = getSeverityColor(severity);
  const now = new Date();
  const alertTime = new Date(createdAt);
  const diffMs = now - alertTime;
  const diffMins = Math.floor(diffMs / 60000);
  const timeStr = diffMins === 0 ? 'Just now' : `${diffMins}m ago`;

  const isPredictedRisk = severity === 'PREDICTED_RISK';
  const canAcknowledge = status === 'ACTIVE' || !status;

  return (
    <div className={`alert-card severity-${severity?.toLowerCase().replace('_', '-')}`} style={{ background: colors.bg, borderColor: colors.border }}>
      <div className="alert-header">
        <div className="alert-meta">
          <span className="severity-icon">{getSeverityIcon(severity)}</span>
          <div className="alert-title-group">
            <h3 className="alert-metric" style={{ color: colors.textColor }}>{metricName || 'Unknown'}</h3>
            <span className="alert-time" style={{ color: colors.textColor }}>{timeStr}</span>
          </div>
        </div>
        <span className={`severity-badge`} style={{ color: colors.textColor }}>
          {severity}
        </span>
      </div>

      <div className="alert-divider" style={{ borderColor: colors.border }}></div>

      <div className="alert-body">
        {isPredictedRisk ? (
          <div className="prediction-details">
            <div className="prediction-row">
              <span className="prediction-label">📊 Current Value:</span>
              <span className="prediction-value current">{actualCurrentValue?.toFixed(2) || 'N/A'}</span>
            </div>
            <div className="prediction-row">
              <span className="prediction-label">🔮 Predicted Value:</span>
              <span className="prediction-value predicted">{currentValue?.toFixed(2) || 'N/A'}</span>
            </div>
            <div className="prediction-row">
              <span className="prediction-label">🎯 Threshold:</span>
              <span className="prediction-value">{threshold?.toFixed(2) || 'N/A'}</span>
            </div>
            <div className="prediction-row">
              {currentValue >= threshold ? (
                <>
                  <span className="prediction-label">📈 Exceeds By:</span>
                  <span className="prediction-value exceeds">
                    {`+${(currentValue - threshold).toFixed(2)} (${((currentValue / threshold - 1) * 100).toFixed(1)}%)`}
                  </span>
                </>
              ) : (
                <>
                  <span className="prediction-label">📉 Gap to Threshold:</span>
                  <span className="prediction-value" style={{ color: '#2ecc71', fontWeight: 'bold' }}>
                    {`${(currentValue - threshold).toFixed(2)} (${((currentValue / threshold - 1) * 100).toFixed(1)}%)`}
                  </span>
                </>
              )}
            </div>
          </div>
        ) : (
          <div className="alert-row">
            <div className="alert-stat">
              <span className="stat-label" style={{ color: colors.textColor }}>Current Value</span>
              <span className="stat-value" style={{ color: colors.textColor }}>{currentValue?.toFixed(2) ?? 'N/A'}</span>
            </div>
            <div className="alert-stat">
              <span className="stat-label" style={{ color: colors.textColor }}>Threshold</span>
              <span className="stat-value" style={{ color: colors.textColor }}>{threshold?.toFixed(2) ?? 'N/A'}</span>
            </div>
          </div>
        )}

        {/* AI Suggestion - Now shown for all alerts */}
        <div className="prediction-info" style={{ marginTop: '12px', opacity: 0.9 }}>
          <span className="info-icon">💡</span>
          <span style={{ fontSize: '0.85rem' }}>{getPredictionSuggestion(metricName)}</span>
        </div>

        {currentValue && threshold && !isPredictedRisk && (
          <div className="alert-progress" style={{ marginTop: '12px' }}>
            <div className="progress-bar">
              <div
                className="progress-fill"
                style={{
                  width: `${Math.min((currentValue / (threshold || 100)) * 100, 100)}%`,
                  backgroundColor: severity === 'critical' ? '#ff4757' :
                    severity === 'high' ? '#ffa502' :
                      severity === 'medium' ? '#ffd93d' :
                        severity === 'low' ? '#6bcf7f' :
                          '#5f9ea0'
                }}
              ></div>
            </div>
          </div>
        )}
      </div>

      <div className="alert-footer">
        <span className="alert-timestamp" style={{ color: colors.textColor }}>{new Date(createdAt).toLocaleString()}</span>
        {canAcknowledge && onAcknowledge && (
          <button
            className="btn-acknowledge"
            onClick={() => onAcknowledge(alert.id)}
            style={{ backgroundColor: colors.textColor, color: 'white' }}
          >
            👁️ Acknowledge
          </button>
        )}
      </div>
    </div>
  );
}
