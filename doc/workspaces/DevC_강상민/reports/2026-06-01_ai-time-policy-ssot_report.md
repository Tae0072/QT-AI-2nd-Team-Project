# Report - 2026-06-01 ai-time-policy-ssot

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `docs/ai-time-policy-ssot` |
| PR 대상 | `dev` |
| 작업 유형 | 문서 전용 SSoT 정합화 |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-01_ai-time-policy-ssot.md` |
| 관련 F-ID | F-02, F-14 |

## 실행 요약

00:05 KST 내부 AI 해설 generation job 시딩과 04:00 KST Today QT 사용자 노출/cache refresh 정책을 SSoT 문서에서 분리했다.

서버 코드, API 계약, OpenAPI, DB schema 변경은 없다.

## 변경 내용

- `07_요구사항_정의서.md`
  - 00:00 KST 외부 QT 범위 공개, 00:05 KST 내부 해설 job 시딩, 04:00 KST 사용자 노출/cache refresh 기준을 분리했다.
  - 00:05 시딩은 `EXPLANATION + BIBLE_VERSE` generation job 생성만 의미하며 승인본 노출을 보장하지 않는다고 명시했다.
  - SIMULATOR는 00:05 시딩 범위에서 제외한다고 명시했다.
- `25_기능_명세서.md`
  - AI 호출/F-02 항목에서 00:05 내부 해설 job 시딩과 04:00 사용자 노출/cache refresh를 분리했다.
- `18_코드_품질_게이트.md`
  - 품질 검사 기준에 00:05 내부 시딩과 04:00 노출 갱신 기준을 별도 항목으로 반영했다.
- `23_도메인_용어사전.md`
  - `AI 해설 내부 시딩 시각`, `Today QT 노출/cache refresh 시각` 용어를 정리했다.
- `22_구현_저장소_반영_체크리스트.md`
  - Today QT 계약 테스트와 데이터·배치 체크리스트의 시간 기준을 정합화했다.
- `09_Git_규칙.md`
  - PR 체크 기준에서 00:05, 04:00의 의미를 분리했다.
- `CLAUDE.md`
  - 구현 공통 지침의 고정 제품 결정과 테스트 기준을 00:05/04:00 정책에 맞게 갱신했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `rg -n "00:05|04:00|AI 호출|해설.*생성|Today QT 캐시|수집 배치" "doc/프로젝트 문서" CLAUDE.md` | 성공. 변경 대상 문서에서 00:05 내부 시딩과 04:00 사용자 노출/cache refresh 기준이 확인됨 |
| `rg -n "해설·시뮬레이터 생성은 04:00|04:00 KST 배치 또는 관리자 트리거에서만 실행" "doc/프로젝트 문서" CLAUDE.md` | 기대대로 매칭 없음 |
| `git diff --check` | 성공. CRLF 변환 warning만 출력 |

## 제외 확인

- 서버 코드 변경 없음.
- 신규 API/OpenAPI/DB schema 변경 없음.
- scheduler 시간 변경 없음.
- workspace 실행가이드 HTML 등 파생 산출물 직접 수정 없음.
- Gradle build는 문서 전용 PR이므로 실행하지 않았다.

## 후속 작업

- 실제 서버 scheduler/worker 변경은 기존 PR에서 처리된 범위를 유지한다.
- 운영 알림, schedlock, DB unique constraint, retry/backoff 정책은 별도 작업으로 남긴다.
