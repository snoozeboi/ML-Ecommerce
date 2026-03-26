# Backend Verification and Run Script
# Run this AFTER restarting your computer

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Backend Tools Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# Check Java
Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion) {
        Write-Host "[OK] Java: $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "[X] Java not found" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "[X] Java not found" -ForegroundColor Red
    $allGood = $false
}

# Check Maven
Write-Host "Checking Maven..." -ForegroundColor Yellow
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    if ($mavenVersion) {
        Write-Host "[OK] Maven: $mavenVersion" -ForegroundColor Green
        $mavenHome = mvn -version 2>&1 | Select-String "Maven home"
        if ($mavenHome) {
            Write-Host "      $mavenHome" -ForegroundColor Gray
        }
    } else {
        Write-Host "[X] Maven not found" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "[X] Maven not found" -ForegroundColor Red
    $allGood = $false
}

# Check Git
Write-Host "Checking Git..." -ForegroundColor Yellow
try {
    $gitVersion = git --version 2>&1
    if ($gitVersion) {
        Write-Host "[OK] Git: $gitVersion" -ForegroundColor Green
    } else {
        Write-Host "[X] Git not found" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "[X] Git not found" -ForegroundColor Red
    $allGood = $false
}

# Check Docker
Write-Host "Checking Docker..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version 2>&1
    if ($dockerVersion) {
        Write-Host "[OK] Docker: $dockerVersion" -ForegroundColor Green
        
        # Check if Docker is running
        Write-Host "Checking if Docker is running..." -ForegroundColor Yellow
        try {
            $dockerPs = docker ps 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "[OK] Docker is running" -ForegroundColor Green
            } else {
                Write-Host "[!] Docker is installed but not running" -ForegroundColor Yellow
                Write-Host "    Please start Docker Desktop from Start menu" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "[!] Docker is installed but not running" -ForegroundColor Yellow
            Write-Host "    Please start Docker Desktop from Start menu" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[X] Docker not found" -ForegroundColor Red
        Write-Host "    Please install Docker Desktop from: E:\BackendTools\DockerDesktopInstaller.exe" -ForegroundColor Yellow
        $allGood = $false
    }
} catch {
    Write-Host "[X] Docker not found" -ForegroundColor Red
    Write-Host "    Please install Docker Desktop from: E:\BackendTools\DockerDesktopInstaller.exe" -ForegroundColor Yellow
    $allGood = $false
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($allGood) {
    Write-Host "All tools are installed!" -ForegroundColor Green
    Write-Host ""
    
    # Check if we're in the backend directory
    $currentDir = Get-Location
    if ($currentDir.Path -notlike "*backend*") {
        Write-Host "Changing to backend directory..." -ForegroundColor Yellow
        if (Test-Path "backend") {
            Set-Location backend
        } elseif (Test-Path "..\backend") {
            Set-Location ..\backend
        } else {
            Write-Host "[!] Backend directory not found. Please navigate to the backend folder manually." -ForegroundColor Yellow
            exit 1
        }
    }
    
    # Check Docker containers
    Write-Host ""
    Write-Host "Checking Docker containers..." -ForegroundColor Cyan
    try {
        $containers = docker ps -a 2>&1
        if ($LASTEXITCODE -eq 0) {
            $postgresRunning = docker ps --filter "name=postgres" --format "{{.Names}}" 2>&1
            $rabbitRunning = docker ps --filter "name=rabbitmq" --format "{{.Names}}" 2>&1
            $elasticRunning = docker ps --filter "name=elasticsearch" --format "{{.Names}}" 2>&1
            
            if ($postgresRunning -or $rabbitRunning -or $elasticRunning) {
                Write-Host "[OK] Some containers are running" -ForegroundColor Green
            } else {
                Write-Host "[!] No containers running. Starting Docker Compose..." -ForegroundColor Yellow
                Write-Host ""
                
                $startContainers = Read-Host "Do you want to start Docker containers? (y/n)"
                if ($startContainers -eq "y" -or $startContainers -eq "Y") {
                    Write-Host "Starting Docker containers..." -ForegroundColor Cyan
                    docker compose up -d
                    Write-Host ""
                    Write-Host "Waiting for containers to start..." -ForegroundColor Yellow
                    Start-Sleep -Seconds 10
                    Write-Host "[OK] Containers should be starting..." -ForegroundColor Green
                }
            }
        }
    } catch {
        Write-Host "[!] Could not check Docker containers" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Ready to Run Backend!" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "You can now run the backend using one of these methods:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Using Maven:" -ForegroundColor White
    Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Using IDE (IntelliJ IDEA):" -ForegroundColor White
    Write-Host "   - Open: backend/src/main/java/com/shop/ecommerce/EcommerceApplication.java" -ForegroundColor Gray
    Write-Host "   - Right-click → Run 'EcommerceApplication'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. After backend starts, check:" -ForegroundColor White
    Write-Host "   - Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Gray
    Write-Host "   - Health: http://localhost:8080/health" -ForegroundColor Gray
    Write-Host ""
    
    $runNow = Read-Host "Do you want to run the backend now? (y/n)"
    if ($runNow -eq "y" -or $runNow -eq "Y") {
        Write-Host ""
        Write-Host "Starting backend with Maven..." -ForegroundColor Cyan
        Write-Host "This may take a few minutes on first run..." -ForegroundColor Yellow
        Write-Host ""
        mvn spring-boot:run
    }
    
} else {
    Write-Host "Some tools are missing. Please install them first." -ForegroundColor Red
    Write-Host ""
    Write-Host "Missing tools:" -ForegroundColor Yellow
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host "  - Java 17+" -ForegroundColor Red
    }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Host "  - Maven" -ForegroundColor Red
    }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Host "  - Git" -ForegroundColor Red
    }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "  - Docker Desktop" -ForegroundColor Red
        Write-Host "    Install from: E:\BackendTools\DockerDesktopInstaller.exe" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "See installation guide: E:\BackendTools\INSTALLATION_GUIDE.txt" -ForegroundColor Yellow
}

Write-Host ""
