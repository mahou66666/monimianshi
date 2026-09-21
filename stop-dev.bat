@echo off
setlocal

cd /d "%~dp0"

echo [dev-bootstrap] Stopping Docker services from docker-compose.dev.yml ...
docker compose -f docker-compose.dev.yml down --remove-orphans
if errorlevel 1 (
  echo [dev-bootstrap] Failed to stop services.
  exit /b 1
)

echo.
echo [dev-bootstrap] All services are stopped.
echo [dev-bootstrap] Data volumes are kept.

exit /b 0
