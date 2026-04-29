# SLA Predictive Monitoring Platform 🚀

![SLA Predictive Monitoring Platform](https://img.shields.io/badge/Status-Active-brightgreen)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-green)
![React](https://img.shields.io/badge/React-18-blue)
![Flask](https://img.shields.io/badge/Flask-3.0-lightgrey)
![Machine Learning](https://img.shields.io/badge/SciKit--Learn-Random_Forest-orange)

A production-grade, full-stack Service Level Agreement (SLA) monitoring system featuring real-time data ingestion and **machine learning-powered breach prediction**.

Instead of waiting for an SLA to be violated (which impacts users and costs business revenue), this system predicts when a metric will breach its threshold in the near future and proactively warns the administration team.

---

## 🏆 Key Features

- **Real-Time Telemetry Dashboard:** Monitor 8 critical system metrics, including CPU, Memory, Latency (p95), Request Count, and Network I/O.
- **Predictive ML Engine:** Multi-output Random Forest regressor forecasts next-tick metric values.
- **Dynamic SLA Alerts:** Configurable SLA rules trigger dynamic alerts (`INFO`, `WARNING`, `CRITICAL`) based on threshold proximity and predicted breaches. 
- **Role-Based Access Control (RBAC):** Secure JWT authentication dividing responsibilities among `ADMIN` and `USER` tiers.
- **Stateless Z-Score Anomaly Detection:** Instantly flags spontaneous outliers outside of normal operations.

---

## 🏗️ Architecture Stack

### Backend - *Spring Boot (Java 21)*
- REST API layer
- JWT based stateless authentication
- MySQL persistence
- **Live Telemetry Simulation Engine:** Intentionally generated load testing to stress-test SLAs and trigger prediction scenarios for demonstration purposes locally without requiring DDoS attacks on your network.

### Machine Learning Service - *Flask (Python 3.10+)*
- Scikit-Learn **Random Forest Regression**
- Deployed behind a Waitress WSGI production server
- High-performance `scaler` and pre-trained binary models (Git LFS)

### Frontend - *React.js*
- Interactive SPA (Single Page Application) Dashboards
- Context-based Auth State
- Real-time charting representations

---

## 🚀 Quick Setup & Deployment

The easiest way to run the platform locally or on a server is using Docker Compose.

### Prerequisites
- Docker Engine & Docker Compose
- Node.js 18+ (for local frontend dev)
- Java 21 (for local backend dev)
- Python 3.10+ (for local ML dev)

### 🐳 Running with Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/SLA.git
   cd SLA
   ```

2. **Run via Docker Compose**
   ```bash
   docker-compose up --build
   ```

   This orchestrates:
   - MySQL Database (Port 3306)
   - Spring Boot Backend (Port 8080)
   - Python ML Service (Port 5001)
   - React Frontend (Port 3000)

3. **Access the Application**
   - Frontend: `http://localhost:3000`
   - Backend API Docs (Swagger): `http://localhost:8080/swagger-ui/index.html`
   - ML Health Check: `http://localhost:5001/health`

### 💻 Local Development Setup

If you wish to run the services bare-metal without Docker, follow these steps.

**1. ML Service (Port 5001)**
```bash
cd ml-service
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install -r requirements.txt
python app.py
```
*(Note: Be sure you have downloaded the ML model `.pkl` files and `metrics.csv` from the repository releases since they are excluded from Git.)*

**2. Backend (Port 8080)**
```bash
cd backend/backend
# Make sure MySQL is running locally with credentials in application.properties
./mvnw clean install
./mvnw spring-boot:run
```

**3. Frontend (Port 3000)**
```bash
cd frontend
cp .env.example .env  # Edit as necessary
npm install
npm start
```

---

## 📈 ML Model Performance (Random Forest Regressor)

The model is trained on a synthetic load dataset. Sample validation metrics across 8 output targets:
*(Run `evaluate_model.py` to recreate testing benchmarks)*

* **CPU Usage RMSE**: ~1.32% | R2: 0.94
* **Memory Usage RMSE**: ~18.5 MB | R2: 0.92
* **p95 Latency RMSE**: ~12.1 ms | R2: 0.96

*(The exact dataset and `scaler.pkl` vary by deployment environment.)*

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

*This project was developed to showcase an end-to-end production pipeline utilizing AI integration, resilient backend architectures, and modern dashboard UX.*
