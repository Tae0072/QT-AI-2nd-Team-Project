# =============================================================================
# QT-AI 원클릭 로컬 실행 — 클론 직후부터 Flutter 실행까지 (Windows / PowerShell)
#
#   .env(JWT키) -> 서비스 jar 빌드 -> Docker(MySQL/Redis/5서비스/게이트웨이)
#   -> DB 스키마+성경 seed -> (있으면)AI 해설 import -> Flutter 실행
#
# 사용(레포 루트 또는 어디서든):
#   powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
#
# 자주 쓰는 옵션:
#   -BackendOnly        Flutter 실행 없이 백엔드까지만
#   -Device <id>        Flutter 대상 기기 지정(미지정 시 자동 선택)
#   -AllDevices         연결된 모든 안드로이드 기기에 최신 빌드 설치(에뮬+실기기 동시 갱신)
#   -SkipImport         AI 해설 import 건너뜀
#   -Recreate           DB 볼륨까지 비우고 처음부터(주의: 로컬 DB 삭제)
#   -KakaoKey <key>     카카오 네이티브 앱 키(기본값은 AndroidManifest의 키)
#
# 전제: Docker Desktop 실행, JDK 21, Flutter, (실기기면)adb 가 PATH/SDK에 있어야 함.
# =============================================================================
[CmdletBinding()]
param(
    [switch]$BackendOnly,
    [string]$Device = '',
    [switch]$SkipImport,
    [switch]$Recreate,
    [switch]$SkipAdminWeb,
    [switch]$AllDevices,
    [string]$KakaoKey = '53e5afb2d90048af9e71332e47f387fa'
)
# 네이티브 도구(java/gradle/docker/flutter)는 stderr에 정상 출력을 쓰므로 'Stop'이면
# stderr 한 줄에 스크립트가 죽는다. 'Continue'로 두고 실패는 $LASTEXITCODE로 명시 처리한다.
$ErrorActionPreference = 'Continue'
$root      = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $root 'qtai-server'
$flutterDir= Join-Path $root 'flutter-app'
$migDir    = Join-Path $serverDir 'admin-server\src\main\resources\db\migration'
$importDir = Join-Path $root 'db-import'
$envFile   = Join-Path $root '.env'
$MYSQL     = 'qtai-mysql'

function Step($n,$msg){ Write-Host "`n[$n] $msg" -ForegroundColor Cyan }
function Info($m){ Write-Host "    $m" -ForegroundColor Gray }
function Ok($m){ Write-Host "    OK $m" -ForegroundColor Green }
function Warn($m){ Write-Host "    ! $m" -ForegroundColor Yellow }
function Die($m){ Write-Host "`n[중단] $m" -ForegroundColor Red; exit 1 }
function Have($c){ [bool](Get-Command $c -ErrorAction SilentlyContinue) }

# --- DB 헬퍼 (root 비밀번호는 컨테이너 env로만 참조, 스크립트에 노출 안 함) -----
function Sql-Scalar([string]$sql){
    $f = New-TemporaryFile
    [IO.File]::WriteAllText($f, $sql, (New-Object Text.UTF8Encoding $false))
    docker cp "$f" "${MYSQL}:/tmp/_q.sql" | Out-Null
    Remove-Item $f -Force
    $r = docker exec $MYSQL sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" qtai -N -s < /tmp/_q.sql 2>/dev/null'
    if ($LASTEXITCODE -ne 0) { return $null }
    return ($r | Select-Object -Last 1)
}
function Sql-File([string]$localPath){
    docker cp "$localPath" "${MYSQL}:/tmp/_run.sql" | Out-Null
    docker exec $MYSQL sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" qtai < /tmp/_run.sql'
    return $LASTEXITCODE
}

# --- .env 헬퍼 ---------------------------------------------------------------
function Set-EnvVar([string]$path,[string]$name,[string]$value){
    $lines = if (Test-Path $path) { @(Get-Content $path) } else { @() }
    $found = $false
    $out = foreach ($l in $lines) { if ($l -match "^\s*$name=") { $found=$true; "$name=$value" } else { $l } }
    if (-not $found) { $out = @($out) + "$name=$value" }
    [IO.File]::WriteAllLines($path, $out, (New-Object Text.UTF8Encoding $false))
}
function Test-EnvKey([string]$path){ (Test-Path $path) -and (Select-String -Path $path -Pattern '^\s*JWT_PRIVATE_KEY=\S' -Quiet) }

