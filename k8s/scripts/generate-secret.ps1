# =============================================================================
# QT-AI MSA 로컬 k8s — qtai-secret 생성 (Windows / PowerShell)
#
# RS256 키쌍을 생성하고 DB 비밀번호를 받아 클러스터에 Secret을 직접 만든다.
# 평문 키는 파일로 저장하지 않고 kubectl로 바로 주입한다(.env/secret.yaml 미생성).
#
# 사용:  ./k8s/scripts/generate-secret.ps1
# 전제:  kubectl, openssl 이 PATH에 있어야 한다. 네임스페이스 qtai 가 먼저 있어야 한다
#        (kubectl apply -f k8s/00-namespace.yaml).
# =============================================================================
$ErrorActionPreference = "Stop"

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) { Write-Error "kubectl 가 필요합니다."; exit 1 }
# openssl — PATH 우선, 없으면 Git for Windows 동봉 openssl 탐색
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
if (-not $openssl) { Write-Error "openssl 을 찾을 수 없습니다."; exit 1 }

$dbPw   = Read-Host "DB_PASSWORD (qtai 사용자)"
$rootPw = Read-Host "MYSQL_ROOT_PASSWORD"
$deep   = Read-Host "DEEPSEEK_API_KEY (없으면 Enter)"

$tmp = Join-Path $env:TEMP ("qtai-k8s-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    # private는 PKCS#8(DER) 강제(genpkey -outform DER이 일부 빌드에서 PKCS#1을 내보내 Java 디코딩 실패).
    cmd /c "`"$openssl`" genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out `"$tmp\priv.pem`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl genpkey 실패(exit $LASTEXITCODE)" }
    cmd /c "`"$openssl`" pkcs8 -topk8 -nocrypt -in `"$tmp\priv.pem`" -outform DER -out `"$tmp\private.der`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl pkcs8(topk8) 실패(exit $LASTEXITCODE)" }
    cmd /c "`"$openssl`" rsa -in `"$tmp\priv.pem`" -pubout -outform DER -out `"$tmp\public.der`" 2>nul"
    if ($LASTEXITCODE -ne 0) { throw "openssl rsa(pubout) 실패(exit $LASTEXITCODE)" }
    $priv = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $tmp "private.der")))
    $pub  = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $tmp "public.der")))

    # 기존 Secret 있으면 교체
    kubectl -n qtai delete secret qtai-secret --ignore-not-found | Out-Null
    kubectl -n qtai create secret generic qtai-secret `
        --from-literal=DB_PASSWORD="$dbPw" `
        --from-literal=MYSQL_ROOT_PASSWORD="$rootPw" `
        --from-literal=JWT_PRIVATE_KEY="$priv" `
        --from-literal=JWT_PUBLIC_KEY="$pub" `
        --from-literal=DEEPSEEK_API_KEY="$deep"
    Write-Host "완료: qtai 네임스페이스에 qtai-secret 생성됨(평문 파일 미저장)."
}
finally {
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
