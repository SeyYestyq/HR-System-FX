#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "== mvn test =="
mvn -q test
echo "== OK: unit/e2e/property/differential =="
