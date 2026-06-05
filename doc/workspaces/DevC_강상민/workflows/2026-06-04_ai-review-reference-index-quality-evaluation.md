# Workflow - 2026-06-04 ai-review-reference-index-quality-evaluation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/ai-reference-index-quality-evaluation` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | parser 보강 후 재생성된 AI 검수 참조자료 index를 실제 평가/연결 테스트 후보로 사용해도 되는지 정량 판단이 필요하다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-heading-parser-recovery.md`, `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-heading-parser-recovery_report.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

현재 로컬 `qtai-server/build/ai-review-reference/reference-index.json`을 정량 평가하고, 품질 평가 및 layer 2 연결 테스트용으로 사용할 수 있는지 문서로 판단한다.

평가 report는 원문 포함 산출물을 Git에 올리지 않고도 검토자가 index의 규모, 분포, 회복 대상 9권 상태, 안전 조건을 확인할 수 있게 작성한다.

## 범위

- `reference-index.json`의 schema, entry count, book count, text length 지표, 빈 텍스트 수, 중복 hash 수를 집계한다.
- promotion summary의 `candidate`, `promoted`, `unusable`, `unmapped` 수치를 기록한다.
- 66권별 promoted count 분포를 기록한다.
- 회복 대상 9권(`PSA`, `ECC`, `SNG`, `OBA`, `NAM`, `MAL`, `PHM`, `2JN`, `3JN`)이 모두 1건 이상인지 기록한다.
- report 판정은 품질 평가 및 layer 2 연결 테스트용 사용 가능, 운영 최종 확정 전 샘플 육안 검수 권장으로 둔다.
- workflow와 report 문서만 Git에 포함한다.

## 제외 범위

- PDF 원본 커밋
- `qtai-server/build/**` 산출물 커밋
- restricted storage 배포
- parser, generator, promotion 코드 변경
- DB, API, OpenAPI 변경
- 원문 장문 값 또는 긴 인용문을 report에 수록
- 샘플 육안 검수 자체 수행

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-quality-evaluation.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-index-quality-evaluation_report.md` | 정량 평가 결과와 사용 판단 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `docs/ai-reference-index-quality-evaluation` 브랜치를 생성한다.
2. workflow 문서를 저장한다.
3. `workflow-spec-runner` 기준으로 현재 브랜치와 작업트리를 확인한다.
4. `reference-index.json`과 promotion summary 존재를 확인한다.
5. PowerShell JSON 집계로 전체 지표와 권별 count를 산출한다.
6. 회복 대상 9권 count가 모두 1 이상인지 확인한다.
7. report를 작성하되 원문 장문 값과 긴 인용문은 포함하지 않는다.
8. report에 금지 키워드가 포함되지 않았는지 확인한다.
9. PDF와 `build` 산출물이 ignored 상태이며 stage되지 않았는지 확인한다.
10. 문서 2개만 stage 후 commit message convention에 맞춰 커밋한다.
11. 원격 브랜치로 push한다.
12. `pr-prep-assistant` 기준 PR 제목과 본문을 준비한다.

## 테스트 보강 목록

문서 전용 평가 작업이므로 코드 테스트를 추가하지 않는다.

| 검증 항목 | 확인 내용 |
| --- | --- |
| 산출물 존재 | `reference-index.json`, promotion summary 존재 |
| JSON 집계 | schema, entry/book count, 길이 지표, 빈 텍스트, 중복 hash |
| 권별 분포 | 66권 promoted count 산출 |
| 회복 대상 | 9권 promoted count가 모두 1 이상 |
| Git 안전 | PDF와 `qtai-server/build/**` 미stage |
| report 안전 | 원문 장문 값과 긴 인용문 미수록 |

## 수용 기준

- [ ] workflow 문서가 저장된다.
- [ ] report 문서가 저장된다.
- [ ] report에는 정량 지표와 66권별 count가 포함된다.
- [ ] 9권 promoted count가 모두 1 이상으로 기록된다.
- [ ] report 판정이 품질 평가 및 layer 2 연결 테스트용 사용 가능, 운영 최종 확정 전 샘플 육안 검수 권장으로 기록된다.
- [ ] PDF 원본과 `qtai-server/build/**` 산출물이 stage되지 않는다.
- [ ] 코드 변경이 없다.
- [ ] 문서 2개만 commit된다.
- [ ] 원격 브랜치 push가 완료된다.
- [ ] PR 제목과 본문 초안이 준비된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 workflow/report 문서 2개로 작다.
- 평가 수치와 report 판정을 같은 맥락에서 확인해야 한다.
- 원문 포함 산출물을 다루는 작업이라 편집 범위를 좁게 유지하는 편이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 수치 검증, report, commit, push, PR 초안 준비를 직접 수행한다.

## 검증 계획

```powershell
Get-Item qtai-server/build/ai-review-reference/reference-index.json
Get-Content qtai-server/build/ai-review-reference/reference-index-promotion-summary.json
```

```powershell
# PowerShell JSON 집계로 schema, entry/book count, 길이 지표, 빈 텍스트, 중복 hash, 66권별 count, 9권 count 확인
```

```powershell
git status --short --ignored -- doc/TalkFile_IVP성경배경주석.pdf.pdf qtai-server/build/ai-review-reference
git status --short --branch
```

## 후속 작업으로 남길 항목

- 운영 최종 확정 전 권별 앞/중간/뒤 샘플 육안 검수
- restricted storage 배포
- `validation_reference_jobs.indexStorageUri` 연결 검증
