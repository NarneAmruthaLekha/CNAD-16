
import numpy as np
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix
)
from sklearn.tree import DecisionTreeClassifier

# Evaluation examples kept separate from the training examples.
X_test = np.array([
    [1, 10, 5, 1],
    [5, 120, 30, 3],
    [12, 500, 200, 5],
    [2, 5, 2, 1],
    [8, 250, 100, 4],
    [4, 60, 20, 2]
])

y_test = np.array([
    "LOW", "MEDIUM", "HIGH",
    "LOW", "HIGH", "MEDIUM"
])

# Use the same training examples as main.py.
X_train = np.array([
    [1, 10, 5, 1], [2, 20, 10, 2],
    [3, 40, 15, 2], [5, 120, 30, 3],
    [8, 250, 100, 4], [12, 500, 200, 5],
    [2, 5, 2, 1], [6, 180, 80, 4],
    [10, 350, 150, 5], [4, 60, 20, 2],
    [2, 889, 324, 5], [5, 700, 300, 5],
    [8, 1000, 500, 5], [12, 1500, 800, 5],
    [15, 2000, 1000, 5], [10, 100, 50, 2],
    [3, 300, 200, 4], [1, 600, 400, 5]
])

y_train = np.array([
    "LOW", "LOW", "LOW", "MEDIUM", "HIGH", "HIGH",
    "LOW", "HIGH", "HIGH", "MEDIUM", "HIGH", "HIGH",
    "HIGH", "HIGH", "HIGH", "MEDIUM", "HIGH", "HIGH"
])

model = DecisionTreeClassifier(max_depth=4, random_state=42)
model.fit(X_train, y_train)

predictions = model.predict(X_test)

print("Accuracy:", round(accuracy_score(y_test, predictions), 3))
print("\nClassification report:")
print(classification_report(
    y_test, predictions, zero_division=0
))
print("Confusion matrix (LOW, MEDIUM, HIGH):")
print(confusion_matrix(
    y_test, predictions, labels=["LOW", "MEDIUM", "HIGH"]
))
