# Report - 2026-06-04 ai-review-reference-heading-parser-recovery

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `feature/ai-reference-heading-parser-recovery` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-heading-parser-recovery.md` |
| 실행 방식 | `workflow-spec-runner` 기준 직접 실행 |
| 관련 F-ID | 해당 없음 |

## 작업 요약

PDF index heading parser가 책 이름 prefix가 붙은 heading과 책 경계가 한 줄에 섞인 OCR 라인을 인식하도록 보강했다. 기존 순수 장절 heading 파싱과 일반 body line reject 정책은 유지했다.

이번 변경은 candidate schema, promotion output schema, HTTP API, OpenAPI, DB, restricted storage 배포를 변경하지 않는다. candidate entry의 `bookCode`도 기존 계약대로 `null`을 유지한다.

## 변경 파일

| 구분 | 경로 | 내용 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-heading-parser-recovery.md` | 작업 명세 작성 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParser.java` | 책명 prefix/mixed boundary heading 파싱 보강 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParserTest.java` | parser 신규 패턴과 reject 회귀 테스트 추가 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexCandidateGeneratorTest.java` | generator entry split 회귀 테스트 추가 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-heading-parser-recovery_report.md` | 실행 결과 기록 |

## 구현 내용

- 순수 heading(`1:1`, `1:1-7`, `1:1-2:3`)은 기존 정규식 경로로 먼저 처리한다.
- 기존 파싱이 실패한 경우에만 책명 prefix heading을 검사한다.
- 책명 prefix 검사는 공백과 관찰된 OCR 구두점(`’`, `‘`, fullwidth colon 등)을 정규화한 뒤 수행한다.
- 책 catalog의 한글명 66권과 관찰 alias `오바다`, `나홍`, `벌레몬서`, `벌레몬 서`만 허용한다.
- 한 줄에 두 책 경계가 섞이면 오른쪽에 있는 책명-heading pair를 선택한다.
- 책명 뒤 suffix 전체가 heading 형식일 때만 인정해, body line 오탐을 제한한다.

## 테스트 결과

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferencePdf*Test" --tests "*AiReviewReference*Promotion*Test"
```

- 1차 실행: 신규 테스트 5건 실패 확인
- 구현 후 실행: `BUILD SUCCESSFUL`
- 요한이서/요한삼서 명시 테스트 추가 후 재실행: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

- 결과: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server build
```

- 결과: `BUILD SUCCESSFUL`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test jacocoTestReport jacocoTestCoverageVerification
```

- 결과: 실패
- 사유: 현재 `qtai-server` Gradle 프로젝트에 `jacocoTestReport` task가 없고, 저장소에서 Jacoco 설정도 발견되지 않았다.

```powershell
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
```

- 실행하지 않음
- 사유: 이번 작업은 OpenAPI 변경이 없으며, 현재 저장소 루트에 `.spectral.yaml`과 `apis/` 경로가 없다.

```powershell
gitleaks detect --source . --redact --exit-code 1
```

- 실행하지 않음
- 사유: 현재 PC에 `gitleaks` 명령이 설치되어 있지 않다.

## CLI 검증 결과

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePdfIndexDiagnostics --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-reference-index-candidate.json --summary build\ai-review-reference\ivp-reference-index-diagnostics.json"
```

- 결과: `entries=3184 usable=0 needsReview=3031 unusable=153`
- candidate schema: `ai-review-reference-index-candidate.v1`
- candidate non-null `bookCode` count: `0`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferenceBookSectionMapCandidate --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-book-section-map-candidate.json --summary build\ai-review-reference\ivp-book-section-map-candidate-summary.json"
```

- 결과: `detected=64 high=39 low=25 missing=2 duplicates=1284`

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePromoteCandidateIndex --args="--candidate build\ai-review-reference\ivp-reference-index-candidate.json --book-section-map build\ai-review-reference\ivp-book-section-map.json --output build\ai-review-reference\reference-index.json --summary build\ai-review-reference\reference-index-promotion-summary.json"
```

- 결과: `candidate=3184 promoted=3021 unusable=153 unmapped=10`

## 누락 9권 회복 결과

| bookCode | promoted count |
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

9권 모두 promotion 결과에서 1건 이상으로 회복됐다.

## 산출물 안전 확인

- `ivp-book-section-map.json`, `ivp-book-section-map-candidate-summary.json`, `reference-index-promotion-summary.json`에서 `referenceText`, `excerpt`, `본문` 검색 결과: `NO_MATCH`
- PDF 원본과 `qtai-server/build/ai-review-reference/*.json`은 Git ignore 대상이며 stage하지 않았다.
- HTTP API, OpenAPI, DB, restricted storage 배포는 변경하지 않았다.

## 후속 작업

- 운영용 `reference-index.json` restricted storage 배포
- `validation_reference_jobs.indexStorageUri` 연결 검증
- 필요 시 `unmapped=10` 비본문/색인성 후보 정리
