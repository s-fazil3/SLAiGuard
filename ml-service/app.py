"""
app.py  –  ML Prediction Service (Flask)
=========================================
Predicts the FUTURE value of each metric using a multi-output regression
model.  The Spring Boot backend compares those predicted values against the
active SLA thresholds and raises a PREDICTED_RISK alert if any metric is
forecast to breach its limit — before it actually happens.

Endpoints
---------
  GET  /health          Liveness probe
  POST /predict         Predict next metric values + breach risk
  POST /anomaly         Stateless Z-score anomaly detector (unchanged)
"""

from flask import Flask, request, jsonify
import numpy as np
import pickle
import logging
import sys
import os

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

app          = Flask(__name__)
model        = None
scaler       = None
feature_cols = None
target_cols  = None


# ── Model loader ──────────────────────────────────────────────────────────────

def load_ml_model():
    global model, scaler, feature_cols, target_cols
    try:
        for path in ("sla_model.pkl", "scaler.pkl"):
            if not os.path.exists(path):
                logger.error(f"Required file not found: {path}")
                sys.exit(1)

        with open("sla_model.pkl",    "rb") as f: model        = pickle.load(f)
        with open("scaler.pkl",       "rb") as f: scaler       = pickle.load(f)
        if os.path.exists("feature_cols.pkl"):
            with open("feature_cols.pkl", "rb") as f: feature_cols = pickle.load(f)
        if os.path.exists("target_cols.pkl"):
            with open("target_cols.pkl",  "rb") as f: target_cols  = pickle.load(f)

        logger.info("=" * 60)
        logger.info(f"Model type  : {type(model).__name__}")
        logger.info(f"Features    : {feature_cols}")
        logger.info(f"Targets     : {target_cols}")
        logger.info("=" * 60)
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        sys.exit(1)


# ── /health ───────────────────────────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health_check():
    ok = model is not None and scaler is not None
    return jsonify({
        "status":       "healthy" if ok else "unhealthy",
        "service":      "ml-prediction-regression",
        "model_type":   type(model).__name__ if model else None,
        "model_loaded": ok,
    })


# ── /predict ──────────────────────────────────────────────────────────────────

REQUIRED_FEATURES = [
    "avg_response_time", "error_rate", "cpu_usage", "memory_usage",
    "p95_latency", "request_count", "network_latency", "disk_usage",
]

# Map from model feature names to the SLA metric names used in Spring Boot DB
METRIC_TO_SLA_NAME = {
    "avg_response_time": "response_time",
    "error_rate":        "error_rate",
    "cpu_usage":         "cpu_usage",
    "memory_usage":      "memory_usage",
    "p95_latency":       "p95_latency",
    "request_count":     "request_count",
    "network_latency":   "network_latency",
    "disk_usage":        "disk_usage",
}

# Conservative defaults if caller doesn't pass thresholds{}.
# Tuned to be realistic (not ultra-strict, not too loose).
DEFAULT_THRESHOLDS = {
    "avg_response_time": 500.0,   # ms
    "error_rate":          5.0,   # %
    "cpu_usage":          85.0,   # %
    "memory_usage":      800.0,   # MB
    "p95_latency":       700.0,   # ms
    "request_count":    9999.0,   # req/s — practically unbounded
    "network_latency":   100.0,   # ms
    "disk_usage":         85.0,   # %
}