# JWT 키 보장: 이미 있으면 그대로, 없으면 host openssl → 없으면 Docker(mysql:8.0)로 생성.
# 순정 PC(Docker/JDK/Flutter만)에서도 openssl 설치 없이 동작하게 한다.
function Ensure-JwtKeys {
    if (Test-EnvKey $envFile) { Ok '.env JWT 키 존재 → 건너뜀'; return }
    if (-not (Test-Path $envFile)) { Copy-Item (Join-Path $root '.env.example') $envFile; Info '.env 생성(.env.example 기반)' }
    $ossl = (Get-Command openssl -ErrorAction SilentlyContinue).Source
    if (-not $ossl) {
        foreach ($c in @("$env:ProgramFiles\Git\usr\bin\openssl.exe","$env:ProgramFiles\Git\mingw64\bin\openssl.exe","D:\Git\usr\bin\openssl.exe")) {
            if (Test-Path $c) { $ossl = $c; break }
        }
    }
    if ($ossl) {
        Info 'JWT 키 생성 (host openssl)...'
        & (Join-Path $PSScriptRoot 'generate-keys.ps1')
        if (-not (Test-EnvKey $envFile)) { Die 'JWT 키 생성 실패(generate-keys.ps1).' }
        Ok '.env JWT 키 기록 완료 (host openssl)'
        return
    }
    Info 'openssl 미발견 → Docker(mysql:8.0)의 openssl로 JWT 키 생성...'
    # 큰따옴표를 쓰지 않는다(PowerShell→docker.exe 인자 전달 시 따옴표가 깨짐).
    # private(PKCS8 DER) base64 한 줄, echo로 줄바꿈, public(X509 DER) base64 한 줄.
    $gen = 'openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/k.pem 2>/dev/null; openssl pkcs8 -topk8 -nocrypt -in /tmp/k.pem -outform DER 2>/dev/null | base64 -w0; echo; openssl rsa -in /tmp/k.pem -pubout -outform DER 2>/dev/null | base64 -w0; echo'
    $out  = docker run --rm --entrypoint sh mysql:8.0 -c $gen
    $ls   = @($out | Where-Object { $_ -match '\S' })
    $priv = if ($ls.Count -ge 1) { $ls[0].Trim() } else { '' }
    $pub  = if ($ls.Count -ge 2) { $ls[1].Trim() } else { '' }
    if ($priv.Length -lt 100 -or $pub.Length -lt 50) { Die 'Docker 기반 JWT 키 생성 실패(mysql 이미지에 openssl 확인).' }
    $sb = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($sb)
    Set-EnvVar $envFile 'JWT_PRIVATE_KEY' $priv
    Set-EnvVar $envFile 'JWT_PUBLIC_KEY'  $pub
    Set-EnvVar $envFile 'SECURITY_JWT_SYSTEM_SECRET' ([Convert]::ToBase64String($sb))
    Ok '.env JWT 키 기록 완료 (Docker openssl)'
}

# =============================================================================
Write-Host "QT-AI 원클릭 실행 시작 (레포: $root)" -ForegroundColor Magenta

# --- [1] 사전 점검 -----------------------------------------------------------
Step 1 '사전 점검 (Docker / JDK21 / Flutter)'
if (-not (Have docker)) { Die 'docker 를 찾을 수 없습니다. Docker Desktop을 설치/실행하세요.' }
docker info *> $null
if ($LASTEXITCODE -ne 0) { Die 'Docker 데몬에 연결할 수 없습니다. Docker Desktop을 실행하세요.' }
Ok 'Docker 동작 중'
if (-not (Have java)) { Die 'java(JDK 21)를 찾을 수 없습니다.' }
# java -version 은 stderr로 출력 → cmd로 stdout 병합해 일반 문자열로 받는다(에러 레코드화 방지).
$jv = (cmd /c "java -version 2>&1" | Select-String 'version' | Select-Object -First 1)
if (-not $jv -or "$jv" -notmatch '"(\d+)') { Warn "Java 버전 확인 실패(계속 진행): $jv" }
elseif ([int]$Matches[1] -lt 17) { Die "JDK 17+ 필요(현재 $($Matches[1])). JDK 21 권장. JAVA_HOME을 21로 설정하세요." }
else { Ok "Java $($Matches[1])" }
if (-not $BackendOnly -and -not (Have flutter)) { Die 'flutter 를 찾을 수 없습니다(-BackendOnly로 백엔드만 실행 가능).' }

