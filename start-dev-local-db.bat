@echo off
setlocal

cd /d "%~dp0"

echo [dev-bootstrap] Starting Docker services with local MySQL snapshot ...
docker compose -f docker-compose.dev.yml -f docker-compose.local-db.yml up -d --build
if errorlevel 1 (
  echo [dev-bootstrap] Failed to start local-db services.
  exit /b 1
)

echo.
echo [dev-bootstrap] Current service status:
docker compose -f docker-compose.dev.yml -f docker-compose.local-db.yml ps

echo.
echo [dev-bootstrap] Admin Web:   http://localhost:9527
echo [dev-bootstrap] Admin API:   http://localhost:18081/admin
echo [dev-bootstrap] MySQL:       localhost:13306 / interview_agent
echo [dev-bootstrap] Redis:       localhost:16379
echo [dev-bootstrap] Resume AI:   http://localhost:8089/api/resume/parse
echo [dev-bootstrap] Note: first MySQL init imports interview_agent.sql and may take a little while.

exit /b 0
