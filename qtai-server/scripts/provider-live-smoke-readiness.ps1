param(
    [switch]$AllowSkip
)

$ErrorActionPreference = "Stop"

$requiredEnvNames = @(
    "QTAI_AI_CLIENT_SERVICE_TOKEN",
    "QTAI_AI_CLIENT_QT_BASE_URL",
    "QTAI_AI_CLIENT_BIBLE_BASE_URL",
    "QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL",
    "QTAI_PROVIDER_SMOKE_QT_PASSAGE_ID",
    "QTAI_PROVIDER_SMOKE_QT_DATE",
    "QTAI_PROVIDER_SMOKE_BIBLE_VERSE_ID",
    "QTAI_PROVIDER_SMOKE_BIBLE_BATCH_VERSE_IDS",
    "QTAI_PROVIDER_SMOKE_BIBLE_BOOK",
    "QTAI_PROVIDER_SMOKE_BIBLE_CHAPTER",
    "QTAI_PROVIDER_SMOKE_BIBLE_START_VERSE",
    "QTAI_PROVIDER_SMOKE_BIBLE_END_VERSE",
    "QTAI_PROVIDER_SMOKE_ADMIN_MEMBER_ID",
    "QTAI_PROVIDER_SMOKE_ADMIN_ROLE",
    "QTAI_PROVIDER_SMOKE_ADMIN_ROLES"
)

if ($env:QTAI_PROVIDER_SMOKE_ENABLED -ne "true") {
    if ($AllowSkip) {
        Write-Host "Provider live smoke skipped. Set QTAI_PROVIDER_SMOKE_ENABLED=true to run live provider smoke."
        exit 0
    }

    throw "QTAI_PROVIDER_SMOKE_ENABLED=true is required. Use -AllowSkip only for readiness guard verification."
}

$missingEnvNames = @()
foreach ($name in $requiredEnvNames) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        $missingEnvNames += $name
    }
}

if ($missingEnvNames.Count -gt 0) {
    throw ("Missing required environment variable(s): " + ($missingEnvNames -join ", "))
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$qtaiServerDir = Resolve-Path (Join-Path $scriptDir "..")

Push-Location $qtaiServerDir
try {
    .\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest --rerun-tasks
} finally {
    Pop-Location
}
