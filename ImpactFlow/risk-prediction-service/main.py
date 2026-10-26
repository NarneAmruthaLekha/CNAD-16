
from fastapi import FastAPI
from pydantic import BaseModel
from pathlib import Path
import joblib
import numpy as np
from sklearn.tree import DecisionTreeClassifier

app = FastAPI(title="ImpactFlow Risk Prediction Service")

MODEL_PATH = Path(__file__).parent / "model.joblib"

class RiskRequest(BaseModel):
    changed_files: int
    lines_added: int = 0
    lines_deleted: int = 0
    complexity: int = 1

def train_model():
    x = np.array([
        [1,10,5,1],
        [2,20,10,2],
        [3,40,15,2],
        [5,120,30,3],
        [8,250,100,4],
        [12,500,200,5],
        [2,5,2,1],
        [6,180,80,4],
        [10,350,150,5],
        [4,60,20,2],
        [2,889,324,5],
        [5,700,300,5],
        [8,1000,500,5],
        [12,1500,800,5],
        [15,2000,1000,5],
        [10,100,50,2],
        [3,300,200,4],
        [1,600,400,5]
    ])
    y = np.array([
        "LOW","LOW","LOW","MEDIUM","HIGH",
        "HIGH","LOW","HIGH","HIGH","MEDIUM",
        "HIGH","HIGH","HIGH","HIGH","HIGH",
        "MEDIUM","HIGH","HIGH"
    ])
    model = DecisionTreeClassifier(max_depth=4, random_state=42)
    model.fit(x,y)
    joblib.dump(model,MODEL_PATH)
    return model

@app.on_event("startup")
def startup():
    train_model()

@app.get("/health")
def health():
    return {"status":"UP","service":"risk-prediction-service"}

@app.post("/predict")
def predict(request: RiskRequest):
    if not MODEL_PATH.exists():
        train_model()

    model = joblib.load(MODEL_PATH)
    features = np.array([[
        request.changed_files,
        request.lines_added,
        request.lines_deleted,
        request.complexity
    ]])

    prediction = model.predict(features)[0]
    probabilities = model.predict_proba(features)[0]
    classes = list(model.classes_)
    risk_order = {"LOW":1,"MEDIUM":2,"HIGH":3}
    score = sum(
        float(probability) * risk_order.get(str(label),2)
        for label,probability in zip(classes,probabilities)
    ) / 3

    return {
        "risk":str(prediction),
        "risk_score":round(score,3),
        "message":"Prediction generated using the trained decision tree model."
    }
