# 2026-05-28 AI inputHash 제외 문서 정리 리포트

## 기준

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-remove-input-hash-plan` |
| PR 대상 | `dev` |
| 변경 유형 | docs |
| 관련 F-ID | F-14 |
| 실행 경로 | 직접 실행 |

## 정리 배경

W2 스케줄과 Lead 확인 체크리스트에는 `inputHash`를 `ai_generation_jobs.input_hash` 컬럼으로 저장하고 active unique key에 포함한다는 문구가 남아 있었다.

2026-05-28 정리 결정에 따라 W2/MVP 범위에서는 `inputHash` 컬럼, 서버 계산 helper, active unique key 확장을 진행하지 않는다. 중복 실행 방지는 현재 구현된 `QUEUED`/`RUNNING` 진행 중 job 차단과 기존 active unique key 기준을 유지한다.

## 변경 내용

| 파일 | 변경 |
| --- | --- |
| `doc/workspaces/DevC_강상민/강상민_2W_스케줄.md` | `inputHash` 구현 후보와 확정 요약을 제거하고 W2/MVP 제외 결정으로 정리 |
| `doc/workspaces/DevC_강상민/reports/checkList.md` | `inputHash` 저장 위치 확정 항목을 제외 결정으로 변경 |
| `doc/프로젝트 문서/23_도메인_용어사전.md` | 문서 버전을 v1.2로 올리고 `입력 해시(inputHash)` 용어를 제거 |

## 제외 범위

- 과거 workflow/report의 `inputHash` 언급은 당시 검토 이력으로 남겼다.
- 서버 코드, OpenAPI, DB migration은 변경하지 않았다.
- `ai_generation_jobs` unique key 구현은 변경하지 않았다.
- 신규 workflow 문서는 작성하지 않았다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `rg --line-number "inputHash\|input_hash\|입력 해시" doc/workspaces/DevC_강상민/강상민_2W_스케줄.md doc/workspaces/DevC_강상민/reports/checkList.md "doc/프로젝트 문서/23_도메인_용어사전.md"` | 제외 결정 문구와 본 리포트 참조만 남음 |
| `rg --line-number --glob "*.html" "inputHash\|input_hash\|입력 해시" doc` | 매칭 없음 |
| `git diff --check` | 통과 |

## 남은 참고 사항

- 과거 산출물에는 `inputHash`가 gap 또는 후속 후보로 언급되어 있다. 이번 PR에서는 과거 기록을 소급 수정하지 않는다.
- 이후 구현 PR에서는 `inputHash` 컬럼, request field, server-side hash helper를 추가하지 않는다.
