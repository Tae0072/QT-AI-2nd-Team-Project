# Workflow - 2026-06-01 ai-time-policy-ssot

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-time-policy-ssot` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 저장 대상 | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-time-policy-ssot.md` |
| 리포트 대상 | `doc/workspaces/DevC_강상민/reports/2026-06-01_ai-time-policy-ssot_report.md` |
| 작업 유형 | 문서 전용 SSoT 정합화 |

## 작업 목표

`00:05 KST` 내부 AI 해설 generation job 시딩과 `04:00 KST` Today QT 사용자 노출/cache refresh 정책을 SSoT 문서에서 명확히 분리한다.

서버 코드, API 계약, OpenAPI, DB schema는 변경하지 않는다.

## 정책 기준

- `00:00 KST`: 외부 QT 범위 공개 시각.
- `00:05 KST`: 오늘 QT passage가 존재한다는 전제의 내부 AI 해설 generation job 사전 시딩 시각.
- `04:00 KST`: Today QT 사용자 노출/cache refresh 및 승인본 기준 노출 갱신 시각.
- `00:05` 시딩은 `EXPLANATION + BIBLE_VERSE` generation job 생성만 의미하며, 승인본 사용자 노출을 보장하지 않는다.
- `SIMULATOR`는 이번 `00:05` 내부 시딩 범위에 포함하지 않는다.
- 사용자 요청 경로에서 해설·시뮬레이터를 즉시 생성하지 않는 정책은 유지한다.

## 수정 대상

| 문서 | 수정 방향 |
| --- | --- |
| `doc/프로젝트 문서/07_요구사항_정의서.md` | 00:00, 00:05, 04:00 시간 정책을 요구사항 기준으로 분리 |
| `doc/프로젝트 문서/25_기능_명세서.md` | F-02와 AI 호출 정책에서 00:05 내부 해설 job 시딩과 04:00 사용자 노출/cache refresh 분리 |
| `doc/프로젝트 문서/18_코드_품질_게이트.md` | 품질 검사 기준에 00:05 내부 시딩과 04:00 노출 갱신을 별도 항목으로 반영 |
| `doc/프로젝트 문서/23_도메인_용어사전.md` | 용어 정의에 AI 해설 내부 시딩 시각 추가, AI 실행 시점 표현 정합화 |
| `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md` | 구현 체크리스트의 시간 정책과 LLM 호출 기준 정합화 |
| `doc/프로젝트 문서/09_Git_규칙.md` | PR/리뷰 체크 기준에서 00:05와 04:00의 의미 분리 |
| `CLAUDE.md` | 구현 공통 지침의 고정 제품 결정 문구 정합화 |

## 제외 범위

- 서버 코드 변경 없음.
- 신규 API, OpenAPI, DB schema 변경 없음.
- scheduler 시간 변경 없음.
- workspace 실행가이드 HTML 등 파생 산출물 직접 수정 없음.
- 중앙 정책 외 구현 상세, retry, schedlock, 운영 알림 변경 없음.

## 구현 순서

1. 이 workflow 문서를 저장한다.
2. 대상 Markdown SSoT 문서의 04:00 단일 AI 생성 표현을 검색한다.
3. 00:05 내부 해설 job 시딩과 04:00 사용자 노출/cache refresh 기준으로 문구를 수정한다.
4. 오래된 금지 표현이 남았는지 검색 검증한다.
5. `git diff --check`를 실행한다.
6. report 문서를 작성한다.

## 검증 계획

```powershell
rg -n "00:05|04:00|AI 호출|해설.*생성|Today QT 캐시|수집 배치" "doc/프로젝트 문서" CLAUDE.md
rg -n "해설·시뮬레이터 생성은 04:00|04:00 KST 배치 또는 관리자 트리거에서만 실행" "doc/프로젝트 문서" CLAUDE.md
git diff --check
```

서버 코드는 변경하지 않으므로 Gradle build는 필수 검증에서 제외한다.

## 수용 기준

- 00:05 KST와 04:00 KST의 의미가 SSoT 문서에서 분리된다.
- 00:05 시딩이 `EXPLANATION + BIBLE_VERSE` generation job 생성만 의미함이 명시된다.
- 04:00 KST가 Today QT 사용자 노출/cache refresh 및 승인본 기준 노출 갱신 시각으로 유지된다.
- SIMULATOR가 00:05 시딩 범위에서 제외됨이 명시된다.
- 서버 코드, API 계약, OpenAPI, DB schema 변경이 없다.