# --- [2] .env / JWT 키 (openssl 없으면 Docker로 생성) -------------------------
Step 2 '.env / JWT 키 준비'
Ensure-JwtKeys

# --- [3] 서비스 jar 빌드 ------------------------------------------------------
Step 3 '서비스 jar 빌드 (gradlew bootJar)'
Push-Location $serverDir
$built = $false
foreach ($attempt in 1..2) {
    & .\gradlew.bat bootJar --console=plain
    if ($LASTEXITCODE -eq 0) { $built = $true; break }
    Warn "빌드 실패(시도 $attempt). 스테일 출력/락 정리 후 재시도..."
    & .\gradlew.bat --stop *> $null
    Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Recurse -Directory -Filter build -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notlike '*\.gradle\*' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}
Pop-Location
if (-not $built) { Die 'jar 빌드 실패. 위 Gradle 오류를 확인하세요.' }
Ok '5개 서비스 jar 빌드 완료'

# --- [4] Docker 이미지 빌드 + 인프라 기동 ------------------------------------
Step 4 'Docker 이미지 빌드 + MySQL/Redis 기동'
Push-Location $root
if ($Recreate) { Warn 'DB 볼륨 포함 초기화(down -v)'; docker compose down -v *> $null }
docker compose build | Out-Host
if ($LASTEXITCODE -ne 0) { Pop-Location; Die 'Docker 이미지 빌드 실패(위 로그 확인).' }
docker compose up -d mysql redis | Out-Host
if ($LASTEXITCODE -ne 0) { Pop-Location; Die 'MySQL/Redis 기동 실패(위 로그 확인).' }
Pop-Location
Info 'MySQL healthy 대기...'
$deadline = (Get-Date).AddMinutes(2)
while ((docker inspect -f '{{.State.Health.Status}}' $MYSQL 2>$null) -ne 'healthy') {
    if ((Get-Date) -gt $deadline) { Die 'MySQL이 healthy 되지 않았습니다.' }
    Start-Sleep 3
}
Ok 'MySQL healthy'

# --- [5] DB 스키마 + 성경 seed (idempotent) ----------------------------------
Step 5 'DB 스키마 + 성경 seed'
$bibleCnt = Sql-Scalar 'SELECT COUNT(*) FROM bible_books;'   # 테이블 없으면 null
if ($bibleCnt -and [int]$bibleCnt -gt 0) {
    Ok "이미 시드됨 (bible_books=$bibleCnt) → 건너뜀"
} elseif ($null -ne $bibleCnt) {
    # 테이블은 있는데 데이터가 비어있는 반쪽 상태(이전 실행 잔여) → 직접 적용 시 충돌
    Die 'DB가 시드되지 않은 반쪽 상태입니다. `-Recreate` 옵션으로 다시 실행하세요.'
} else {
    Info 'Flyway 마이그레이션 SQL을 빈 DB에 버전 순서대로 적용...'
    $migFiles = Get-ChildItem (Join-Path $migDir 'V*.sql') |
        Sort-Object { [int]([regex]::Match($_.Name,'^V(\d+)__').Groups[1].Value) }
    foreach ($f in $migFiles) {
        docker cp "$($f.FullName)" "${MYSQL}:/tmp/run.sql" | Out-Null
        docker exec $MYSQL sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" qtai < /tmp/run.sql'
        if ($LASTEXITCODE -ne 0) { Die "마이그레이션 적용 실패: $($f.Name)" }
    }
    $bibleCnt = Sql-Scalar 'SELECT COUNT(*) FROM bible_books;'
    $verseCnt = Sql-Scalar 'SELECT COUNT(*) FROM bible_verses;'
    Ok "시드 완료 (bible_books=$bibleCnt, bible_verses=$verseCnt)"
}

