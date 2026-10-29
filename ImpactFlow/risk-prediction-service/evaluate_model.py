
import numpy as np
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.tree import DecisionTreeClassifier

X = np.array([
    [1,10,5,1],[2,20,10,2],[3,40,15,2],
    [5,120,30,3],[8,250,100,4],[12,500,200,5],
    [2,5,2,1],[6,180,80,4],[10,350,150,5],
    [4,60,20,2],[2,889,324,5],[5,700,300,5],
    [8,1000,500,5],[12,1500,800,5],[15,2000,1000,5],
    [10,100,50,2],[3,300,200,4],[1,600,400,5]
])

y = np.array([
    "LOW","LOW","LOW","MEDIUM","HIGH","HIGH",
    "LOW","HIGH","HIGH","MEDIUM","HIGH","HIGH",
    "HIGH","HIGH","HIGH","MEDIUM","HIGH","HIGH"
])

model = DecisionTreeClassifier(max_depth=4, random_state=42)
cv = StratifiedKFold(n_splits=3, shuffle=True, random_state=42)

predictions = cross_val_predict(model, X, y, cv=cv)
labels = ["LOW", "MEDIUM", "HIGH"]

print("Evaluation: 3-fold stratified cross-validation")
print("Dataset size:", len(y))
print("Accuracy:", round(accuracy_score(y, predictions), 3))
print("\nClassification report:")
print(classification_report(y, predictions, labels=labels, zero_division=0))
print("Confusion matrix (LOW, MEDIUM, HIGH):")
print(confusion_matrix(y, predictions, labels=labels))

scores = []
for train_idx, test_idx in cv.split(X, y):
    model.fit(X[train_idx], y[train_idx])
    scores.append(model.score(X[test_idx], y[test_idx]))

print("Fold accuracies:", [round(s, 3) for s in scores])
print("Mean fold accuracy:", round(float(np.mean(scores)), 3))
print("Note: Small synthetic dataset; results do not establish real-world accuracy.")
