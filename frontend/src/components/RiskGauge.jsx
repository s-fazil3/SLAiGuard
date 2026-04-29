import React from 'react';
import './RiskGauge.css';

const RiskGauge = ({ riskScore, serviceName, showLabel = true }) => {
  const getRiskColor = (score) => {
    if (score <= 40) return '#6bcf7f'; // Green for Low
    if (score <= 70) return '#ffd93d'; // Yellow for Medium
    return '#ff6b6b'; // Red for High
  };

  const getRiskLevel = (score) => {
    if (score <= 40) return 'LOW';
    if (score <= 70) return 'MEDIUM';
    return 'HIGH';
  };

  const color = getRiskColor(riskScore);
  const riskLevel = getRiskLevel(riskScore);
  const percentage = Math.min(Math.max(riskScore, 0), 100);

  // Calculate the stroke dash array for the circular progress
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  return (
    <div className="risk-gauge-container">
      <div className="risk-gauge">
        <svg className="risk-gauge-svg" width="120" height="120" viewBox="0 0 120 120">
          {/* Background circle */}
          <circle
            cx="60"
            cy="60"
            r={radius}
            fill="none"
            stroke="rgba(255, 255, 255, 0.1)"
            strokeWidth="8"
          />
          {/* Progress circle */}
          <circle
            cx="60"
            cy="60"
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth="8"
            strokeDasharray={strokeDasharray}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            transform="rotate(-90 60 60)"
            className="risk-gauge-progress"
          />
          {/* Center content */}
          <text x="60" y="55" textAnchor="middle" className="risk-gauge-value">
            {percentage.toFixed(0)}%
          </text>
          <text x="60" y="70" textAnchor="middle" className="risk-gauge-label">
            Risk
          </text>
        </svg>
      </div>
      {showLabel && (
        <div className="risk-gauge-info">
          <div className="service-name">{serviceName}</div>
          <div className="risk-level" style={{ color }}>
            {riskLevel}
          </div>
        </div>
      )}
    </div>
  );
};

export default RiskGauge;
