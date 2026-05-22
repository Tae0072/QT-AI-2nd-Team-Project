# Report — 2026-05-22 bible-source-review-table

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/bible-data-source-check` |
| PR 대상 | `dev` |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevA_이지윤/workflows/2026-05-22_bible-source-review-table.md` |
| 관련 F-ID | F-01 |
| 작성 위치 | `doc/workspaces/DevA_이지윤/reports/2026-05-22_bible-source-review-table_report.md` |

## 작업 결과

성경 본문 JSON을 seed, fixture, 관리자 등록 데이터로 적재하기 전에 출처와 라이선스를 검토할 수 있는 기준 문서를 추가했다. 기준 산출물은 `data/bible-sources/README.md`이며, 현재 실제 승인 후보가 없으므로 빈 placeholder 행을 두지 않고 "현재 승인된 후보 없음"과 후보 추가 규칙을 기록했다.

이번 작업은 문서 작성에 한정했다. 성경 본문 텍스트, 절 샘플, 원본 JSON, 가공 JSON, SQLite DB, seed SQL, fixture 파일은 추가하지 않았다. 서버 코드, OpenAPI, 마이그레이션, 성경 적재 로직도 변경하지 않았다.

## 변경 요약

1. `data/bible-sources/README.md`를 생성했다.
2. 성경 JSON 출처 검토표의 필수 컬럼과 상태값 기준을 문서화했다.
3. 승인 후보가 없을 때 빈 미정 행을 두지 않는 운영 기준을 남겼다.
4. 금지/보류 기준에 출처 검토 누락, 재배포 불가, 라이선스 불명확 후보의 적재 차단 정책을 기록했다.
5. F-01 정책에 따라 한글 클라이언트 번들 가능 여부와 서버 마스터 사본 가능 여부를 분리해 판단하도록 정리했다.
6. 원본 파일 Git 커밋 금지, 승인 전 seed/fixture 생성 금지, PR 본문에 "본문 텍스트 커밋 없음" 명시 규칙을 남겼다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `data/bible-sources/README.md` | 성경 JSON 출처 검토표, 필수 메타데이터, 승인 상태, 금지 데이터 차단 규칙 추가 |
| `doc/workspaces/DevA_이지윤/workflows/2026-05-22_bible-source-review-table.md` | 작업 전 범위, 제외 범위, 수용 기준, 검증 계획 기록 |
| `doc/workspaces/DevA_이지윤/reports/2026-05-22_bible-source-review-table_report.md` | 작업 결과, 정책 반영, 검증 결과 기록 |

## 정책 반영

