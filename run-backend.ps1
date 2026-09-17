$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $repoRoot '.env'
$backendDir = Join-Path $repoRoot 'backend'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw 'No se encontro el archivo .env en la raiz del repositorio.'
}

$lineNumber = 0
foreach ($rawLine in Get-Content -LiteralPath $envFile) {
    $lineNumber++
    if ([string]::IsNullOrWhiteSpace($rawLine) -or $rawLine.TrimStart().StartsWith('#')) {
        continue
    }

    if ($rawLine -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        throw "Formato invalido en .env, linea $lineNumber."
    }

    $name = $Matches[1]
    $value = $Matches[2].Trim()
    if ($value.Length -ge 2 -and
        (($value.StartsWith('"') -and $value.EndsWith('"')) -or
         ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

Push-Location -LiteralPath $backendDir
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "El backend termino con codigo $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
