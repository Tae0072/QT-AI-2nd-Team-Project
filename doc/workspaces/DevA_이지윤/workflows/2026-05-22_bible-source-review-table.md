# Workflow — 2026-05-22 bible-source-review-table

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/bible-data-source-check` |
| PR 대상 | `dev` |
| 관련 F-ID | F-01 |
| 트리거 | 성경 JSON 출처 검토표 작성 작업을 시작하기 전에 문서 작성 범위, 검토 항목, 검증 기준을 고정한다. |
| 기준 문서 | `doc/프로젝트 문서/07_요구사항_정의서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md`, `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md`, `doc/프로젝트 문서/23_도메인_용어사전.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `data/bible-sources/**`, `workspaces/이지윤/**` |

## 작업 목표

성경 본문 JSON을 seed, fixture, 관리자 등록 데이터로 적재하기 전에 출처와 라이선스를 검토할 수 있는 표를 작성한다. 검토표는 `repoUrl`, `license`, `translationName`, `attribution`, `redistributionAllowed`를 필수 항목으로 포함하고, 사용 보류 데이터는 `prohibitedReason`과 함께 명확히 차단한다.

이번 작업은 성경 본문 텍스트를 저장하거나 샘플 절을 커밋하는 작업이 아니다. 목표는 이후 한글 성경 번들, 서버 마스터 사본, 영어 온라인 조회 구현이 저작권 리스크를 낮춘 상태에서 진행되도록 검토 기준과 승인 상태를 문서화하는 것이다.

## 범위

- `data/bible-sources/README.md`를 기준 산출물로 생성하고 성경 JSON 출처 검토표를 작성한다.
- 기존 `qtai-server/data/bible-sources/README.md`는 현재 저장소 현황 참고용으로만 확인하고, Lead가 구현 저장소 canonical path로 승인하지 않는 한 이번 작업 대상에서 제외한다.
- 검토표에는 `repoUrl`, `license`, `translationName`, `language`, `attribution`, `redistributionAllowed`, `appBundleAllowed`, `serverMasterAllowed`, `reviewStatus`, `prohibitedReason`, `reviewedAt`, `reviewer`, `notes`를 둔다.
- `reviewStatus` 값은 `APPROVED`, `REJECTED`, `NEEDS_LEAD_REVIEW` 중 하나로 제한한다.
- 실제 후보가 확정되지 않은 경우에도 빈 placeholder 행을 두지 않고, "후보 추가 규칙"과 "승인 전 적재 금지" 정책을 문장으로 남긴다.
- 금지 번역본은 후보 표에 행으로 추가하지 않고, 운영 규칙의 차단 기준에서만 다룬다.
- 한글 본문은 F-01 정책에 따라 클라이언트 로컬 SQLite와 서버 마스터 사본 가능 여부를 별도로 판단한다.
- 영어 본문은 F-01 정책에 따라 서버 마스터 사본과 온라인 조회 가능 여부를 별도로 판단한다.
- `data/bible-sources/README.md`에 원본 JSON 파일을 Git에 커밋하지 않는 운영 규칙을 유지한다.
- PR 설명에는 관련 기준 문서와 "본문 텍스트 커밋 없음"을 명시한다.

## 제외 범위

- 성경 JSON 원본 파일, 가공 JSON, SQLite DB, seed SQL, fixture 파일 추가는 제외한다.
- 성경 본문 텍스트, 절 샘플, 번역문 일부 인용은 제외한다.
- `bible_books`, `bible_verses` 적재 로직과 마이그레이션 변경은 제외한다.
- `GET /api/v1/bible/**` API 구현 또는 OpenAPI 변경은 제외한다.
- 라이선스 법률 판단 확정은 제외한다. 문서상 불명확한 후보는 `NEEDS_LEAD_REVIEW`로 남기고 적재하지 않는다.
- 요구사항 문서의 저작권 표현을 임의로 변경하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `data/bible-sources/README.md` | 성경 JSON 출처 검토표, 필수 메타데이터, 승인 상태, 금지 데이터 차단 규칙을 기록한다. |
| Modify | `workspaces/이지윤/workflows/2026-05-22_bible-source-review-table.md` | 작업 전 계획과 검증 기준을 보존한다. |

## 구현 순서

