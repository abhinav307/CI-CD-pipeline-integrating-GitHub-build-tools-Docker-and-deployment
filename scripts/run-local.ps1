$ErrorActionPreference = 'Stop'

Write-Host 'Building and testing the Maven application...'
mvn -B clean package
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

Write-Host 'Building the Docker image...'
docker build -t maven-cicd-demo:local .
if ($LASTEXITCODE -ne 0) { throw 'Docker image build failed.' }

Write-Host 'Running the container...'
docker run --rm maven-cicd-demo:local
if ($LASTEXITCODE -ne 0) { throw 'Container execution failed.' }
