import React from 'react';
import './TrendChart.css';

const TrendChart = ({ data, width = 200, height = 80, color = '#00d4ff' }) => {
  if (!data || data.length < 2) {
    return (
      <div className="trend-chart-placeholder" style={{ width, height }}>
        <span>No trend available</span>
      </div>
    );
  }

  // Calculate chart dimensions with padding
  const padding = 10;
  const chartWidth = width - (padding * 2);
  const chartHeight = height - (padding * 2);

  // Find min and max values
  const values = data.map(point => point.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const valueRange = maxValue - minValue || 1;

  // Create path for the line
  const pathData = data.map((point, index) => {
    const x = padding + (index / (data.length - 1)) * chartWidth;
    const y = padding + chartHeight - ((point.value - minValue) / valueRange) * chartHeight;
    return `${index === 0 ? 'M' : 'L'} ${x} ${y}`;
  }).join(' ');

  // Create gradient fill area
  const areaPath = pathData + ` L ${padding + chartWidth} ${padding + chartHeight} L ${padding} ${padding + chartHeight} Z`;

  // Calculate trend direction
  const firstValue = data[0].value;
  const lastValue = data[data.length - 1].value;
  const trend = lastValue > firstValue ? 'up' : lastValue < firstValue ? 'down' : 'flat';
  const trendColor = trend === 'up' ? '#ff6b6b' : trend === 'down' ? '#6bcf7f' : '#a0a0a0';

  return (
    <div className="trend-chart-container">
      <svg width={width} height={height} className="trend-chart">
        {/* Gradient definition */}
        <defs>
          <linearGradient id={`gradient-${color.replace('#', '')}`} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor={color} stopOpacity="0.3" />
            <stop offset="100%" stopColor={color} stopOpacity="0.05" />
          </linearGradient>
        </defs>

        {/* Background grid */}
        <rect
          x={padding}
          y={padding}
          width={chartWidth}
          height={chartHeight}
          fill="rgba(255, 255, 255, 0.02)"
          rx="4"
        />

        {/* Area fill */}
        <path
          d={areaPath}
          fill={`url(#gradient-${color.replace('#', '')})`}
          className="trend-chart-area"
        />

        {/* Trend line */}
        <path
          d={pathData}
          fill="none"
          stroke={color}
          strokeWidth="2"
          className="trend-chart-line"
        />

        {/* Data points */}
        {data.map((point, index) => {
          const x = padding + (index / (data.length - 1)) * chartWidth;
          const y = padding + chartHeight - ((point.value - minValue) / valueRange) * chartHeight;
          return (
            <circle
              key={index}
              cx={x}
              cy={y}
              r="3"
              fill={color}
              className="trend-chart-point"
            />
          );
        })}
      </svg>

      {/* Trend indicator */}
      <div className="trend-indicator" style={{ color: trendColor }}>
        <span className="trend-arrow">
          {trend === 'up' ? '↗' : trend === 'down' ? '↘' : '→'}
        </span>
        <span className="trend-text">
          {trend === 'up' ? 'Rising' : trend === 'down' ? 'Falling' : 'Stable'}
        </span>
      </div>
    </div>
  );
};

export default TrendChart;