# --- [6] 전체 스택 기동 + 게이트웨이 DNS 갱신 --------------------------------
Step 6 '전체 서비스 + 게이트웨이 기동'
Push-Location $root
docker compose up -d | Out-Host
Pop-Location
# 게이트웨이가 옛 서비스 IP를 캐시해 502 나는 것 방지 — 재시작으로 재resolve
docker restart qtai-gateway *> $null
Info '게이트웨이 라우팅 확인...'
$deadline = (Get-Date).AddMinutes(2); $routed = $false
while ((Get-Date) -lt $deadline) {
    $code = (curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8080/api/v1/bible/books" 2>$null)
    if ($code -eq '401' -or $code -eq '200') { $routed = $true; break }  # 401=서비스 도달(인증필요)
    Start-Sleep 4
}
if ($routed) { Ok '게이트웨이 → 서비스 라우팅 정상(8080)' } else { Warn '게이트웨이 라우팅 확인 실패(서비스 기동 지연일 수 있음). docker compose logs 확인.' }

# --- [7] AI 해설 import (db-import 있을 때만, idempotent) ---------------------
Step 7 'AI 해설 import'
if ($SkipImport) { Info '-SkipImport 지정 → 건너뜀' }
elseif (-not (Test-Path $importDir)) { Warn "db-import 폴더 없음 → 건너뜀(핸드오프 패키지를 $importDir 에 풀면 자동 적재)" }
else {
    $expl = Sql-Scalar 'SELECT COUNT(*) FROM verse_explanations;'
    if ($expl -and [int]$expl -gt 0) { Ok "이미 import됨 (verse_explanations=$expl) → 건너뜀" }
    else {
        Info 'preflight 체크리스트 버전 row 확인/추가...'
        $sql = @"
INSERT INTO ai_validation_checklist_versions (checklist_type, version, content_hash, status, activated_at)
SELECT 'EXPLANATION','2026.06.local-batch','local-batch-checklist-2026.06.09','ACTIVE',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM ai_validation_checklist_versions
  WHERE checklist_type='EXPLANATION' AND version='2026.06.local-batch' AND content_hash='local-batch-checklist-2026.06.09');
"@
        $tmp = New-TemporaryFile; [IO.File]::WriteAllText($tmp,$sql,(New-Object Text.UTF8Encoding $false))
        [void](Sql-File $tmp); Remove-Item $tmp -Force
        Info '권별 SQL 순차 적용(각 파일 별도 세션)...'
        $books = Get-ChildItem (Join-Path $importDir 'sql-books\*_bundle.sql') | Sort-Object Name
        $i = 0
        foreach ($b in $books) {
            $i++; docker cp "$($b.FullName)" "${MYSQL}:/tmp/run.sql" | Out-Null
            docker exec $MYSQL sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" qtai < /tmp/run.sql'
            if ($LASTEXITCODE -ne 0) { Die "import 실패: $($b.Name)" }
            Write-Progress -Activity 'AI 해설 import' -Status "$($b.Name)" -PercentComplete (($i/$books.Count)*100)
        }
        Write-Progress -Activity 'AI 해설 import' -Completed
        $expl = Sql-Scalar 'SELECT COUNT(*) FROM verse_explanations;'
        Ok "import 완료 (verse_explanations=$expl, 66권)"
    }
}

# --- [7a] dev 관리자 전체 권한(SUPER_ADMIN) 보장 -------------------------------
# dev 로컬 관리자(members.role=ADMIN)에 연결된 admin_users.admin_role을 SUPER_ADMIN으로
# 동기화한다. SUPER_ADMIN은 백엔드 verifyAnyRole에서 모든 세부권한을 통과하므로,
# REVIEWER/CONTENT_CREATOR 전용 API에서도 AD0003(권한 부족)이 나지 않는다.
# Flyway 재적용 여부와 무관하게 매 실행 시 DB에 직접 반영한다(멱등).
Step '7a' 'dev 관리자 권한 SUPER_ADMIN 동기화'
$adminRoleSql = @"
UPDATE admin_users SET admin_role='SUPER_ADMIN', status='ACTIVE', deleted_at=NULL
WHERE member_id IN (SELECT id FROM members WHERE role='ADMIN');
"@
$arDeadline = (Get-Date).AddSeconds(60); $arDone = $false
while ((Get-Date) -lt $arDeadline -and -not $arDone) {
    $hasTbl = Sql-Scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='admin_users';"
    if ($hasTbl -and [int]$hasTbl -gt 0) {
        $arTmp = New-TemporaryFile; [IO.File]::WriteAllText($arTmp, $adminRoleSql, (New-Object Text.UTF8Encoding $false))
        $arRc = Sql-File $arTmp; Remove-Item $arTmp -Force
        if ($arRc -eq 0) {
            $arCnt = Sql-Scalar "SELECT COUNT(*) FROM admin_users WHERE admin_role='SUPER_ADMIN';"
            Ok "관리자 SUPER_ADMIN 동기화 완료(SUPER_ADMIN 계정 수=$arCnt)"
            $arDone = $true
        }
    }
    if (-not $arDone) { Start-Sleep 3 }
}
if (-not $arDone) { Warn 'admin_users 준비 전이라 권한 동기화 건너뜀(admin-server 기동 후 dev-up 재실행 시 적용).' }

