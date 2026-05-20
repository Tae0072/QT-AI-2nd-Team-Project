# Workflow — 2026-05-20 ai-admin-review-regeneration

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-review-regeneration` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-06, F-14 |
| 기준 문서 | `04_API_명세서.md` §4.7.3, `03_아키텍처_정의서.md` §8, `05_시퀀스_다이어그램.md` §10.3 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

관리자가 AI 산출물을 조회, 승인, 반려, 숨김, 재생성 요청할 수 있는 검토 흐름을 구현한다. 관리자 조작은 기능별 권한과 감사 로그 기준을 만족해야 하며, 승인되지 않은 산출물은 사용자에게 노출되지 않아야 한다.

## 문제 정의

AI 산출물은 자동 검증만으로 바로 사용자에게 노출되면 안 되는 경우가 있다. 운영자는 검증 로그와 체크리스트 버전을 확인하고 승인 또는 반려해야 하며, 승인 시 사용자 노출 테이블과의 연결을 원자적으로 처리해야 한다.

## API 범위

| API | 권한 | 책임 |
| --- | --- | --- |
| `GET /api/v1/admin/ai/assets` | REVIEWER/SUPER_ADMIN | 산출물 목록 조회 |
| `GET /api/v1/admin/ai/assets/{assetId}` | REVIEWER/SUPER_ADMIN | 산출물 상세 조회 |
| `POST /api/v1/admin/ai/assets/{assetId}/approve` | REVIEWER/SUPER_ADMIN | 검증 통과 산출물 승인 |
| `POST /api/v1/admin/ai/assets/{assetId}/reject` | REVIEWER/SUPER_ADMIN | 산출물 반려 |
| `POST /api/v1/admin/ai/assets/{assetId}/hide` | REVIEWER/SUPER_ADMIN | 승인 산출물 숨김 |
| `POST /api/v1/admin/ai/assets/{assetId}/regenerate` | REVIEWER/SUPER_ADMIN | 재생성 작업 생성 |

## 권한 기준

- 일반 `members.role=ADMIN`만으로는 접근할 수 없다.
- `admin_users.admin_role`이 `REVIEWER` 또는 `SUPER_ADMIN`이어야 한다.
- 관리자 변경 작업은 `audit_logs`에 남긴다.
- 배치/AI 내부 작업은 사용자 계정이 아니라 `SYSTEM_BATCH` 주체로 남긴다.

## 승인 조건

- 요청의 `checklistVersionId`가 활성 체크리스트여야 한다.
- 산출물의 최신 검증 로그가 해당 checklist version으로 존재해야 한다.
- 최신 검증 결과가 `PASSED`가 아니면 승인할 수 없다.
- 승인 대상 asset type에 맞는 사용자 노출 테이블 연결 정책이 있어야 한다.
- 절별 해설 승인 시 기존 활성 해설의 `activeUniqueKey`를 해제한 뒤 신규 승인본을 활성화한다.

## 상태 전이

```text
VALIDATING -> APPROVED
VALIDATING -> REJECTED
APPROVED -> HIDDEN
REJECTED -> regenerate -> ai_generation_jobs(QUEUED)
```

## 제외 범위

- 관리자 웹 UI 구현은 김지민 담당 범위와 별도이다.
- 검증 참조 원문 전체를 일반 목록 응답에 포함하지 않는다.
- AI 산출물 원문 전체 노출은 상세 권한과 필요 필드가 확정되기 전까지 제한한다.

## 구현 순서

1. 관리자 인증/인가 공통 구조가 있는지 확인한다.
2. Admin controller는 Request 검증과 UseCase 호출만 담당한다.
3. Review service에서 상태 전이와 checklist version을 검증한다.
4. 승인, 반려, 숨김, 재생성 요청마다 audit log를 남긴다.
5. 재생성은 즉시 LLM 호출이 아니라 `ai_generation_jobs(QUEUED)` 생성으로 처리한다.
6. 사용자 노출 연결은 asset type별로 분기하고, 연결 불가 타입은 명확한 예외를 반환한다.

## 수용 기준

- [ ] REVIEWER/SUPER_ADMIN이 아닌 관리자는 변경 API를 호출할 수 없다.
- [ ] checklist version 누락 시 `CHECKLIST_VERSION_REQUIRED`를 반환한다.
- [ ] 검증 실패 산출물은 승인할 수 없다.
- [ ] 승인/반려/숨김/재생성 요청은 감사 로그에 남는다.
- [ ] 숨김 상태 산출물은 사용자 API로 노출되지 않는다.
- [ ] 재생성 요청은 작업만 생성하고 사용자 요청 스레드에서 LLM을 직접 호출하지 않는다.

## 테스트 계획

- Controller 테스트: 권한별 403/200, request validation
- Service 테스트: 상태 전이, checklist version 검증, audit log 호출
- Repository 테스트: 목록 필터(assetType, status, promptVersionId, checklistVersionId)
- 회귀 테스트: 검증 참조 원문 미노출

## 검증 명령

- `./gradlew -p qtai-server test --tests "*AdminAi*"`
- `./gradlew -p qtai-server build`
- `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`
