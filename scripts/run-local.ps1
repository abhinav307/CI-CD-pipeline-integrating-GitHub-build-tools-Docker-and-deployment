$ErrorActionPreference = 'Stop'

Write-Host "=========================================="
Write-Host " Maven CI/CD Demo - Local Run Script"
Write-Host "=========================================="

Write-Host "`n[1/4] Building and testing Maven application..."
mvn -B clean package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }

Write-Host "`n[2/4] Building Docker container image..."
docker build -t maven-cicd-demo:local .
if ($LASTEXITCODE -ne 0) { throw "Docker image build failed." }

Write-Host "`n[3/4] Starting microservice container..."
docker rm -f ci-cd-pipeline-demo 2>$null | Out-Null
docker run -d --name ci-cd-pipeline-demo -p 8080:8080 maven-cicd-demo:local
if ($LASTEXITCODE -ne 0) { throw "Container launch failed." }

Write-Host "`n[4/4] Verifying health and HTTP endpoints..."
$ready = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8080/health" -TimeoutSec 2
        if ($res.status -eq "UP") {
            $ready = $true
            break
        }
    } catch {}
}

if (-not $ready) {
    docker logs ci-cd-pipeline-demo
    throw "Microservice failed health check on http://localhost:8080/health"
}

Write-Host "`nMicroservice is RUNNING and HEALTHY!"
Write-Host "------------------------------------------"
Write-Host "  Service URL:  http://localhost:8080/"
Write-Host "  Health Check: http://localhost:8080/health"
Write-Host "  Diagnostics:  http://localhost:8080/api/info"
Write-Host "------------------------------------------"
Write-Host "Container Name: ci-cd-pipeline-demo (visible & active in Docker Desktop)"
Write-Host "To stop container: docker stop ci-cd-pipeline-demo"
