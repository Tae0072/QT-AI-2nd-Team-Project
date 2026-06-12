# Workflow - 2026-06-12 batch-ownership-guard

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | 운영 안정화 |
| 트리거 | Docker 로컬 환경에서 모놀리식 복사본 `admin-server`와 도메인 서비스가 같은 배치/워커를 동시에 실행할 수 있는지 점검 요청 |
| 기준 문서 | `docker-compose.yml`, `qtai-server/*/src/main/resources/application.yml`, `@Scheduled` 배치/워커 코드 |
| 해당 경로 | `docker-compose.yml`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

로컬 Docker compose 환경에서 같은 책임의 배치/워커가 둘 이상의 프로세스에서 동시에 실행되지 않도록 소유권을 명확히 한다. 특히 `admin-server`는 모놀리식 복사본이라 note/qt/music/ai 코드가 함께 들어 있어 도메인 서비스와 중복 실행될 수 있다.

## 범위

- `@Scheduled`, `ConditionalOnProperty`, `@Value(...enabled...)` 기준으로 배치/워커 후보를 점검한다.
- `docker compose config` 기준 실제 환경변수 적용 상태를 확인한다.
- 중복 실행 위험이 있는 admin-server 복사본 배치를 명시적으로 비활성화한다.
- 컨테이너를 재생성해 환경변수 적용 여부와 health를 확인한다.
- report를 작성한다.

## 제외 범위

- 운영/staging 배포 환경 변수 변경
- 배치 분산락 구현
- DB schema 변경
- 배치 실행 이력 화면 개편

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `docker-compose.yml` | admin-server 복사본 배치/워커 비활성 플래그 명시 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_batch-ownership-guard_report.md` | 점검 결과와 조치 기록 |

## 구현 순서

1. `@Scheduled`, `ConditionalOnProperty`, enabled property를 검색한다.
2. `service-ai`, `service-bible`, `service-note`, `admin-server`의 배치 소유권을 비교한다.
3. `admin-server`에서 도메인 서비스와 겹치는 배치를 식별한다.
4. `docker-compose.yml`의 `service-admin.environment`에 누락된 비활성 플래그를 추가한다.
5. `docker compose config`로 플래그 반영을 확인한다.
6. `docker compose up -d service-admin`으로 컨테이너를 재생성한다.
7. `docker exec qtai-admin-server printenv`와 health로 실제 적용을 확인한다.
8. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| 해당 없음 | compose 환경 설정 점검 작업으로 자동 테스트 대신 compose config, 컨테이너 env, health를 검증 |

## 수용 기준

- [ ] `admin-server` AI 생성 worker가 꺼져 있다.
- [ ] `admin-server` AI daily seed가 꺼져 있다.
- [ ] `admin-server` SU 오늘 QT 수집이 꺼져 있다.
- [ ] `admin-server` music seed가 꺼져 있다.
- [ ] `admin-server` journal reprocessor가 꺼져 있다.
- [ ] `qtai-admin-server` 컨테이너가 재생성 후 healthy 상태다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 compose 설정 1파일과 문서 기록에 한정된다.
- 서비스별 배치 소유권을 같은 맥락에서 한 번에 판단해야 한다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 점검, compose 수정, 컨테이너 재생성, report 작성을 직접 수행한다.

## 검증 계획

```powershell
docker compose config
docker compose up -d service-admin
docker exec qtai-admin-server printenv
docker ps
```

## 후속 작업으로 남길 항목

- 운영/staging 환경에서도 동일한 배치 소유권 문서를 확인한다.
- 장기적으로 admin-server 복사본에 포함된 도메인 배치 코드를 제거하거나 조건부 빈으로 묶는다.
- 중요한 배치에는 DB 기반 lock 또는 idempotency key를 추가 검토한다.
