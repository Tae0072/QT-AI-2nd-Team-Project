# Report - 2026-06-05 ai-review-reference-restricted-index-link

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `chore/ai-reference-restricted-index-link` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md` |
| 실행 방식 | `workflow-spec-runner` 기준 직접 실행 |
| 연결 URI | `restricted://validation/index/reference-index.json` |
| 관련 F-ID | 해당 없음 |

## 작업 요약

AI 검수 참조자료 index를 로컬 restricted storage 경로에 배치하고, 해당 경로가 Git에서 제외되는지 확인했다. `indexStorageUri` 값으로 사용할 URI는 `restricted://validation/index/reference-index.json`이다.

이번 작업은 파일/reader 수준 연결 검증으로 한정했다. DB row 생성과 system API 호출 검증은 후속 작업으로 남긴다.

## 변경 내용

| 구분 | 경로 | 내용 |
| --- | --- | --- |
| Modify | `qtai-server/.gitignore` | `restricted/` 추가 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-restricted-index-link_report.md` | 실행 결과 기록 |
| Ignored | `qtai-server/restricted/validation/index/reference-index.json` | 로컬 restricted storage 배치 파일 |

## Restricted Storage 배치

| 항목 | 값 |
| --- | --- |
| restricted root | `qtai-server/restricted` |
| index path | `qtai-server/restricted/validation/index/reference-index.json` |
| index URI | `restricted://validation/index/reference-index.json` |
| file size | `9423918` bytes |

`git check-ignore` 결과:

```text
qtai-server/.gitignore:4:restricted/    qtai-server\restricted\validation\index\reference-index.json
```

## Index 계약 검증

| 항목 | 값 |
| --- | ---: |
| schemaVersion | `ai-review-reference-index.v1` |
| sourceFileHash | `sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b` |
| entry count | 3021 |
| book count | 66 |
| PDF hash match | `true` |

## 회복 대상 9권 확인

| bookCode | count |
| --- | ---: |
| `PSA` | 57 |
| `ECC` | 8 |
| `SNG` | 8 |
| `OBA` | 2 |
| `NAM` | 3 |
| `MAL` | 3 |
| `PHM` | 4 |
| `2JN` | 1 |
| `3JN` | 1 |

9권 모두 count가 1 이상이다.

## 검증 명령

```powershell
git check-ignore -v qtai-server\restricted\validation\index\reference-index.json
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
```

- 결과: PDF, `build`, `restricted` 모두 ignored 상태

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*RestrictedStorageUriResolverTest" --tests "*AiReviewReferenceIndexReaderTest"
```

- 결과: `BUILD SUCCESSFUL`

## Stage 안전 확인

커밋 대상은 다음 3개로 제한한다.

- `qtai-server/.gitignore`
- `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-restricted-index-link_report.md`

다음은 Git에 포함하지 않는다.

- `qtai-server/restricted/**`
- `qtai-server/build/**`
- `doc/TalkFile_IVP성경배경주석.pdf.pdf`

## 보안 안전장치 판단

`restricted/` ignore는 로컬 산출물이 일반 stage에 포함되는 것을 막는 1차 안전장치다. 다만 `.gitignore`는 `git add -f` 강제 stage를 차단하지 못한다.

따라서 다음 작업에서 pre-commit 또는 CI hook으로 아래 경로의 stage를 차단해야 한다.

- `qtai-server/restricted/**`
- `qtai-server/build/**`
- `doc/TalkFile_IVP*.pdf*`

## PR 설명 링크 확인

PR 설명에는 다음 문서 경로를 포함해야 한다.

- `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-restricted-index-link.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-restricted-index-link_report.md`

## 후속 작업

- pre-commit 또는 CI hook으로 민감 검증 참조자료 경로 강제 stage 차단
- DB row 생성 또는 system API 호출 방식의 `validation_reference_jobs.indexStorageUri` 연결 검증
- 운영 환경의 `QTAI_RESTRICTED_STORAGE_ROOT` 값 확정
- 운영 restricted storage 배포
