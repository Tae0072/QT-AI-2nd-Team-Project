$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$qtaiServerDir = Resolve-Path (Join-Path $scriptDir "..\..")

Push-Location $qtaiServerDir
try {
    .\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.internal.AiGenerationWorkerRuntimeSmokeReadinessTest --rerun-tasks
} finally {
    Pop-Location
}
