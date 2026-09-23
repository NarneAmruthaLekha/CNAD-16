import os
import time
import json
import logging
import threading
from datetime import datetime
from typing import List, Dict, Any
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
from py_eureka_client import eureka_client
from kafka import KafkaConsumer, KafkaProducer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("risk-prediction-service")

app = FastAPI(title="Risk Prediction Service", version="1.0.0")

# Cache for predicted results
predictions_cache: Dict[str, Dict[str, Any]] = {}

# ML Model loading
model_path = os.path.join(os.path.dirname(__file__), 'model.joblib')
model = None

def load_model():
    global model
    if not os.path.exists(model_path):
        logger.info("Model file not found. Running training script...")
        from train_model import train_and_save_model
        train_and_save_model()
    model = joblib.load(model_path)
    logger.info("Model loaded successfully.")

# DTOs
class PredictRequest(BaseModel):
    submissionId: str
    totalLocDelta: int
    cyclomaticComplexity: int
    fileChurnRate: float
    impactedServices: List[str]

class RiskPredictionResponse(BaseModel):
    submissionId: str
    riskLevel: str
    confidence: float
    contributors: List[str]
    predictedAt: str

def evaluate_risk(totalLocDelta: int, cyclomaticComplexity: int, fileChurnRate: float, impacted_count: int) -> tuple:
    if model is None:
        load_model()
        
    features = [[totalLocDelta, cyclomaticComplexity, fileChurnRate, impacted_count]]
    risk_class = model.predict(features)[0]
    probs = model.predict_proba(features)[0]
    confidence = float(probs[risk_class])
    
    risk_level = "Low" if risk_class == 0 else "Medium" if risk_class == 1 else "High"
    return risk_level, confidence

def get_contributors(loc: int, complexity: int, churn: float, impacted: int) -> List[str]:
    global model
    if model is None:
        load_model()
        
    # Features in training: totalLocDelta, cyclomaticComplexity, fileChurnRate, impactedServicesCount
    importances = model.feature_importances_
    
    thresholds = [150, 10, 0.3, 1]
    values = [loc, complexity, churn, impacted]
    names = [
        "Lines of code changed (LOC delta)",
        "Cyclomatic complexity",
        "File churn rate",
        "Downstream services impacted"
    ]
    
    contributions = []
    for i in range(4):
        ratio = values[i] / thresholds[i] if thresholds[i] > 0 else 0
        weight = ratio * importances[i]
        contributions.append((names[i], weight, values[i], thresholds[i]))
        
    # Sort by contribution weight descending
    contributions.sort(key=lambda x: x[1], reverse=True)
    
    contributors = []
    for name, weight, val, thresh in contributions:
        if val > thresh:
            if isinstance(val, float):
                contributors.append(f"{name} is elevated (Value: {val:.2f}, relative weight: {weight:.2f})")
            else:
                contributors.append(f"{name} is elevated (Value: {val}, relative weight: {weight:.2f})")
                
    if not contributors:
        top_feature_idx = importances.argmax()
        contributors.append(f"Standard metrics (Top driver: {names[top_feature_idx]} with importance {importances[top_feature_idx]:.2f})")
        
    return contributors

# Kafka background task
def kafka_consumer_loop():
    logger.info("Starting Kafka Consumer Thread...")
    consumer = None
    producer = None
    
    # Connection retry loop
    while consumer is None or producer is None:
        try:
            consumer = KafkaConsumer(
                'change.enriched',
                bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', '127.0.0.1:9092').split(','),
                group_id='risk-predictor-group',
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                consumer_timeout_ms=5000
            )
            producer = KafkaProducer(
                bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', '127.0.0.1:9092').split(','),
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            logger.info("Successfully connected to Kafka brokers.")
        except Exception as e:
            logger.warning(f"Kafka brokers not available yet: {e}. Retrying in 5 seconds...")
            time.sleep(5)

    while True:
        try:
            for message in consumer:
                event = message.value
                logger.info(f"Consumed ChangeEnrichedEvent: {event}")
                
                sub_id = event.get('submissionId')
                loc = event.get('totalLocDelta', 0)
                complexity = event.get('cyclomaticComplexity', 0)
                churn = event.get('fileChurnRate', 0.0)
                impacted_services = event.get('impactedServices', [])
                
                risk_level, confidence = evaluate_risk(loc, complexity, churn, len(impacted_services))
                contributors = get_contributors(loc, complexity, churn, len(impacted_services))
                predicted_at = datetime.utcnow().isoformat()
                
                prediction = {
                    "submissionId": sub_id,
                    "riskLevel": risk_level,
                    "confidence": confidence,
                    "contributors": contributors,
                    "predictedAt": predicted_at
                }
                
                # Cache prediction
                predictions_cache[sub_id] = prediction
                
                # Publish risk.scored event
                logger.info(f"Publishing risk.scored event: {prediction}")
                producer.send('risk.scored', key=sub_id.encode('utf-8'), value=prediction)
                
        except Exception as e:
            logger.error(f"Error in Kafka consumer loop: {e}")
            time.sleep(2)

@app.on_event("startup")
async def startup_event():
    # Load ML Model
    load_model()
    
    # Start Kafka Consumer Thread
    threading.Thread(target=kafka_consumer_loop, daemon=True).start()
    
    # Register with Eureka (with safety fallback if Eureka is not running)
    try:
        await eureka_client.init_async(
            eureka_server="http://localhost:8761/eureka",
            app_name="risk-prediction-service",
            instance_port=8083,
            instance_host="localhost"
        )
        logger.info("Successfully registered with Eureka server.")
    except Exception as e:
        logger.warning(f"Could not register with Eureka server: {e}. Running standalone.")

@app.post("/api/predict", response_model=RiskPredictionResponse)
def predict_risk(request: PredictRequest):
    try:
        risk_level, confidence = evaluate_risk(
            request.totalLocDelta,
            request.cyclomaticComplexity,
            request.fileChurnRate,
            len(request.impactedServices)
        )
        
        contributors = get_contributors(
            request.totalLocDelta,
            request.cyclomaticComplexity,
            request.fileChurnRate,
            len(request.impactedServices)
        )
        
        prediction = {
            "submissionId": request.submissionId,
            "riskLevel": risk_level,
            "confidence": confidence,
            "contributors": contributors,
            "predictedAt": datetime.utcnow().isoformat()
        }
        
        # Cache prediction
        predictions_cache[request.submissionId] = prediction
        return RiskPredictionResponse(**prediction)
    except Exception as e:
        logger.error(f"Error predicting risk synchronously: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/predict/{submissionId}", response_model=RiskPredictionResponse)
def get_prediction(submissionId: str):
    if submissionId in predictions_cache:
        return RiskPredictionResponse(**predictions_cache[submissionId])
    raise HTTPException(status_code=404, detail=f"Prediction for submission ID {submissionId} not found.")
