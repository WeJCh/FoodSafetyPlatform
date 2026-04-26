#!/bin/bash
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

echo "[build] Building backend executable jars..."
mvn "-Dmaven.repo.local=.mvn_repo" -DskipTests clean package \
  -pl user-service,gateway-service,regulation-service,regulation-operation-service,complaint-service,query-service,warning-service \
  -am

echo "[build] Building frontend dist..."
cd "$ROOT_DIR/food-web"
npm ci
npm run build

echo "[build] Done."
