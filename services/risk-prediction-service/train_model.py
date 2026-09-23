import numpy as np
import pandas as pd
from sklearn.tree import DecisionTreeClassifier
import joblib

def generate_synthetic_data(n_samples_per_class=500):
    np.random.seed(42)
    
    # 1. Generate Low Risk Samples
    # complexity < 8, loc < 100, impacted <= 1
    low_loc = np.random.randint(5, 95, n_samples_per_class)
    low_complexity = np.random.randint(1, 7, n_samples_per_class)
    low_churn = np.random.uniform(0.0, 0.4, n_samples_per_class)
    low_impacted = np.random.randint(0, 2, n_samples_per_class)
    low_risk = np.zeros(n_samples_per_class, dtype=int)
    
    # 2. Generate High Risk Samples
    # complexity > 30 or loc > 600 or impacted >= 3
    # We will mix these conditions
    high_loc = []
    high_complexity = []
    high_churn = []
    high_impacted = []
    for _ in range(n_samples_per_class):
        choice = np.random.choice(['loc', 'complexity', 'impacted'])
        if choice == 'loc':
            high_loc.append(np.random.randint(601, 1200))
            high_complexity.append(np.random.randint(5, 30))
            high_impacted.append(np.random.randint(0, 3))
        elif choice == 'complexity':
            high_loc.append(np.random.randint(50, 600))
            high_complexity.append(np.random.randint(31, 60))
            high_impacted.append(np.random.randint(0, 3))
        else:
            high_loc.append(np.random.randint(50, 600))
            high_complexity.append(np.random.randint(5, 30))
            high_impacted.append(np.random.randint(3, 6))
        high_churn.append(np.random.uniform(0.3, 1.0))
    high_risk = np.ones(n_samples_per_class, dtype=int) * 2
    
    # 3. Generate Medium Risk Samples
    # Anything else. We'll generate ranges in between
    med_loc = np.random.randint(100, 599, n_samples_per_class)
    med_complexity = np.random.randint(8, 29, n_samples_per_class)
    med_churn = np.random.uniform(0.1, 0.7, n_samples_per_class)
    med_impacted = np.random.randint(1, 3, n_samples_per_class)
    med_risk = np.ones(n_samples_per_class, dtype=int)
    
    # Combine
    loc = np.concatenate([low_loc, med_loc, high_loc])
    complexity = np.concatenate([low_complexity, med_complexity, high_complexity])
    churn = np.concatenate([low_churn, med_churn, high_churn])
    impacted = np.concatenate([low_impacted, med_impacted, high_impacted])
    risk = np.concatenate([low_risk, med_risk, high_risk])
    
    df = pd.DataFrame({
        'totalLocDelta': loc,
        'cyclomaticComplexity': complexity,
        'fileChurnRate': churn,
        'impactedServicesCount': impacted,
        'riskLevel': risk
    })
    
    # Introduce class overlap/noise (e.g., flip 8% of labels randomly to prevent 1.0 confidence locks)
    np.random.seed(42) # Set seed for noise reproducibility
    n_noise = int(len(df) * 0.08)
    noise_indices = np.random.choice(df.index, size=n_noise, replace=False)
    for idx in noise_indices:
        df.loc[idx, 'riskLevel'] = np.random.choice([0, 1, 2])

    # Shuffle
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)
    return df

def train_and_save_model():
    print("Generating balanced synthetic dataset...")
    df = generate_synthetic_data()
    
    X = df[['totalLocDelta', 'cyclomaticComplexity', 'fileChurnRate', 'impactedServicesCount']]
    y = df['riskLevel']
    
    print("Training DecisionTreeClassifier...")
    clf = DecisionTreeClassifier(max_depth=5, random_state=42)
    clf.fit(X, y)
    
    # Save the model
    model_filename = 'model.joblib'
    joblib.dump(clf, model_filename)
    print(f"Model saved successfully to {model_filename}")

if __name__ == '__main__':
    train_and_save_model()
