# =============================================================================
# Link a local AI review reference index to the Docker dev stack.
#
# Usage:
#   .\scripts\setup-ai-review-reference.ps1 -SourceIndex C:\path\reference-index.json
#   .\scripts\setup-ai-review-reference.ps1
#
# The first form copies the source file into:
#   qtai-server\restricted\validation\index\reference-index.json
#
# Then the script creates an ACTIVE validation_reference_jobs row pointing at:
#   restricted://validation/index/reference-index.json
#
# The index file itself is ignored by git. Do not commit restricted reference data.
# =============================================================================
[CmdletBinding()]
param(
    [string]$SourceIndex,
    [string]$TargetIndex,
    [string]$IndexStorageUri = "restricted://validation/index/reference-index.json",
    [string]$SourceName = "AI_REVIEW_REFERENCE_INDEX",
    [string]$SourceFileName = "reference-index.json",
    [switch]$SkipDatabase
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
if (-not $TargetIndex) {
    $TargetIndex = Join-Path $root "qtai-server\restricted\validation\index\reference-index.json"
}

if ($SourceIndex) {
    $resolvedSource = Resolve-Path -LiteralPath $SourceIndex
    $targetDir = Split-Path -Parent $TargetIndex
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Copy-Item -LiteralPath $resolvedSource -Destination $TargetIndex -Force
    Write-Host "Copied reference index to $TargetIndex"
}

if (-not (Test-Path -LiteralPath $TargetIndex)) {
    throw "Reference index not found: $TargetIndex. Pass -SourceIndex or place the file first."
}

$index = Get-Content -LiteralPath $TargetIndex -Raw -Encoding UTF8 | ConvertFrom-Json
if ($index.schemaVersion -ne "ai-review-reference-index.v1") {
    throw "Unsupported schemaVersion: $($index.schemaVersion)"
}
if (-not $index.sourceFileHash) {
    throw "sourceFileHash is required in reference-index.json"
}
$entryCount = @($index.entries).Count
if ($entryCount -lt 1) {
    throw "reference-index.json must contain at least one entry"
}

Write-Host "Reference index ready"
Write-Host "  schemaVersion: $($index.schemaVersion)"
Write-Host "  sourceFileHash: $($index.sourceFileHash)"
Write-Host "  entries: $entryCount"
Write-Host "  indexStorageUri: $IndexStorageUri"

if ($SkipDatabase) {
    Write-Host "Skipped DB setup."
    exit 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found. Start Docker Desktop and retry."
}

$mysqlRunning = docker inspect -f "{{.State.Running}}" qtai-mysql 2>$null
if ($LASTEXITCODE -ne 0 -or $mysqlRunning.Trim() -ne "true") {
    throw "qtai-mysql container is not running. Start the stack with docker compose up -d."
}

function SqlLiteral([string]$Value) {
    return "'" + ($Value -replace "'", "''") + "'"
}

$sourceNameSql = SqlLiteral $SourceName
$sourceFileNameSql = SqlLiteral $SourceFileName
$sourceFileHashSql = SqlLiteral $index.sourceFileHash
$indexStorageUriSql = SqlLiteral $IndexStorageUri

$sql = @"
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @source_name = $sourceNameSql;
SET @source_file_name = $sourceFileNameSql;
SET @source_file_hash = $sourceFileHashSql;
SET @index_storage_uri = $indexStorageUriSql;

UPDATE validation_reference_jobs
   SET status = 'EXPIRED',
       updated_at = NOW(6)
 WHERE status = 'ACTIVE'
   AND NOT (source_file_hash = @source_file_hash AND index_storage_uri = @index_storage_uri);

INSERT INTO validation_reference_jobs (
    source_name,
    source_file_name,
    source_file_hash,
    storage_uri,
    index_storage_uri,
    status,
    expires_at,
    deleted_at,
    created_at,
    updated_at
)
SELECT
    @source_name,
    @source_file_name,
    @source_file_hash,
    NULL,
    @index_storage_uri,
    'ACTIVE',
    NULL,
    NULL,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
      FROM validation_reference_jobs
     WHERE status = 'ACTIVE'
       AND source_file_hash = @source_file_hash
       AND index_storage_uri = @index_storage_uri
);

SELECT id, status, source_name, source_file_hash, index_storage_uri, created_at
  FROM validation_reference_jobs
 WHERE status = 'ACTIVE'
 ORDER BY id DESC;
"@

$sql | docker exec -i qtai-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" --default-character-set=utf8mb4 -N -B'
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create validation_reference_jobs ACTIVE row"
}

Write-Host "DB setup complete."
