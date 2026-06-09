# 2026-06-10 워크플로우 — MSA 로컬 배포 컨테이너화 (Docker Compose + 로컬 Kubernetes)

- 담당: Claude(작업) / Lead 강태오(검토)
- 브랜치: `feature/local-deploy-k8s` (base `dev-msa`)
- 작업 폴더: `D:\workspace\QT-AI-deploy` (다른 worktree 미접촉)
- 근거: 회의록 2026-06-09 §9 "로컬은 쿠버네티스 배포로 마무리" + Lead 결정 `workflows/2026-06-10_local-k8s-deploy.md`(CI 가드 완화 PR #432)

## 목표

단일 DB(MySQL 1개) + Redis + 사용자 서비스 4개 + admin-server를 로컬에서 컨테이너로 기동한다.

| 모듈 | 포트 | 배포 | JWT |
|---|---|---|---|
| service-user | 8081 | O | RS256 **발급**(개인키) + 검증 |
| service-bible | 8082 | O | 공개키 **검증만** |
| service-note | 8083 | O | 공개키 **검증만** |
| service-ai | 8084 | O | 공개키 **검증만** |
| admin-server | 8090 | **TODO** | 공개키 검증만 (PR #433 머지 후) |
| lib-common | - | X(라이브러리) | - |

## 결정 사항

1. **로컬 k8s 런타임 = Docker Desktop Kubernetes**, 노출 = **NodePort**(30081~30084). 런타임 무관하게 이식성이 가장 높고, Docker Desktop은 로컬 이미지 스토어를 공유해 `image load`가 불필요(`imagePullPolicy: IfNotPresent`).
2. **단일 DB(`qtai`) 공유** — DB-per-service 금지. 각 서비스가 자기 도메인 테이블만 생성(JPA `update`, 로컬 한정). 운영 기본은 `validate`.
3. **JWT 키 최소 권한** — 개인키(`JWT_PRIVATE_KEY`)는 service-user에만. bible/note/ai/admin은 공개키(`SECURITY_JWT_PUBLIC_KEY`)만 주입해 검증만. 평문 키는 `.env`(gitignore)/k8s Secret으로만 주입(저장소 미커밋, gitleaks 통과).
4. **서비스 소스 미변경** — bible/note/ai는 `security.jwt.public-key` 미설정 상태라, 배포 설정에서 env(`SECURITY_JWT_PUBLIC_KEY`, relaxed binding)만 주입해 lib-common 검증 필터를 활성화. application.yml/코드 수정 없음(불필요 리팩터링 회피).
5. **admin-server는 PR #433 OPEN(미머지)이라 4개로 우선 구성**, admin은 compose 주석 블록 + `k8s/34-service-admin.yaml.todo` 템플릿으로 준비만.

## 산출물 (TODO 체크리스트)

- [x] ① 서비스별 Dockerfile 4종 (`qtai-server/service-*/Dockerfile`, temurin 21-jre, 비루트, bootJar 복사) + `.dockerignore`
- [x] ② `docker-compose.yml` (루트) — mysql:8.0 1개 + redis + 4서비스, env(DB/REDIS/JWT/PROFILE), healthcheck·depends_on, admin 주석 TODO
- [x] ② `.env.example` 갱신 + `scripts/generate-keys.(ps1|sh)` (RS256 PKCS8 생성→.env, 평문 미커밋)
- [x] ③ `k8s/` 매니페스트 — namespace/configmap/secret 템플릿/mysql(PVC)/redis/4서비스(Deployment+NodePort) + `k8s/README.md` + `k8s/scripts/generate-secret.(ps1|sh)`
- [x] 빌드·이미지·기동 검증 (compose up, 전 컨테이너 healthy, HTTP 401/403, JWT 검증, 단일 DB 31테이블) — 2~3회 검토 완료
- [x] 워크플로우·리포트·스터디노트
- [ ] 커밋·푸시·PR(base=dev-msa) — 첫푸시 APPROVE 체크리스트 적용
- [ ] (후속) PR #433 머지 후 admin-server Dockerfile + compose/k8s 활성화

## 검증 요약 (재현 절차)

```
./scripts/generate-keys.ps1                 # .env + RS256(PKCS8) 키
cd qtai-server && ./gradlew :service-user:bootJar :service-bible:bootJar \
    :service-note:bootJar :service-ai:bootJar
cd .. && docker compose up -d --build
docker compose ps                            # 6개 컨테이너 (healthy)
```

- 결과: mysql/redis/service-user/bible/note/ai 전부 `healthy`.
- HTTP: 8081→401, 8082→401, 8083→403, 8084→403 (보안 활성, 앱 정상).
- JWT: 변조 토큰 → 401 (lib-common 검증 필터 활성 확인).
- 단일 DB: `qtai` 스키마에 31개 테이블(ai_*/bible_*/member_* 등) 생성 — 단일 DB 공유 확인.
- k8s 매니페스트: 오프라인 YAML 파싱 전부 OK(클러스터 미기동이라 서버 dry-run은 생략).

## 막힌 점·교훈 (리포트/스터디노트 상세)

1. **JWT 개인키 PKCS#1 vs PKCS#8** — `openssl genpkey -outform DER`이 이 빌드에서 PKCS#1(1191B)을 내보내 Java `PKCS8EncodedKeySpec`이 "algid parse error"로 실패. `openssl pkcs8 -topk8 -nocrypt`로 PKCS#8(1217~1218B) 변환 강제. 스크립트 4종 모두 반영.
2. **PowerShell 5.1 + UTF-8(BOM 없음)** — 한글 `.ps1`을 ANSI로 오독해 파싱 붕괴. 스크립트 `.ps1`은 **UTF-8 BOM**으로 저장.
3. **컨테이너명 충돌** — 모놀리식 데모 스택의 잔류 `qtai-mysql`/`qtai-redis`와 충돌 → `docker rm -f`로 제거 후 기동.
4. **기존 루트 docker-compose.yml은 모놀리식(8080)** — MSA 4서비스로 교체.
