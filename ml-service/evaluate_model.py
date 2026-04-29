import pickle
import numpy as np
import pandas as pd
from sklearn.metrics import r2_score, mean_squared_error

model        = pickle.load(open("sla_model.pkl", "rb"))
scaler       = pickle.load(open("scaler.pkl",    "rb"))
feature_cols = pickle.load(open("feature_cols.pkl", "rb"))
target_cols  = pickle.load(open("target_cols.pkl",  "rb"))

print("=" * 60)
print(f"Model type      : {type(model).__name__}")
print(f"n_estimators    : {model.n_estimators}")
print(f"max_depth       : {model.max_depth}")
print("=" * 60)

df = pd.read_csv("metrics.csv").sample(5000, random_state=42)
X  = scaler.transform(df[feature_cols].values)
y  = df[target_cols].values
p  = model.predict(X)

print("\nPer-metric  R2  (1.0 = perfect, >0.85 = good):")
for i, col in enumerate(feature_cols):
    r2   = r2_score(y[:, i], p[:, i])
    rmse = np.sqrt(mean_squared_error(y[:, i], p[:, i]))
    print(f"  {col:22}  R2={r2:+.3f}   RMSE={rmse:.2f}")

print("\nSpike-safety check — max predicted/current ratio across 5k samples:")
for i, col in enumerate(feature_cols):
    cur  = df[col].values
    pred = p[:, i]
    safe = cur > 0
    ratios = pred[safe] / cur[safe]
    print(f"  {col:22}  max_ratio={ratios.max():.2f}  mean_ratio={ratios.mean():.2f}")

scenarios = {
    "NORMAL     (low stress)":      [130.0, 0.8, 28.0,  350.0, 200.0, 180.0,  15.0, 54.0],
    "MODERATE   (mid stress)":      [280.0, 2.5, 52.0,  580.0, 430.0, 300.0,  55.0, 61.0],
    "HIGH STRESS":                  [420.0, 6.0, 75.0,  720.0, 640.0, 400.0,  95.0, 70.0],
}

print("\nSpot-check predictions (current -> predicted_next):")
for label, inp in scenarios.items():
    Xs = scaler.transform([inp])
    pred = model.predict(Xs)[0]
    print(f"\n  {label}")
    for feat, c, pr in zip(feature_cols, inp, pred):
        ratio = pr / c if c > 0 else 0
        flag  = "*** SPIKE ***" if ratio > 2.0 else ""
        print(f"    {feat:22} {c:7.1f} -> {pr:7.1f}  (x{ratio:.2f}) {flag}")