1. `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md`에서 성경 JSON 소스 검토표의 필수 항목과 대상 경로를 확인한다.
2. `doc/프로젝트 문서/18_코드_품질_게이트.md`의 "성경 JSON 소스 등록 검사"를 기준으로 필수 컬럼을 확정한다.
3. `doc/프로젝트 문서/07_요구사항_정의서.md` F-01 데이터 정책을 확인해 한글/영어 성경 저장 위치 판단 컬럼을 반영한다.
4. `data/bible-sources/` 디렉터리가 없으면 생성한다.
5. `data/bible-sources/README.md`를 생성하고 `성경 JSON 출처 검토표` 섹션을 추가한다.
6. 검토표 헤더를 `reviewStatus`, `language`, `translationName`, `repoUrl`, `license`, `attribution`, `redistributionAllowed`, `appBundleAllowed`, `serverMasterAllowed`, `prohibitedReason`, `reviewedAt`, `reviewer`, `notes` 순서로 작성한다.
7. 승인된 후보가 없는 상태라면 검토표 본문 대신 "현재 승인된 후보 없음" 문장을 두고, 후보 추가 시 각 컬럼을 채우도록 안내한다.
8. `금지/보류 기준` 섹션을 추가해 금지 번역본은 후보 표에 추가하지 않고, 라이선스 불명확 후보와 재배포 불가 후보는 본문 적재 전 차단한다고 명시한다.
9. `운영 규칙` 섹션에 원본 파일 Git 커밋 금지, 별도 스토리지 보관, 승인 전 seed/fixture 생성 금지, 출처 표기 누락 시 머지 금지를 남긴다.
10. `qtai-server/data/bible-sources/README.md`가 남아 있더라도 이번 PR의 산출물 경로는 `data/bible-sources/README.md`임을 PR 본문에 명시한다.
11. 문서 전체에서 실제 성경 절 인용, 원본 JSON, 절 샘플이 추가되지 않았는지 수동 확인한다.
12. `git diff --check`로 Markdown 공백 오류를 확인한다.
13. `rg -n "저작권 문제 없음|유실률 0% 보장|이벤트 유실률 0% 보장" data/bible-sources/README.md`를 실행해 금지 표현을 확인한다.
14. `rg -n "^[[:space:]]*[0-9]+:[0-9]+|Genesis [0-9]+:[0-9]+|John [0-9]+:[0-9]+|요한복음 [0-9]+:[0-9]+|창세기 [0-9]+:[0-9]+" data/bible-sources/README.md`를 실행해 절 인용 형태의 샘플 본문이 들어가지 않았는지 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| 해당 없음 | 이번 작업은 문서 작성만 수행하므로 Java 테스트 파일을 추가하지 않는다. 대신 검증 계획의 `git diff --check`와 `rg` 명령으로 문서 guardrail을 확인한다. |

## 수용 기준

- [ ] `data/bible-sources/README.md`에 성경 JSON 출처 검토표가 존재한다.
- [ ] 검토표는 `repoUrl`, `license`, `translationName`, `attribution`, `redistributionAllowed`를 필수 검토 항목으로 포함한다.
- [ ] 검토표는 한글 클라이언트 번들 가능 여부와 서버 마스터 사본 가능 여부를 분리해서 판단한다.
- [ ] 승인된 후보가 없으면 빈 미정 행 대신 "현재 승인된 후보 없음"과 후보 추가 규칙을 기록한다.
- [ ] 금지 번역본은 후보 표에 추가하지 않고 운영 규칙에서 차단한다.
- [ ] 라이선스 불명확 후보는 적재 금지 또는 Lead 검토 필요 상태로 분류된다.
- [ ] 성경 본문 텍스트, 절 샘플, 원본 JSON 파일은 커밋하지 않는다.
- [ ] "저작권 문제 없음" 표현을 사용하지 않고 "저작권 리스크를 낮춘다" 표현을 사용한다.
- [ ] PR 본문에 F-01, `18_코드_품질_게이트.md` §3.7, `22_구현_저장소_반영_체크리스트.md`의 P0 항목을 근거로 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 대상이 `data/bible-sources/README.md` 한 파일에 집중되어 병렬화 이점이 작다.
- 출처 검토표의 컬럼, 금지 데이터 기준, 저장 위치 판단은 같은 문맥에서 일관되게 정리해야 한다.
- 실제 후보 조사와 법무 수준 검토는 별도 후속 작업으로 분리해야 하며, 이번 문서 작성과 병렬 진행하면 승인 상태가 섞일 위험이 있다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 기준 문서 확인, README 검토표 작성, 금지 데이터/표현 검사를 직접 수행한다.

## 검증 계획

- `git diff --check`
- `rg -n "저작권 문제 없음|유실률 0% 보장|이벤트 유실률 0% 보장" data/bible-sources/README.md`
- `rg -n "^[[:space:]]*[0-9]+:[0-9]+|Genesis [0-9]+:[0-9]+|John [0-9]+:[0-9]+|요한복음 [0-9]+:[0-9]+|창세기 [0-9]+:[0-9]+" data/bible-sources/README.md`
- 실제 성경 절 인용, 원본 JSON, 절 샘플이 추가되지 않았는지 수동 확인한다.
- `rg -n "repoUrl|license|translationName|attribution|redistributionAllowed|prohibitedReason" data/bible-sources/README.md`
- 서버 코드와 OpenAPI를 수정하지 않으므로 `./gradlew -p qtai-server build`, `./gradlew -p qtai-server test jacocoTestReport`, `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`은 이번 문서 작업의 필수 검증에서 제외한다.
- 성경 원본 파일이나 secret을 추가하지 않지만, PR 전에는 `gitleaks` 실행 파일이 설치된 환경에서 `gitleaks detect --source . --redact --exit-code 1`을 실행한다.

## 후속 작업으로 남길 항목

- 실제 한글 성경 JSON 후보 조사와 라이선스 검토
- 실제 영어 성경 JSON 후보 조사와 라이선스 검토
- 승인된 후보가 생긴 뒤 성경 본문 적재 방식, 파일 보관 위치, 버전 식별자 정책 확정
- `bible_books`, `bible_verses` seed 또는 관리자 등록 절차 구현 workflow 작성
