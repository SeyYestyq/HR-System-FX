$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
if (-not (Test-Path ".env")) { Copy-Item ".env.example" ".env" }
docker compose --env-file .env -f docker-compose.yml up -d --build --wait
Write-Host "Web UI:      http://localhost:8080/"
Write-Host "Swagger:     http://localhost:8080/swagger-ui.html"
Write-Host "Health:      http://localhost:8080/actuator/health"
Write-Host "REST API:    http://localhost:8080/api"
