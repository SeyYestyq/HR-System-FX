#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then cp .env.example .env; fi
docker compose --env-file .env -f docker-compose.yml up -d --build --wait
echo "Web UI:      http://localhost:8080/"
echo "Swagger:     http://localhost:8080/swagger-ui.html"
echo "Health:      http://localhost:8080/actuator/health"
echo "REST API:    http://localhost:8080/api"
