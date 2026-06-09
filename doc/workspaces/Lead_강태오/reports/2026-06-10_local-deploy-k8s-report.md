# 리포트 — MSA 로컬 배포 컨테이너화 (Docker Compose + 로컬 Kubernetes)

- 일자: 2026-06-10
- 브랜치: `feature/local-deploy-k8s` → PR base `dev-msa`
- 범위(Day3 ③ 로컬 k8s): service-user/bible/note/ai **+ admin-server**(작업 중 PR #433 머지 확인) 컨테이너화 + 단일 MySQL/Redis + k8s 매니페스트.

## 1. 무엇을 만들었나

### Docker
- `qtai-server/service-{user,bible,note,ai}/Dockerfile` + `qtai-server/admin-server/Dockerfile` — `eclipse-temurin:21-jre-jammy` 기반, 비루트(`spring`) 실행, 사전 빌드한 bootJar(`*-SNAPSHOT.jar`)만 복사(plain jar 제외). 각 모듈 `.dockerignore`로 빌드 컨텍스트 최소화.
- 루트 `docker-compose.yml` — `mysql:8.0`(단일) + `redis:7` + **5서비스**(user/bible/note/ai/admin). healthcheck(서비스는 TCP, actuator 미탑재), `depends_on: service_healthy`, `restart: unless-stopped`.
- `.env.example` + `scripts/generate-keys.(ps1|sh)` — RS256 키쌍 생성→`.env`. 평문 키/비밀번호는 `.env`(gitignore)에만.

### Kubernetes (`k8s/`)
- `00-namespace`(qtai) / `10-configmap`(비밀 아닌 공유 설정) / `15-secret.example`(placeholder) / `20-mysql`(PVC+Deployment+Service) / `25-redis` / `30~33-service-*`(Deployment + NodePort 30081~30084).
- `34-service-admin.yaml`(prod 프로파일·개인키+공개키·Flyway off·`SERVER_PORT=8090`), `k8s/README.md`, `k8s/scripts/generate-secret.(ps1|sh)`(클러스터에 Secret 직접 주입, 평문 파일 미생성).
- Docker Desktop k8s 전제: `imagePullPolicy: IfNotPresent`.

## 2. 핵심 설계 판단

- **단일 DB**: 4개 서비스가 `qtai` 한 DB를 공유하고 도메인 테이블로 구분. JPA `update`(로컬)로 각자 스키마 생성. 검증에서 31개 테이블 동시 생성 확인.
- **JWT 최소 권한**: 개인키는 service-user만. 나머지는 공개키만 받아 검증. k8s에서도 Deployment별로 secretKeyRef를 달리해 개인키 노출 차단.
- **서비스 소스 불변**: bible/note/ai에 `SECURITY_JWT_PUBLIC_KEY` env만 주입(relaxed binding)해 검증 필터 활성화. yml/코드 미수정.

## 3. 검증 결과 (2~3회 검토)

| 항목 | 방법 | 결과 |
|---|---|---|
| compose 문법 | `docker compose config` | EXIT 0 |
| 빌드 | 호스트 gradlew bootJar 4종 | BUILD SUCCESSFUL(22s), fat jar 54~67MB |
| 빌드 | admin-server bootJar | BUILD SUCCESSFUL(17s) |
| 이미지·기동 | `docker compose up -d --build` | **7컨테이너 전부 healthy**(5서비스+mysql+redis) |
| HTTP 응답 | 8081~8084,8090 GET | 401/401/403/403/401 (보안 활성·앱 정상) |
| JWT 검증 | 변조 토큰 → bible | 401 (검증 필터 활성) |
| 단일 DB 공유 | information_schema | `qtai` 31+ 테이블(5서비스 공유) |
| 종단(스크립트 키) | 스크립트 .env로 재기동 | service-user 포함 전부 healthy |
| admin 단일DB 통합 | Flyway off·음원시더 off·SERVER_PORT=8090 | healthy(8090→401) |
| k8s 매니페스트 | 오프라인 YAML 파싱 | 전부 valid(12파일) |

> k8s 서버 dry-run은 Docker Desktop Kubernetes 미활성으로 생략. apply 절차는 `k8s/README.md`에 정리(사용자가 k8s 켜고 그대로 적용 가능).

## 4. 보안·CI 적합성

- `.env`는 `.gitignore` 등록·미추적. 커밋 대상 파일에 평문 키/긴 base64 없음(수동 스캔 통과). k8s 실제 Secret(`secret.local.yaml`/`secret.yaml`)도 gitignore.
- CI `docker-compose-config` 잡(루트 `docker compose config`)·`requirements-guard`(#6 k8s 비차단 INFO) 충족하도록 루트에 배치, env는 전부 기본값 부여(`.env` 없이도 config 통과).

## 5. admin-server 단일 DB 통합 메모 (입문자용)

admin-server는 슬림 서비스가 아니라 **모놀리식 전체 복사본**(admin 컨트롤러만 잔류)이라, 단일 DB에 그대로 붙이면 3가지가 충돌했다. 모두 **env로만** 해결(소스 불변):

1. **포트**: yml에 `server.port`가 없어 기본 8080으로 떠 8090 매핑과 어긋남 → `SERVER_PORT=8090`.
2. **음원 시더**: `MusicSeedRunner`(기본 ON)가 LONGBLOB 음원을 단일 DB에 시딩하다 `Data too long for column 'audio_data'` → `QTAI_MUSIC_SEED_ENABLED=false`(service-bible과 동일, 음원 자산 미이전 상태).
3. **스키마 소유권**: admin은 Flyway+`validate`, 4서비스는 JPA `update` → 한 DB에 두 소유자가 충돌 → admin도 `SPRING_FLYWAY_ENABLED=false`+`SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 통일.

> 운영 전환 시에는 반대로 "Flyway 단일 소유" 등 스키마 권위를 한 곳으로 정하는 결정이 필요하다(로컬은 JPA update로 통일).

## 6. 남은 일 / 후속

- 커밋·푸시·PR(base=dev-msa), 첫푸시 APPROVE 체크리스트 적용.
- (운영) k8s/Helm 운영 도입은 별도 결정(본 작업은 로컬 한정). admin-server의 사용자용 컨트롤러 정리·스키마 권위 일원화는 후속.
- RestClient 통합(서비스 간 내부 HTTP)은 Day3 별도 트랙.
