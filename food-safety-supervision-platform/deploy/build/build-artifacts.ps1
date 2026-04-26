$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $root

Write-Host "Building backend executable jars..." -ForegroundColor Cyan
mvn "-Dmaven.repo.local=.mvn_repo" -DskipTests clean package `
  -pl user-service,gateway-service,regulation-service,regulation-operation-service,complaint-service,query-service,warning-service `
  -am

Write-Host "Building frontend dist..." -ForegroundColor Cyan
Push-Location "$root\food-web"
npm ci
npm run build
Pop-Location

Write-Host "Done. Verify these outputs:" -ForegroundColor Green
Write-Host "  user-service/target/user-service-0.0.1-SNAPSHOT.jar"
Write-Host "  gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar"
Write-Host "  regulation-service/target/regulation-service-0.0.1-SNAPSHOT.jar"
Write-Host "  regulation-operation-service/target/regulation-operation-service-0.0.1-SNAPSHOT.jar"
Write-Host "  complaint-service/target/complaint-service-0.0.1-SNAPSHOT.jar"
Write-Host "  query-service/target/query-service-0.0.1-SNAPSHOT.jar"
Write-Host "  warning-service/target/warning-service-0.0.1-SNAPSHOT.jar"
Write-Host "  food-web/dist"
