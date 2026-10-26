ImpactFlow Frontend (starter)

Contents
- index.html: user interface
- styles.css: responsive styling
- app.js: calls existing local APIs

Expected backend endpoints
- POST http://localhost:8082/api/changes/submit
- GET  http://localhost:8083/api/reports
- GET  http://localhost:8083/api/reports/summary
- GET  http://localhost:8083/api/reports/health

Run locally
1. Keep the backend services running.
2. Open CMD in this folder.
3. Run: python -m http.server 5500
4. Open: http://localhost:5500

Important
Browsers may block calls from port 5500 to backend ports 8082 and 8083 due to CORS. If that happens, we will configure CORS in the Spring Boot services or serve the frontend through the API Gateway. Do not disable browser security.

This UI is a first working draft. Verify the integration against the actual backend response formats before treating every user journey as complete.
