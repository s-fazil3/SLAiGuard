"""
train_model.py
==============
Train a Multi-Output Random Forest Regressor model for SLA breach PREDICTION.

Purpose
-------
Given the CURRENT values of 8 production metrics, predict the NEXT tick's
values for all 8 metrics. Random Forest is used for better non-linear
pattern recognition and higher prediction accuracy compared to Linear Regression.

Usage
-----
1. Generate data: python generate_training_data.py
2. Train model:   python train_model.py
"""

import os
import pickle
import logging
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import (
    mean_squared_error,
    r2_score,
    roc_auc_score,
    confusion_matrix,
    classification_report,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# ── Configuration ─────────────────────────────────────────────────────────────

DATASET_PATH = "metrics.csv"

FEATURE_COLS = [
    "avg_response_time",
    "error_rate",
    "cpu_usage",
    "memory_usage",
    "p95_latency",
    "request_count",
    "network_latency",
    "disk_usage",
]

TARGET_COLS = [f"next_{c}" for c in FEATURE_COLS]

TEST_SIZE    = 0.20
RANDOM_SEED  = 42


# ── Load data ─────────────────────────────────────────────────────────────────

def load_data(path: str) -> pd.DataFrame:
    if not os.path.exists(path):
        raise FileNotFoundError(f"Dataset not found: {path}")
    df = pd.read_csv(path)
    logger.info(f"Loaded {len(df)} rows from {path}")
    return df


# ── Train ─────────────────────────────────────────────────────────────────────

def train(df: pd.DataFrame):
    X = df[FEATURE_COLS].values
    y = df[TARGET_COLS].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=RANDOM_SEED
    )

    logger.info("Scaling features...")
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled  = scaler.transform(X_test)

    logger.info("Fitting Multi-Output Random Forest Regressor model (Advanced SLA Prediction)...")
    model = RandomForestRegressor(
        n_estimators=200,        # More trees for better accuracy
        max_depth=10,            # Control overfitting
        min_samples_split=5,     # Require more samples to split
        min_samples_leaf=2,      # Require more samples per leaf
        random_state=42,         # Reproducibility
        n_jobs=-1                # Use all CPU cores
    )
    model.fit(X_train_scaled, y_train)

    # ── Evaluate Regression ───────────────────────────────────────────────────
    preds = model.predict(X_test_scaled)
    
    logger.info("Results per target metric:")
    for i, col in enumerate(FEATURE_COLS):
        mse = mean_squared_error(y_test[:, i], preds[:, i])
        r2  = r2_score(y_test[:, i], preds[:, i])
        logger.info(f"  {col:20} | RMSE: {np.sqrt(mse):.3f} | R2: {r2:.3f}")

    # ── Save Artifacts ────────────────────────────────────────────────────────
    logger.info("Saving model artifacts...")
    with open("sla_model.pkl", "wb") as f:
        pickle.dump(model, f)
    with open("scaler.pkl", "wb") as f:
        pickle.dump(scaler, f)
    with open("feature_cols.pkl", "wb") as f:
        pickle.dump(FEATURE_COLS, f)
    with open("target_cols.pkl", "wb") as f:
        pickle.dump(TARGET_COLS, f)
    
    logger.info("Training complete. Artifacts saved: sla_model.pkl, scaler.pkl, feature_cols.pkl, target_cols.pkl")

if __name__ == "__main__":
    try:
        data = load_data(DATASET_PATH)
        train(data)
    except Exception as e:
        logger.error(f"Training failed: {e}")
        exit(1)
