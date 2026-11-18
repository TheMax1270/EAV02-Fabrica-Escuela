$ErrorActionPreference = 'Stop'

$frontendPath = Join-Path $PSScriptRoot 'frontend'
$backendPath = Join-Path $PSScriptRoot 'backend'

Push-Location $frontendPath
try {
    Write-Host '== Frontend: lint =='
    npm.cmd run lint
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend lint failed with exit code $LASTEXITCODE."
    }

    Write-Host '== Frontend: tests and coverage =='
    npm.cmd test
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend tests failed with exit code $LASTEXITCODE."
    }

    Write-Host '== Frontend: production build =='
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Push-Location $backendPath
try {
    Write-Host '== Backend: tests and verification =='
    .\mvnw.cmd -B verify
    if ($LASTEXITCODE -ne 0) {
        throw "Backend verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Write-Host 'All frontend and backend checks passed.'
