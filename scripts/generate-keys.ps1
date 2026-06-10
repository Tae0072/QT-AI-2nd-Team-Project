# =============================================================================
# QT-AI MSA 로컬 배포 — JWT RS256 키 + .env 생성 스크립트 (Windows / PowerShell)
#
# - RS256 2048bit 키쌍을 생성해 Base64(private=PKCS8, public=X509)로 .env에 기록한다.
# - .env가 이미 있으면 JWT_* 값만 갱신하고, 없으면 .env.example을 복사해 만든다.
# - 평문 키는 .env(gitignore)에만 저장되고 저장소에 커밋되지 않는다.
#
# 사용:  ./scripts/generate-keys.ps1
# 전제:  openssl 이 PATH에 있어야 한다(Git for Windows 동봉 openssl 사용 가능).
# =============================================================================
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
$envExample = Join-Path $root ".env.example"

# openssl 확인 — PATH 우선, 없으면 Git for Windows 동봉 openssl 탐색
$openssl = (Get-Command openssl -ErrorAction SilentlyContinue).Source
if (-not $openssl) {
    foreach ($cand in @(
        "$env:ProgramFiles\Git\usr\bin\openssl.exe",
        "$env:ProgramFiles\Git\mingw64\bin\openssl.exe",
        "D:\Git\usr\bin\openssl.exe",
        "D:\Git\mingw64\bin\openssl.exe")) {
        if (Test-Path $cand) { $openssl = $cand; break }
    }
}
if (-not $openssl) {
    Write-Error "openssl을 찾을 수 없습니다. Git for Windows의 openssl 경로를 PATH에 추가하거나 설치하세요."
    exit 1
}

$tmp = Join-Path $env:TEMP ("qtai-jwt-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    Write-Host "RS256 2048bit 키쌍 생성 중..."
    # 네이티브 명령 stderr가 PowerShell 예외로 처리되지 않도록 cmd /c 로 호출하고 종료코드를 확인한다.
    # private는 반드시 PKCS#8(DER)로 만든다 — 일부 openssl 빌드의 `genpkey -outform DER`은
    # PKCS#1을 내보내 Java PKCS8EncodedKeySpec 디코딩이 실패한다. pkcs8 -topk8 로 변환을 강제한다.
    cmd /c "`"$openssl`" genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out `"$tmp\priv.pem`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl genpkey 실패(exit $LASTEXITCODE)" }
    cmd /c "`"$openssl`" pkcs8 -topk8 -nocrypt -in `"$tmp\priv.pem`" -outform DER -out `"$tmp\private.der`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl pkcs8(topk8) 실패(exit $LASTEXITCODE)" }
    cmd /c "`"$openssl`" rsa -in `"$tmp\priv.pem`" -pubout -outform DER -out `"$tmp\public.der`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl rsa(pubout) 실패(exit $LASTEXITCODE)" }

    $priv = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $tmp "private.der")))
    $pub  = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $tmp "public.der")))
    $sysBytes = New-Object byte[] 48
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($sysBytes)
    $sys = [Convert]::ToBase64String($sysBytes)

    # .env 준비
    if (-not (Test-Path $envFile)) {
        Copy-Item $envExample $envFile
        Write-Host ".env 생성(.env.example 기반)."
    }

    $lines = Get-Content $envFile
    $hasPriv = $false; $hasPub = $false; $hasSys = $false
    $out = foreach ($l in $lines) {
        if ($l -match '^\s*JWT_PRIVATE_KEY=') { $hasPriv = $true; "JWT_PRIVATE_KEY=$priv" }
        elseif ($l -match '^\s*JWT_PUBLIC_KEY=') { $hasPub = $true; "JWT_PUBLIC_KEY=$pub" }
        elseif ($l -match '^\s*SECURITY_JWT_SYSTEM_SECRET=') { $hasSys = $true; "SECURITY_JWT_SYSTEM_SECRET=$sys" }
        else { $l }
    }
    if (-not $hasPriv) { $out += "JWT_PRIVATE_KEY=$priv" }
    if (-not $hasPub)  { $out += "JWT_PUBLIC_KEY=$pub" }
    if (-not $hasSys) { $out += "SECURITY_JWT_SYSTEM_SECRET=$sys" }

    # UTF-8 (BOM 없음)로 기록
    [IO.File]::WriteAllLines($envFile, $out, (New-Object Text.UTF8Encoding $false))
    Write-Host "완료: .env 의 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY / SECURITY_JWT_SYSTEM_SECRET 갱신됨."
    Write-Host "DB 비밀번호 등 나머지 값도 .env 에서 확인/변경하세요."
}
finally {
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
