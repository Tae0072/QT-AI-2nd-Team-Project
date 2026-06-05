# Workflow - 2026-06-05 ai-review-reference-restricted-index-link

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/ai-reference-restricted-index-link` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 품질 평가를 마친 AI 검수 참조자료 index를 로컬 restricted storage에 배치하고 `indexStorageUri`로 읽을 수 있는지 검증해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-index-quality-evaluation_report.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-reader.md` |
| 대상 경로 | `qtai-server/.gitignore`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

로컬 `qtai-server/build/ai-review-reference/reference-index.json`을 `qtai-server/restricted/validation/index/reference-index.json`에 배치한다. 서버 코드가 사용하는 `restricted://validation/index/reference-index.json` URI가 해당 파일을 가리키는지 파일/reader 수준에서 검증한다.

원문 포함 JSON은 Git에 포함하지 않는다. 커밋 대상은 `restricted/` ignore 안전장치와 workflow/report 문서로 제한한다.

## 범위

- `qtai-server/.gitignore`에 `restricted/`를 추가한다.
- `qtai-server/restricted/validation/index/reference-index.json` 파일을 생성 또는 갱신한다.
- `restricted://validation/index/reference-index.json` 연결 URI를 검증한다.
- schema, source hash, entry count, book count, 회복 대상 9권 count를 확인한다.
- PDF hash와 index source hash 일치를 확인한다.
- report를 작성한다.

## 제외 범위

- `qtai-server/restricted/**` 커밋
- `qtai-server/build/**` 커밋
- PDF 원본 커밋
- DB row 생성 또는 API 호출 검증
- restricted storage의 외부 클라우드 배포
- parser, generator, promotion 코드 변경
- 장문 원문 값 또는 긴 인용문 report 수록

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/.gitignore` | restricted 로컬 저장소 Git 제외 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-restricted-index-link_report.md` | 실행 결과와 검증 기록 |
| Ignored | `qtai-server/restricted/validation/index/reference-index.json` | 로컬 제한 저장소 index 배치 파일 |

## 구현 순서

1. `dev` 최신 상태에서 `chore/ai-reference-restricted-index-link` 브랜치를 생성한다.
2. `qtai-server/.gitignore`에 `restricted/`를 추가한다.
3. workflow spec을 저장한다.
4. `workflow-spec-runner` 기준으로 현재 브랜치와 작업트리를 확인한다.
5. `qtai-server/restricted/validation/index` 디렉터리를 생성한다.
6. `build` 산출물의 `reference-index.json`을 restricted 경로로 복사한다.
7. `git check-ignore`로 restricted 파일이 ignored 상태인지 확인한다.
8. JSON 지표와 PDF hash 일치를 검증한다.
9. reader/resolver focused test를 실행한다.
10. report를 작성하고 원문성 키워드가 없는지 확인한다.
11. stage 대상이 `.gitignore`, workflow, report 3개뿐인지 확인한다.
12. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

코드 변경이 없으므로 테스트 파일을 추가하지 않는다.

| 검증 항목 | 확인 내용 |
| --- | --- |
| restricted 파일 배치 | `qtai-server/restricted/validation/index/reference-index.json` 존재 |
| ignore 안전장치 | restricted 파일이 Git ignored 상태 |
| JSON 계약 | schema, source hash, entry count, book count |
| hash 일치 | PDF hash와 index source hash 일치 |
| 회복 대상 | 9권 count가 모두 1 이상 |
| reader 계약 | 기존 resolver/reader focused test 통과 |
| stage 안전 | 원문 포함 파일 미stage |

## 수용 기준

- [ ] restricted index 파일이 로컬 제한 저장소 경로에 배치된다.
- [ ] `restricted://validation/index/reference-index.json`가 검증 대상 URI로 기록된다.
- [ ] restricted 파일이 Git ignored 상태다.
- [ ] schema가 `ai-review-reference-index.v1`이다.
- [ ] source hash가 PDF hash와 일치한다.
- [ ] entry count가 `3021`, book count가 `66`이다.
- [ ] 회복 대상 9권 count가 모두 1 이상이다.
- [ ] focused test가 통과한다.
- [ ] report에 장문 원문 값과 긴 인용문이 포함되지 않는다.
- [ ] stage 대상은 `qtai-server/.gitignore`, workflow, report 3개뿐이다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 ignore 안전장치와 문서 2개로 작다.
- 원문 포함 JSON을 로컬에 배치하므로 stage 안전 확인을 한 흐름에서 수행해야 한다.
- DB/API 검증 없이 파일/reader 검증으로 범위가 명확하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 파일 배치, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
git check-ignore -v qtai-server\restricted\validation\index\reference-index.json
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
```

```powershell
$idx = Get-Content -LiteralPath 'qtai-server\restricted\validation\index\reference-index.json' -Raw -Encoding UTF8 | ConvertFrom-Json
$idx.schemaVersion
$idx.sourceFileHash
@($idx.entries).Count
@($idx.entries | Group-Object bookCode).Count
```

```powershell
$pdfHash = 'sha256:' + (Get-FileHash -LiteralPath 'doc\TalkFile_IVP성경배경주석.pdf.pdf' -Algorithm SHA256).Hash.ToLower()
$idx.sourceFileHash -eq $pdfHash
```

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*RestrictedStorageUriResolverTest" --tests "*AiReviewReferenceIndexReaderTest"
```

## 후속 작업으로 남길 항목

- DB row 생성 또는 system API 호출 방식의 `validation_reference_jobs.indexStorageUri` 연결 검증
- 운영 환경의 `QTAI_RESTRICTED_STORAGE_ROOT` 값 확정
- 운영 restricted storage 배포