@app.route("/predict", methods=["POST"])
def predict():
    """
    Accepts:
      {
        "avg_response_time": <float>,   # current metric values
        "error_rate":        <float>,
        "cpu_usage":         <float>,
        "memory_usage":      <float>,
        "p95_latency":       <float>,
        "request_count":     <float>,
        "network_latency":   <float>,
        "disk_usage":        <float>,
        "thresholds": {                 # optional — active SLA thresholds
          "response_time":  <float>,
          "cpu_usage":      <float>,
          ...
        }
      }

    Returns:
      {
        "status":           "success",
        "risk_score":       <0–100 float>,      # % chance of future breach
        "predictions":      { metric: next_value, ... },
        "breach_count":     <int>,              # metrics already in breach
        "predicted_breach_count": <int>,        # metrics forecast to breach
        "breached_metrics": [ ... ],            # currently breached
        "predicted_breaches": [ ... ],          # forecast to breach next tick
        "features_used":    [ ... ],
      }
    """
    if model is None or scaler is None:
        return jsonify({"error": "ML model not available"}), 503

    data = request.get_json()
    if not data:
        return jsonify({"error": "Missing request body"}), 400

    # ── Extract current feature values ────────────────────────────────────────
    current_values = {}
    features       = []
    for f in REQUIRED_FEATURES:
        val = data.get(f)
        if val is None:
            return jsonify({"error": f"Missing feature: {f}"}), 400
        v = float(val)
        features.append(v)
        current_values[f] = v

    # ── Run regression model ──────────────────────────────────────────────────
    try:
        X_scaled        = scaler.transform([features])
        predicted_array = model.predict(X_scaled)[0]  # shape: (8,)
    except Exception as e:
        logger.error(f"Model inference error: {e}", exc_info=True)
        return jsonify({"error": f"Inference failed: {e}"}), 500

    # Map predicted values back to metric names (strip "next_" prefix)
    predictions = {}
    for i, col in enumerate(target_cols):
        base_name           = col.replace("next_", "")
        predictions[base_name] = round(float(predicted_array[i]), 3)

    # ── Resolve thresholds (Strictly from Spring Boot SLA config) ──────────────
    raw_thresholds = data.get("thresholds", {})
    thresholds     = {k.lower().strip(): float(v)
                      for k, v in raw_thresholds.items()} if raw_thresholds else {}

    # If no SLAs are created, we stop here and return a baseline healthy score.
    if not thresholds:
        logger.info("No active SLAs received. Returning baseline 5.0% risk.")
        return jsonify({
            "status":                 "success",
            "risk_score":             5.0,
            "predictions":            predictions,
            "breach_count":           0,
            "predicted_breach_count": 0,
            "breached_metrics":       [],
            "predicted_breaches":     [],
            "features_used":          REQUIRED_FEATURES,
        })

    def get_user_threshold(feature_name: str) -> float | None:
        sla_name = METRIC_TO_SLA_NAME.get(feature_name, feature_name)
        return thresholds.get(sla_name) or thresholds.get(feature_name)

    # ── Check CURRENT vs threshold (already-breached metrics) ─────────────────
    current_breaches = []
    for feat, curr_val in current_values.items():
        thr = get_user_threshold(feat)
        if thr and curr_val > thr:
            current_breaches.append(METRIC_TO_SLA_NAME.get(feat, feat))

    # ── Physical Constraint Clamping ─────────────────────────────────────────
    # Removed clamping to allow ML model to predict actual breaches for PREDICTED_RISK alerts
    clamped_predictions = predictions.copy()

    # ── Check PREDICTED vs threshold ─────────────────────────────────────────
    predicted_breaches = []
    worst_ratio        = 0.0

    for feat, pred_val in clamped_predictions.items():
        thr = get_user_threshold(feat)
        if thr:
            ratio = pred_val / thr
            worst_ratio = max(worst_ratio, ratio)
            if pred_val > thr:
                predicted_breaches.append(METRIC_TO_SLA_NAME.get(feat, feat))

    # ── Compute risk score (0–100) ────────────────────────────────────────────
    # ── Compute risk score (0–100) ────────────────────────────────────────────
    # More conservative scaling:
    # < 80% of threshold -> 5% risk
    # 100% of threshold  -> 50% risk
    # 150% of threshold  -> 90% risk
    base_risk = 0.0
    if worst_ratio > 0.8:
        # Interpolate ratio 0.8 -> 1.5 into 0 -> 90 risk
        base_risk = (worst_ratio - 0.8) / (1.5 - 0.8) * 90.0
    
    # Bonuses for specific conditions
    breach_bonus  = len(current_breaches)   * 20.0
    predict_bonus = len(predicted_breaches) * 10.0
    
    risk_score = min(99.9, max(5.0, base_risk + breach_bonus + predict_bonus))
    
    # If no actual breaches yet, cap risk at 90%
    if len(current_breaches) == 0:
        risk_score = min(90.0, risk_score)
    
    # LOG DETAILS for debugging
    for feat, p_val in predictions.items():
        thr = get_user_threshold(feat)
        if thr:
            logger.info(f"  {feat}: pred={p_val:.1f} vs thresh={thr:.1f} (ratio={p_val/thr:.2f})")

    logger.info(
        f"Prediction done | worst_ratio={worst_ratio:.3f} "
        f"risk={risk_score:.1f}% "
        f"current_breaches={current_breaches} "
        f"predicted_breaches={predicted_breaches}"
    )

    return jsonify({
        "status":                 "success",
        "risk_score":             round(risk_score, 1),
        "predictions":            predictions,
        "breach_count":           len(current_breaches),
        "predicted_breach_count": len(predicted_breaches),
        "breached_metrics":       current_breaches,
        "predicted_breaches":     predicted_breaches,
        "features_used":          REQUIRED_FEATURES,
    })


# ── /anomaly ──────────────────────────────────────────────────────────────────

@app.route("/anomaly", methods=["POST"])
def detect_anomaly():
    """Stateless Z-score anomaly detector (unchanged)."""
    try:
        data = request.get_json()
        if not data or "values" not in data or "current" not in data:
            return jsonify({"error": "Missing values or current"}), 400

        values  = data["values"]
        current = float(data["current"])

        if len(values) < 3:
            return jsonify({"is_anomaly": False, "reason": "insufficient_data"})

        mean    = np.mean(values)
        std     = np.std(values) + 0.001
        z_score = abs((current - mean) / std)

        return jsonify({
            "is_anomaly": bool(z_score > 3.0),
            "z_score":    float(z_score),
            "severity":   "HIGH" if z_score > 5 else "MEDIUM" if z_score > 3 else "LOW",
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ── Entry point ────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    load_ml_model()
    logger.info("ML Regression Prediction Service – port 5001")
    app.run(host="0.0.0.0", port=5001, debug=False)