| 정책 | 반영 결과 |
| --- | --- |
| F-01 데이터 정책 | 한글 본문은 클라이언트 로컬 SQLite와 서버 마스터 사본 가능 여부를 분리해 검토하도록 기록 |
| F-01 데이터 정책 | 영어 본문은 서버 마스터 사본과 온라인 조회 가능 여부를 검토하도록 기록 |
| 코드 품질 게이트 §3.7 | `repoUrl`, `license`, `translationName`, `attribution`, `redistributionAllowed`, `prohibitedReason` 기준 반영 |
| 구현 저장소 반영 체크리스트 P0 | `data/bible-sources/README.md`에 성경 JSON 소스 검토 표 추가 |
| 금지 데이터 정책 | 금지 번역본은 후보 표에 추가하지 않고 운영 규칙에서 차단하도록 기록 |
| 원본 데이터 관리 | 원본 JSON, 가공 JSON, SQLite DB, seed SQL, fixture 파일 Git 커밋 금지 명시 |

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| `data/bible-sources/README.md`에 성경 JSON 출처 검토표가 존재한다 | 충족 | `성경 JSON 출처 검토표` 섹션 추가 |
| 검토표는 필수 검토 항목을 포함한다 | 충족 | `repoUrl`, `license`, `translationName`, `attribution`, `redistributionAllowed` 컬럼 포함 |
| 한글 클라이언트 번들 가능 여부와 서버 마스터 사본 가능 여부를 분리한다 | 충족 | `appBundleAllowed`, `serverMasterAllowed` 컬럼 및 저장 위치 판단 기준 추가 |
| 승인된 후보가 없으면 빈 미정 행 대신 후보 없음과 후보 추가 규칙을 기록한다 | 충족 | "현재 승인된 후보 없음" 문장과 후보 추가 규칙 추가 |
| 금지 번역본은 후보 표에 추가하지 않고 운영 규칙에서 차단한다 | 충족 | 금지/보류 기준에 후보 표 추가 금지 명시 |
| 라이선스 불명확 후보는 적재 금지 또는 Lead 검토 필요 상태로 분류된다 | 충족 | `NEEDS_LEAD_REVIEW` 기준과 적재 금지 규칙 추가 |
| 성경 본문 텍스트, 절 샘플, 원본 JSON 파일은 커밋하지 않는다 | 충족 | 문서 파일 1개만 추가했고 본문/샘플/원본 파일 없음 |
| "저작권 문제 없음" 표현을 사용하지 않고 "저작권 리스크를 낮춘다" 표현을 사용한다 | 충족 | README 목적 문장에 "저작권 리스크를 낮춘다" 사용 |
| PR 본문 기준 문구를 남긴다 | 충족 | 운영 규칙에 F-01, 품질 게이트 §3.7, 체크리스트 P0, "본문 텍스트 커밋 없음" 명시 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 통과 |
| `rg -n "저작권 문제 없음\|유실률 0% 보장\|이벤트 유실률 0% 보장" data/bible-sources/README.md` | 매치 없음 |
| `rg -n "^[[:space:]]*[0-9]+:[0-9]+\|Genesis [0-9]+:[0-9]+\|John [0-9]+:[0-9]+\|요한복음 [0-9]+:[0-9]+\|창세기 [0-9]+:[0-9]+" data/bible-sources/README.md` | 매치 없음 |
| `rg -n "repoUrl\|license\|translationName\|attribution\|redistributionAllowed\|prohibitedReason" data/bible-sources/README.md` | 필수 컬럼 확인 |
| `rg --files data/bible-sources` | `data/bible-sources/README.md`만 확인 |
| `gitleaks version` | 현재 환경에서 실행 파일을 찾을 수 없어 미실행 |

## 생략한 검증

| 명령 | 사유 |
| --- | --- |
| `./gradlew -p qtai-server build` | 서버 코드를 수정하지 않은 문서 작업이므로 workflow 기준에 따라 생략 |
| `./gradlew -p qtai-server test jacocoTestReport` | Java 테스트 대상 변경이 없어 workflow 기준에 따라 생략 |
| `./gradlew -p qtai-server jacocoTestCoverageVerification` | Java 테스트 대상 변경이 없어 workflow 기준에 따라 생략 |
| `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | OpenAPI 파일을 수정하지 않아 workflow 기준에 따라 생략 |
| `gitleaks detect --source . --redact --exit-code 1` | 현재 환경에서 `gitleaks` 실행 파일이 발견되지 않음 |

## 범위 확인

- `qtai-server/data/bible-sources/README.md`는 생성하거나 수정하지 않았다.
- `bible_books`, `bible_verses` 적재 로직과 마이그레이션은 수정하지 않았다.
- `GET /api/v1/bible/**` API와 OpenAPI는 수정하지 않았다.
- 요구사항 문서의 저작권 표현은 변경하지 않았다.
- 기존 `.vscode/settings.json` 변경은 작업 범위 밖으로 두고 수정하지 않았다.

## 남은 후속 작업

1. 실제 한글 성경 JSON 후보 조사와 라이선스 검토
2. 실제 영어 성경 JSON 후보 조사와 라이선스 검토
3. 승인 후보 발생 후 본문 적재 방식, 파일 보관 위치, 버전 식별자 정책 확정
4. `bible_books`, `bible_verses` seed 또는 관리자 등록 절차 구현 workflow 작성