# --- [7b] 관리자 웹(admin-web) dev 서버 기동 -----------------------------------
# Vite dev 서버(5173)를 별도 창에서 띄운다. 이어지는 flutter run이 이 터미널을 점유하므로
# 관리자 웹은 비차단(새 창)으로 실행한다. 브라우저에서 http://localhost:5173 으로 접속.
# admin-web의 /api 요청은 vite proxy가 admin-server(8090)로 전달한다(vite.config.ts).
if (-not $SkipAdminWeb) {
    Step '7b' '관리자 웹 dev 서버 기동 (http://localhost:5173)'
    $adminDir = Join-Path $root 'admin-web'
    $npmCmd = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
    if (-not $npmCmd) { $npmCmd = (Get-Command npm -ErrorAction SilentlyContinue).Source }
    if (-not (Test-Path $adminDir)) { Warn 'admin-web 폴더 없음 → 건너뜀' }
    elseif (-not $npmCmd) { Warn 'npm 미발견 → 관리자 웹 건너뜀(Node.js 설치 필요)' }
    else {
        # 의존성 점검: node_modules가 없거나, 핵심 패키지(antd)가 불완전하면 깨끗이 재설치한다.
        # (과거 'npm install'이 부분 설치로 antd 진입점을 빠뜨려 vite가 실패한 사례 대응.)
        $nm = Join-Path $adminDir 'node_modules'
        $antdEntry = Join-Path $adminDir 'node_modules\antd\es\index.js'
        $needInstall = (-not (Test-Path $nm)) -or (-not (Test-Path $antdEntry))
        # vite 의존성 캐시는 매번 비워 stale 캐시로 인한 해석 오류를 막는다(가볍다).
        $viteCache = Join-Path $nm '.vite'
        if (Test-Path $viteCache) { Remove-Item -Recurse -Force $viteCache -ErrorAction SilentlyContinue }
        if ($needInstall) {
            Warn 'admin-web 의존성 없음/불완전 → 깨끗이 재설치합니다(최초 1회, 시간이 걸릴 수 있음)...'
            Push-Location $adminDir
            if (Test-Path $nm) { Remove-Item -Recurse -Force $nm -ErrorAction SilentlyContinue }
            if (Test-Path (Join-Path $adminDir 'package-lock.json')) {
                & $npmCmd ci --no-fund --no-audit | Out-Host
            } else {
                & $npmCmd install --no-fund --no-audit | Out-Host
            }
            Pop-Location
            if (-not (Test-Path $antdEntry)) { Warn '의존성 설치 후에도 antd가 확인되지 않습니다. admin-web에서 npm ci를 수동 실행하세요.' }
            else { Ok 'admin-web 의존성 설치 완료' }
        }
        # 이미 5173이 떠 있으면 중복 실행하지 않는다.
        $alive = $false
        try { $null = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:5173' -TimeoutSec 2; $alive = $true } catch { $alive = $false }
        if ($alive) {
            Ok '관리자 웹 이미 실행 중 → 브라우저에서 http://localhost:5173 접속'
        } else {
            # 새 창에서 vite dev 실행(비차단). --strictPort로 포트(5173)를 고정한다.
            Start-Process -FilePath $npmCmd -ArgumentList 'run','dev','--','--strictPort' -WorkingDirectory $adminDir -WindowStyle Normal | Out-Null
            $deadline = (Get-Date).AddSeconds(40); $ready = $false
            while ((Get-Date) -lt $deadline -and -not $ready) {
                Start-Sleep 2
                try { if ((Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:5173' -TimeoutSec 2).StatusCode -ge 200) { $ready = $true } } catch {}
            }
            if ($ready) { Ok '관리자 웹 기동 완료 → 브라우저에서 http://localhost:5173 접속' }
            else { Warn '관리자 웹 기동 확인 실패(새 창 로그 확인). 잠시 후 http://localhost:5173 접속' }
        }
    }
}

# --- [8] Flutter 실행 --------------------------------------------------------
if ($BackendOnly) {
    Step 8 '완료(백엔드)'
    Ok '백엔드 준비 완료 → 게이트웨이 http://localhost:8080'
    Info "Flutter는 다음으로: flutter run (DEV_BASE_URL/KAKAO 키는 README 참고)"
    return
}
Step 8 'Flutter 실행 준비'
# l10n 폴더 ReadOnly 속성 제거(Windows 폴더 표식 때문에 gen_l10n이 실패하는 문제 회피)
foreach ($p in @((Join-Path $flutterDir 'lib'), (Join-Path $flutterDir 'lib\l10n'))) {
    if (Test-Path $p) { try { (Get-Item $p).Attributes = [IO.FileAttributes]::Directory } catch {} }
}
# 대상 기기 선택 — cascade: 실기기 → 실행중 에뮬레이터 → AVD 자동 실행 → 웹(브라우저)
function Get-FDevices { try { return @(flutter devices --machine 2>$null | ConvertFrom-Json) } catch { return @() } }
$devs = Get-FDevices
if (-not $Device) {
    $phys = $devs | Where-Object { $_.targetPlatform -like 'android*' -and -not $_.emulator } | Select-Object -First 1
    $emu  = $devs | Where-Object { $_.emulator -eq $true } | Select-Object -First 1
    if     ($phys) { $Device = $phys.id; Info '실기기(USB) 사용' }
    elseif ($emu)  { $Device = $emu.id;  Info '실행 중인 에뮬레이터 사용' }
    else {
        # 실행 중 기기 없음 → 등록된 AVD가 있으면 자동 실행 후 부팅 대기
        $emuExe = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
        $avd = if ($emuExe) { (& $emuExe -list-avds 2>$null | Where-Object { $_ -match '\S' } | Select-Object -First 1) } else { $null }
        if ($avd) {
            Info "사용 중인 기기 없음 → 에뮬레이터 자동 실행: $avd (부팅 대기 최대 180초)..."
            flutter emulators --launch $avd *> $null
            $deadline = (Get-Date).AddSeconds(180)
            while ((Get-Date) -lt $deadline -and -not $Device) {
                Start-Sleep 5
                $e2 = (Get-FDevices | Where-Object { $_.emulator -eq $true } | Select-Object -First 1)
                if ($e2) { $Device = $e2.id; Ok "에뮬레이터 준비됨: $Device" }
            }
            if (-not $Device) { Warn '에뮬레이터 부팅 확인 실패 → 웹으로 폴백' }
        }
        if (-not $Device) {
            $web = Get-FDevices | Where-Object { $_.id -in @('chrome','edge') } | Select-Object -First 1
            if ($web) { $Device = $web.id; Info "브라우저로 폴백: $Device" }
            else { Die '사용 가능한 실기기/에뮬레이터/브라우저가 없습니다. (Chrome 설치 또는 USB 기기 연결)' }
        }
    }
}
$sel = (Get-FDevices) | Where-Object { $_.id -eq $Device } | Select-Object -First 1
Info "대상 기기: $Device"

# adb 실행 파일 경로 1회 해석.
function Resolve-Adb {
    $a = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
    if (-not $a) { $a = (Get-Command adb -ErrorAction SilentlyContinue).Source }
    return $a
}
# 기기별 백엔드 주소(dart-define) 산출 + 실기기면 adb reverse(USB 8080 터널)까지 설정한다.
# - 웹/에뮬레이터: 앱 기본값(localhost / 10.0.2.2) 사용.
# - 실기기(USB): adb -s <id> reverse + DEV_BASE_URL=localhost. (여러 기기 연결 대비 -s 필수)
function Get-DeviceDefines($selDev, $devId, $adbPath) {
    $defs = @("--dart-define=KAKAO_NATIVE_APP_KEY=$KakaoKey",
              '--dart-define=DEV_MODE_PASSWORD=qtai-admin-1234')  # [DEV_MODE] 설정>버전 5탭 진입 비번
    $w = ($selDev -and $selDev.targetPlatform -like 'web*') -or ($devId -match 'chrome|edge|web')
    $e = ($selDev -and $selDev.emulator -eq $true) -or ($devId -match '^emulator-')
    if ($w -or $e) { return $defs }
    if ($adbPath) { & $adbPath -s $devId reverse tcp:8080 tcp:8080 *> $null }
    else { Warn 'adb 미발견 → 같은 Wi-Fi에서 DEV_BASE_URL을 PC LAN IP로 직접 지정하세요.' }
    $defs += '--dart-define=DEV_BASE_URL=http://localhost:8080/api/v1'
    return $defs
}

$adbExe   = Resolve-Adb
$ddefines = Get-DeviceDefines $sel $Device $adbExe
$isWeb = ($sel -and $sel.targetPlatform -like 'web*') -or ($Device -match 'chrome|edge|web')
$isEmu = ($sel -and $sel.emulator -eq $true) -or ($Device -match '^emulator-')
if     ($isWeb) { Info '웹 → localhost 직접 접근(추가 설정 없음)' }
elseif ($isEmu) { Info '안드로이드 에뮬레이터 → 앱 기본값 10.0.2.2 사용(추가 설정 없음)' }
else            { Ok '실기기 → adb reverse 8080(USB 터널) + DEV_BASE_URL=localhost' }

# --- [8b] (-AllDevices) 연결된 모든 안드로이드 기기에 최신 빌드 설치 -------------
# flutter run은 기기 하나만 갱신하므로, 나머지 기기엔 옛 빌드가 남아 화면이 달라진다(구버전처럼 보임).
# -AllDevices면 주(主) 기기를 제외한 모든 안드로이드 기기에 같은 코드를 빌드·설치·실행한다.
# 기기 종류에 맞는 백엔드 주소로 각각 빌드하므로 빌드가 기기 수만큼 반복되어 시간이 더 걸린다.
if ($AllDevices) {
    Step '8b' '-AllDevices: 보조 기기에 최신 빌드 설치'
    $apkPath = Join-Path $flutterDir 'build\app\outputs\flutter-apk\app-debug.apk'
    $others = @(Get-FDevices | Where-Object { $_.id -ne $Device -and ($_.targetPlatform -like 'android*') })
    if ($others.Count -eq 0) { Info '추가 안드로이드 기기 없음 → 건너뜀' }
    foreach ($d in $others) {
        Info "[보조] $($d.name) ($($d.id)) 빌드+설치..."
        $defs2 = Get-DeviceDefines $d $d.id $adbExe
        Push-Location $flutterDir
        & flutter build apk --debug @defs2
        $rc = $LASTEXITCODE
        Pop-Location
        if ($rc -ne 0) { Warn "[보조] 빌드 실패 → 건너뜀: $($d.id)"; continue }
        if (-not $adbExe) { Warn '[보조] adb 미발견 → 설치 건너뜀'; continue }
        & $adbExe -s $d.id install -r $apkPath *> $null
        if ($LASTEXITCODE -ne 0) { Warn "[보조] 설치 실패: $($d.id)"; continue }
        & $adbExe -s $d.id shell monkey -p com.qtai.qtai_app -c android.intent.category.LAUNCHER 1 *> $null
        Ok "[보조] 설치+실행 완료: $($d.id)"
    }
}
Step 9 "flutter run -d $Device"
# Windows: build 폴더 내 파일의 ReadOnly 표식 때문에 gradle의 cleanMergeDebugAssets가
# 그 파일을 못 지워 'Unable to delete directory ...mergeDebugAssets'로 실패한다
# (라이브 프로세스 락이 아니라 ReadOnly 속성 문제 — 진단으로 확인).
# 빌드 직전 build 트리의 ReadOnly 속성을 모두 제거한다(증분 빌드는 보존).
$fb = Join-Path $flutterDir 'build'
if (Test-Path $fb) {
    Get-ChildItem $fb -Recurse -Force -ErrorAction SilentlyContinue |
        Where-Object { $_.Attributes -band [IO.FileAttributes]::ReadOnly } |
        ForEach-Object { try { $_.Attributes = ($_.Attributes -band (-bnot [IO.FileAttributes]::ReadOnly)) } catch {} }
}
# 혹시 이전 flutter run(dart) 세션이 살아있으면 정리(있을 때만)
Get-Process dart -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Push-Location $flutterDir
flutter run -d $Device @ddefines
Pop-Location
