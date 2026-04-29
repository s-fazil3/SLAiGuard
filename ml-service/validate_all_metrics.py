import pickle
import numpy as np
import pandas as pd
import os

# Ensure we're in the right directory
os.chdir(r"c:\Users\fzfaz\OneDrive\Documents\SLA\ml-service")

m  = pickle.load(open('sla_model.pkl','rb'))
sc = pickle.load(open('scaler.pkl','rb'))
fc = pickle.load(open('feature_cols.pkl','rb'))
tc = pickle.load(open('target_cols.pkl','rb'))

# Load training data
df = pd.read_csv('metrics.csv').sample(5000, random_state=42)
X  = sc.transform(df[fc].values)
y  = df[tc].values
p  = m.predict(X)

# Define limits as used in applyPhysicalClamp in MetricService.java
limits = {
    'cpu_usage': 100.0,
    'disk_usage': 100.0,
    'error_rate': 100.0,
    'memory_usage': 1024.0,
    'avg_response_time': 5000.0,
    'p95_latency': 15000.0,
    'network_latency': 1000.0,
    'request_count': 10000.0
}

print(f"{'Metric':<20} | {'Max Predict':<12} | {'Max Ratio':<10} | {'Physical Limit':<14}")
print("-" * 65)

for i, col in enumerate(fc):
    cur = df[col].values
    pred = p[:, i]
    max_p = np.max(pred)
    # Ratio is only meaningful if cur > 0
    safe = cur > 1.0 # Use 1.0 to avoid small numbers causing huge ratios
    ratios = pred[safe] / cur[safe]
    max_r = np.max(ratios) if len(ratios) > 0 else 0.0
    limit = limits.get(col, "N/A")
    
    # Check if prediction exceeds physical limit
    status = "!! EXCESS" if isinstance(limit, float) and max_p > limit + 1 else "OK"
    
    print(f"{col:<20} | {max_p:12.2f} | {max_r:10.2f} | {limit:<8} | {status}")
