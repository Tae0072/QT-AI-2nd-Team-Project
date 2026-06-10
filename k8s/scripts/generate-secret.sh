#!/usr/bin/env bash
# =============================================================================
# QT-AI MSA 로컬 k8s — qtai-secret 생성 (Linux / macOS)
#
# RS256 키쌍을 생성하고 DB 비밀번호를 받아 클러스터에 Secret을 직접 만든다.
# 평문 키는 파일로 저장하지 않고 kubectl로 바로 주입한다.
#
# 사용:  ./k8s/scripts/generate-secret.sh
# 전제:  kubectl, openssl. 네임스페이스 qtai 가 먼저 있어야 한다
#        (kubectl apply -f k8s/00-namespace.yaml).
# =============================================================================
set -euo pipefail

for c in kubectl openssl; do
  command -v "$c" >/dev/null 2>&1 || { echo "$c 가 필요합니다."; exit 1; }
done

read -rp "DB_PASSWORD (qtai 사용자): " DB_PW
read -rp "MYSQL_ROOT_PASSWORD: " ROOT_PW
read -rp "DEEPSEEK_API_KEY (없으면 Enter): " DEEP

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# private는 PKCS#8(DER) 강제(genpkey -outform DER이 일부 빌드에서 PKCS#1을 내보내 Java 디코딩 실패).
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP/priv.pem" 2>/dev/null
openssl pkcs8 -topk8 -nocrypt -in "$TMP/priv.pem" -outform DER -out "$TMP/private.der" 2>/dev/null
openssl rsa -in "$TMP/priv.pem" -pubout -outform DER -out "$TMP/public.der" 2>/dev/null
PRIV="$(base64 "$TMP/private.der" | tr -d '\n')"
PUB="$(base64 "$TMP/public.der" | tr -d '\n')"
SYS="$(openssl rand -base64 48 | tr -d '\n')"

kubectl -n qtai delete secret qtai-secret --ignore-not-found >/dev/null
kubectl -n qtai create secret generic qtai-secret \
  --from-literal=DB_PASSWORD="$DB_PW" \
  --from-literal=MYSQL_ROOT_PASSWORD="$ROOT_PW" \
  --from-literal=JWT_PRIVATE_KEY="$PRIV" \
  --from-literal=JWT_PUBLIC_KEY="$PUB" \
  --from-literal=SECURITY_JWT_SYSTEM_SECRET="$SYS" \
  --from-literal=DEEPSEEK_API_KEY="$DEEP"

echo "완료: qtai 네임스페이스에 qtai-secret 생성됨(평문 파일 미저장)."
