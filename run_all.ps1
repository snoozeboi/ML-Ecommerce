<#
 Simple launcher script:
 1) Starts Docker (Postgres, RabbitMQ, Elasticsearch) in THIS window and waits for it
 2) Opens 3 windows: ML, Backend, Frontend

 Backend fails with "Connection to localhost:5433 refused" if Docker/Postgres is not running.
 Run from project folder: powershell -ExecutionPolicy Bypass -File .\run_all.ps1
#>

# Use script location as project root - works on any machine
$projectRoot = $PSScriptRoot
$backendDir = Join-Path $projectRoot "backend"
$mlDir = Join-Path $backendDir "ml_service"
$frontendDir = Join-Path $projectRoot "frontend"

# Check Docker is available
try {
  docker info 2>&1 | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Docker not running" }
} catch {
  Write-Host "ERROR: Docker is not running. Start Docker Desktop and run this script again." -ForegroundColor Red
  exit 1
}

Write-Host "Step 1: Starting Docker (Postgres, RabbitMQ, Elasticsearch) in this window..." -ForegroundColor Cyan
Set-Location $backendDir
& docker compose up -d
if ($LASTEXITCODE -ne 0) {
  Write-Host "Docker compose failed. Fix errors above and try again." -ForegroundColor Red
  exit 1
}

# Wait for Postgres (port 5433) to accept connections so Backend won't get "Connection refused"
Write-Host "Waiting for Postgres on port 5433..." -ForegroundColor Yellow
$maxWait = 60
$waited = 0
while ($waited -lt $maxWait) {
  $tcp = New-Object System.Net.Sockets.TcpClient
  try {
    $tcp.Connect("127.0.0.1", 5433)
    $tcp.Close()
    Write-Host "Postgres is ready." -ForegroundColor Green
    break
  } catch {
    Start-Sleep -Seconds 3
    $waited += 3
    Write-Host "  waiting... ($waited s)" -ForegroundColor Gray
  }
}
if ($waited -ge $maxWait) {
  Write-Host "Postgres did not become ready in time. Check Docker window. Continuing anyway." -ForegroundColor Yellow
}

Set-Location $projectRoot
Write-Host "Step 2: Opening ML, Backend, Frontend windows..." -ForegroundColor Cyan

# 2) ML service
Start-Process powershell -ArgumentList @(
  "-NoExit",
  "-Command",
  "cd '$mlDir'; py -u app.py"
)

# 3) Backend (needs Docker/DB already running)
Start-Process powershell -ArgumentList @(
  "-NoExit",
  "-Command",
  "cd '$backendDir'; Write-Host 'Starting Spring Boot (wait for ''Started...'' before using the app)...' -ForegroundColor Yellow; mvn spring-boot:run"
)

# Wait for Backend to respond on /health before opening Frontend (avoids "Failed to fetch" on first load)
$backendUrl = "http://localhost:8080/health"
$maxAttempts = 36
$attempt = 0
Write-Host "Waiting for Backend to be ready (checking $backendUrl)..." -ForegroundColor Yellow
do {
  Start-Sleep -Seconds 5
  $attempt++
  try {
    $r = Invoke-WebRequest -Uri $backendUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
    if ($r.StatusCode -eq 200) {
      Write-Host "Backend is up. Opening Frontend..." -ForegroundColor Green
      break
    }
  } catch {
    if ($attempt -ge $maxAttempts) {
      Write-Host "Backend did not respond after 3 minutes. Opening Frontend anyway - check Backend window for errors." -ForegroundColor Red
      break
    }
    Write-Host "  attempt $attempt/$maxAttempts - backend not ready yet." -ForegroundColor Gray
  }
} while ($attempt -lt $maxAttempts)

# 4) Frontend (uses Vite proxy to backend - same origin, no CORS issues)
Start-Process powershell -ArgumentList @(
  "-NoExit",
  "-Command",
  "cd '$frontendDir'; npm run dev"
)

Write-Host "Done. 4 windows are open. Use the app at http://localhost:5173" -ForegroundColor Green
Write-Host "If you see no products: run once from backend folder: mvn spring-boot:run -Dspring-boot.run.profiles=seed" -ForegroundColor Cyan

