@echo off
setlocal

cd /d "%~dp0"

echo [dev-bootstrap] Starting Docker services from docker-compose.dev.yml ...
docker compose -f docker-compose.dev.yml up -d --build
if errorlevel 1 (
  echo [dev-bootstrap] Failed to start services.
  exit /b 1
)

echo.
echo [dev-bootstrap] Current service status:
docker compose -f docker-compose.dev.yml ps

echo.
echo [dev-bootstrap] Admin Web:   http://localhost:9527
echo [dev-bootstrap] Admin API:   http://localhost:18081/admin
echo [dev-bootstrap] Redis:       localhost:16379
echo [dev-bootstrap] Resume AI:   http://localhost:8089/api/resume/parse
echo [dev-bootstrap] Note: first frontend compile may take 30-90 seconds.

exit /b 0
