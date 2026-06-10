#!/usr/bin/env bash
# Docker(mysql/redis) -> qtai-server(dev 프로파일) -> Flutter 웹 순서 실행 (Git Bash/MINGW64)
#
# 사용(레포 루트에서):  bash run-dev-web.sh
# 종료: Flutter에서 q → 서버 종료(아래 안내) → docker compose down
#
# 전제: Docker Desktop 실행 중, flutter/JDK가 PATH에 있어야 함.
#       WEB_DEV_USER_ID(기본 1)는 DB에 존재하는 회원 id여야 데이터가 보인다.
set -e
cd "$(dirname "$0")"
ROOT="$(pwd)"

echo "[1/3] Docker mysql/redis 시작..."
docker compose up -d mysql redis

echo "      MySQL healthy 대기..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' qtai-mysql 2>/dev/null)" = "healthy" ]; do sleep 2; done

if [ ! -f "$ROOT/.env" ]; then
  echo "      .env 없음 → dev용 JWT 키 생성(PKCS#8/X509, throwaway, 커밋 안 됨)..."
  TMP=$(mktemp -d)
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP/priv.pem" 2>/dev/null
  # 핵심: JwtProvider는 PKCS#8을 기대 → -topk8로 강제 변환 (genpkey -outform DER는 환경에 따라 PKCS#1이 나옴)
  openssl pkcs8 -topk8 -nocrypt -in "$TMP/priv.pem" -outform DER -out "$TMP/p.der" 2>/dev/null
  openssl rsa -in "$TMP/priv.pem" -pubout -outform DER -out "$TMP/pub.der" 2>/dev/null
  { echo "JWT_PRIVATE_KEY=$(base64 -w0 "$TMP/p.der")"; echo "JWT_PUBLIC_KEY=$(base64 -w0 "$TMP/pub.der")"; echo "DEEPSEEK_API_KEY="; } > "$ROOT/.env"
  rm -rf "$TMP"
fi
echo "      .env 로드 (JWT 키 등)..."
set -a; . "$ROOT/.env"; set +a

echo "[2/3] 서버(dev 프로파일) 백그라운드 시작... (로그: qtai-server/dev-server.log)"
( cd "$ROOT/qtai-server" && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun > dev-server.log 2>&1 & )
echo "      서버 PID: $!  (종료: kill $!  또는  로그 확인: tail -f qtai-server/dev-server.log)"

echo "      서버 포트 8080 대기 (최초 빌드면 수십 초 걸릴 수 있음)..."
until curl -s -o /dev/null "http://localhost:8080/api/v1/auth/kakao"; do sleep 3; done

echo "[3/3] Flutter 웹 시작..."
cd "$ROOT/flutter-app"
flutter run -d chrome --web-port=3000 --dart-define=WEB_DEV_NO_LOGIN=true --dart-define=WEB_DEV_USER_ID=1
