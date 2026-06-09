#!/usr/bin/env bash
# =============================================================================
# QT-AI MSA 로컬 배포 — JWT RS256 키 + .env 생성 스크립트 (Linux / macOS)
#
# - RS256 2048bit 키쌍을 생성해 Base64(private=PKCS8, public=X509)로 .env에 기록한다.
# - .env가 이미 있으면 JWT_* 값만 갱신하고, 없으면 .env.example을 복사해 만든다.
# - 평문 키는 .env(gitignore)에만 저장되고 저장소에 커밋되지 않는다.
#
# 사용:  ./scripts/generate-keys.sh
# 전제:  openssl, base64 가 설치되어 있어야 한다.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"
ENV_EXAMPLE="$ROOT/.env.example"

command -v openssl >/dev/null 2>&1 || { echo "openssl이 필요합니다."; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "RS256 2048bit 키쌍 생성 중..."
# private는 반드시 PKCS#8(DER) — 일부 openssl 빌드의 genpkey -outform DER은 PKCS#1을 내보내
# Java PKCS8EncodedKeySpec 디코딩이 실패한다. pkcs8 -topk8 로 변환을 강제한다.
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP/priv.pem" 2>/dev/null
openssl pkcs8 -topk8 -nocrypt -in "$TMP/priv.pem" -outform DER -out "$TMP/private.der" 2>/dev/null
openssl rsa -in "$TMP/priv.pem" -pubout -outform DER -out "$TMP/public.der" 2>/dev/null

# base64 -w0 (GNU) / macOS(base64는 줄바꿈 없음) 호환
b64() { base64 "$1" | tr -d '\n'; }
PRIV="$(b64 "$TMP/private.der")"
PUB="$(b64 "$TMP/public.der")"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo ".env 생성(.env.example 기반)."
fi

# JWT_PRIVATE_KEY / JWT_PUBLIC_KEY 줄 치환(없으면 추가)
upsert() {
  local key="$1" val="$2"
  if grep -qE "^[[:space:]]*${key}=" "$ENV_FILE"; then
    # 구분자 | 사용(키에 / + 포함 가능)
    awk -v k="$key" -v v="$val" 'BEGIN{done=0}
      $0 ~ "^[[:space:]]*"k"=" {print k"="v; done=1; next}
      {print}
      END{if(!done) print k"="v}' "$ENV_FILE" > "$ENV_FILE.tmp" && mv "$ENV_FILE.tmp" "$ENV_FILE"
  else
    echo "${key}=${val}" >> "$ENV_FILE"
  fi
}
upsert "JWT_PRIVATE_KEY" "$PRIV"
upsert "JWT_PUBLIC_KEY" "$PUB"

echo "완료: .env 의 JWT_PRIVATE_KEY / JWT_PUBLIC_KEY 갱신됨."
echo "DB 비밀번호 등 나머지 값도 .env 에서 확인/변경하세요."
