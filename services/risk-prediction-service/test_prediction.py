import os
import pytest
import joblib
from main import evaluate_risk, get_contributors, load_model

@pytest.fixture(scope="module", autouse=True)
def setup_model():
    # Ensure model is trained and loaded before running tests
    load_model()

def test_model_file_exists():
    model_path = os.path.join(os.path.dirname(__file__), 'model.joblib')
    assert os.path.exists(model_path), "model.joblib does not exist"

def test_evaluate_risk_high():
    # High LOC, High complexity, multiple impacted services
    risk_level, confidence = evaluate_risk(1000, 45, 0.9, 4)
    assert risk_level == "High"
    assert 0.0 <= confidence <= 1.0

def test_evaluate_risk_low():
    # Low metrics across the board
    risk_level, confidence = evaluate_risk(10, 2, 0.1, 0)
    assert risk_level == "Low"
    assert 0.0 <= confidence <= 1.0

def test_get_contributors_high():
    contributors = get_contributors(1000, 45, 0.9, 4)
    assert any("Lines of code changed" in c for c in contributors)
    assert any("Cyclomatic complexity" in c for c in contributors)
    assert any("Downstream services impacted" in c for c in contributors)
    assert any("File churn rate" in c for c in contributors)

def test_get_contributors_low():
    contributors = get_contributors(10, 2, 0.1, 0)
    assert any("Standard metrics" in c for c in contributors)
