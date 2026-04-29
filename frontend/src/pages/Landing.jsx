import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import "../styles/Landing.css";

export default function Landing() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    setIsVisible(true);
  }, []);

  const features = [
    {
      icon: "📊",
      title: "Real-Time Monitoring",
      description: "Track metrics continuously with live system insights and predictive analytics.",
      color: "var(--primary-gradient)"
    },
    {
      icon: "🔔",
      title: "Intelligent Alerts",
      description: "Context-aware alerts with clear severity levels and automated resolution.",
      color: "var(--warning-gradient)"
    },
    {
      icon: "🤖",
      title: "AI Risk Prediction",
      description: "Predict SLA breaches before they happen using advanced ML algorithms.",
      color: "var(--secondary-gradient)"
    },
    {
      icon: "📈",
      title: "SLA Analytics",
      description: "Analyze trends and SLA compliance with beautiful, interactive visualizations.",
      color: "var(--success-gradient)"
    },
    {
      icon: "🔐",
      title: "Secure Access Control",
      description: "Role-based permissions with JWT authentication and enterprise security.",
      color: "var(--cool-gradient)"
    },
    {
      icon: "⚡",
      title: "Built for Scale",
      description: "Enterprise-ready architecture designed for high-performance and growth.",
      color: "var(--warm-gradient)"
    }
  ];

  const stats = [
    { number: "99.9%", label: "Uptime SLA" },
    { number: "< 5min", label: "Alert Response" },
    { number: "24/7", label: "Monitoring" },
    { number: "AI-Powered", label: "Predictions" }
  ];

  return (
    <div className="landing-container">
      {/* Hero Section with Background */}
      <section className={`hero-section ${isVisible ? 'visible' : ''}`}>
        <div className="hero-background">
          <div className="hero-overlay"></div>
          <div className="floating-shapes">
            <div className="shape shape-1"></div>
            <div className="shape shape-2"></div>
            <div className="shape shape-3"></div>
          </div>
        </div>

        <div className="hero-content">
          <div className="hero-text">
            <h1 className="hero-title">
              <span className="text-gradient">SLAiGuard</span>
              <br />
              <span className="hero-subtitle">Intelligent SLA Monitoring</span>
            </h1>
            <p className="hero-description">
              A cutting-edge platform that combines real-time monitoring, SLA enforcement,
              and AI-powered prediction to keep your systems reliable, resilient, and scalable.
              Transform your infrastructure monitoring with predictive intelligence.
            </p>

            <div className="hero-stats">
              {stats.map((stat, index) => (
                <div key={index} className="stat-item">
                  <div className="stat-number">{stat.number}</div>
                  <div className="stat-label">{stat.label}</div>
                </div>
              ))}
            </div>

            <div className="hero-buttons">
              <Link to="/signup" className="btn-primary hero-cta">
                <span>Start Free Trial</span>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M5 12H19M19 12L12 5M19 12L12 19" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </Link>
              <div className="login-options">
                <Link to="/user/login" className="btn-secondary hero-login">
                  <span>User Login</span>
                </Link>
                <Link to="/admin/login" className="btn-secondary hero-login admin-login">
                  <span>Admin Login</span>
                </Link>
              </div>
            </div>
          </div>

          <div className="hero-visual">
            <div className="dashboard-preview">
              <div className="preview-header">
                <div className="preview-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div className="preview-title">SLAiGuard Dashboard</div>
              </div>
              <div className="preview-content">
                <div className="metric-cards">
                  <div className="metric-card cpu">
                    <div className="metric-icon">⚡</div>
                    <div className="metric-value">67%</div>
                    <div className="metric-label">CPU Usage</div>
                  </div>
                  <div className="metric-card memory">
                    <div className="metric-icon">�</div>
                    <div className="metric-value">4.2GB</div>
                    <div className="metric-label">Memory</div>
                  </div>
                  <div className="metric-card network">
                    <div className="metric-icon">🌐</div>
                    <div className="metric-value">98%</div>
                    <div className="metric-label">Network</div>
                  </div>
                </div>
                <div className="chart-preview">
                  <div className="chart-line"></div>
                  <div className="chart-bars">
                    <div className="bar" style={{ height: '40%' }}></div>
                    <div className="bar" style={{ height: '60%' }}></div>
                    <div className="bar" style={{ height: '80%' }}></div>
                    <div className="bar" style={{ height: '55%' }}></div>
                    <div className="bar" style={{ height: '75%' }}></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="features-container">
          <div className="features-header">
            <h2 className="section-title">
              Powerful Features for
              Modern Infrastructure
            </h2>
            <p className="section-description">
              Everything you need to monitor, analyze, and optimize your system's performance
              with enterprise-grade reliability and AI-powered insights.
            </p>
          </div>

          <div className="features-grid">
            {features.map((feature, index) => (
              <div key={index} className="feature-card glass-card" style={{ '--delay': `${index * 0.1}s` }}>
                <div className="feature-icon" style={{ background: feature.color }}>
                  <span>{feature.icon}</span>
                </div>
                <h3 className="feature-title">{feature.title}</h3>
                <p className="feature-description">{feature.description}</p>
                <div className="feature-hover-effect"></div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="cta-container">
          <div className="cta-content">
            <h2>Ready to Transform Your Monitoring?</h2>
            <p>Join thousands of organizations that trust SLAiGuard for their critical infrastructure monitoring.</p>
            <div className="cta-buttons">
              <Link to="/signup" className="btn-primary">
                Get Started Today
              </Link>
              <Link to="/login" className="btn-secondary">
                View Demo Dashboard
              </Link>
            </div>
          </div>
          <div className="cta-visual">
            <div className="success-metrics">
              <div className="metric">
                <div className="metric-number">500+</div>
                <div className="metric-text">Active Systems</div>
              </div>
              <div className="metric">
                <div className="metric-number">99.9%</div>
                <div className="metric-text">Uptime Achieved</div>
              </div>
              <div className="metric">
                <div className="metric-number">24/7</div>
                <div className="metric-text">Monitoring</div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
