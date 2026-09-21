@echo off
setlocal

cd /d "%~dp0"

echo [dev-bootstrap] Stopping Docker services with local MySQL snapshot ...
docker compose -f docker-compose.dev.yml -f docker-compose.local-db.yml down --remove-orphans
if errorlevel 1 (
  echo [dev-bootstrap] Failed to stop local-db services.
  exit /b 1
)

echo.
echo [dev-bootstrap] All local-db services are stopped.
echo [dev-bootstrap] Data volumes are kept.

exit /b 0
