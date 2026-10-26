
FROM python:3.11-slim
WORKDIR /app
COPY main.py .
RUN pip install --no-cache-dir --retries 10 --timeout 120 fastapi uvicorn joblib numpy scikit-learn
EXPOSE 8000
CMD ["uvicorn","main:app","--host","0.0.0.0","--port","8000"]
