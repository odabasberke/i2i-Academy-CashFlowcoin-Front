# Loads .env into this PowerShell session's environment, then starts the
# Spring Boot app. Run from the project root:
#
#   .\run-backend.ps1
#
# If Windows blocks the script ("running scripts is disabled on this
# system"), either right-click -> Run with PowerShell, or run once first:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

if (-not (Test-Path ".env")) {
    Write-Error ".env not found. Copy .env.example to .env and fill in real values first."
    exit 1
}

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $parts = $line.Split("=", 2)
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

Write-Host "Environment loaded from .env. Starting Spring Boot..." -ForegroundColor Cyan
mvn spring-boot:run
