# run-dev-web.ps1
# Docker(mysql/redis) -> qtai-server(dev 프로파일) -> Flutter 웹 을 순서대로 실행한다.
#
# 사용(레포 루트에서):
#   powershell -ExecutionPolicy Bypass -File .\run-dev-web.ps1
#
# 종료: Flutter 창에서 q 입력 → 서버 새 창 닫기 → `docker compose down`
#
# 전제: Docker Desktop 실행 중, flutter/JDK가 PATH에 있어야 함.
#       WEB_DEV_USER_ID(기본 1)는 DB에 존재하는 회원 id여야 데이터가 보인다.

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

function Test-Port($p) {
    try { $c = New-Object Net.Sockets.TcpClient; $c.Connect("localhost", $p); $c.Close(); return $true }
    catch { return $false }
}

Write-Host "[1/3] Docker mysql/redis 시작..." -ForegroundColor Cyan
docker compose -f "$root\docker-compose.yml" up -d mysql redis

Write-Host "      MySQL healthy 대기..." -ForegroundColor Cyan
while ((docker inspect -f '{{.State.Health.Status}}' qtai-mysql 2>$null) -ne "healthy") {
    Start-Sleep -Seconds 2
}

Write-Host "      .env 로드 (JWT 키 등 환경변수 주입)..." -ForegroundColor Cyan
if (Test-Path "$root\.env") {
    foreach ($line in (Get-Content "$root\.env")) {
        if ($line -match '^\s*#' -or -not $line.Contains('=')) { continue }
        $idx  = $line.IndexOf('=')
        $name = $line.Substring(0, $idx).Trim()
        $val  = $line.Substring($idx + 1).Trim()
        if ($name) { Set-Item -Path "env:$name" -Value $val }
    }
    if (-not $env:JWT_PRIVATE_KEY -or $env:JWT_PRIVATE_KEY.Length -lt 100) {
        Write-Host "  경고: JWT_PRIVATE_KEY 로드 실패(길이=$($env:JWT_PRIVATE_KEY.Length)). 'bash run-dev-web.sh' 사용을 권장합니다." -ForegroundColor Yellow
    }
} else {
    Write-Host "  경고: .env 없음 → 서버가 JWT 키 없이 못 뜹니다. Git Bash로 run-dev-web.sh를 한 번 실행하면 .env가 자동 생성됩니다." -ForegroundColor Yellow
}

Write-Host "[2/3] 서버(dev 프로파일) 새 창에서 시작..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList @(
    "-NoExit", "-Command",
    "cd '$root\qtai-server'; `$env:SPRING_PROFILES_ACTIVE='dev'; .\gradlew.bat bootRun"
)

Write-Host "      서버 포트 8080 대기 (최초 빌드면 수십 초 걸릴 수 있음)..." -ForegroundColor Cyan
while (-not (Test-Port 8080)) { Start-Sleep -Seconds 3 }

Write-Host "[3/3] Flutter 웹 시작 (이 창)..." -ForegroundColor Cyan
Set-Location "$root\flutter-app"
flutter run -d chrome --web-port=3000 --dart-define=WEB_DEV_NO_LOGIN=true --dart-define=WEB_DEV_USER_ID=1
