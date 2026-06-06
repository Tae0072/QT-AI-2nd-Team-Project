# Workflow - 2026-06-04 ai-review-reference-heading-parser-recovery

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-reference-heading-parser-recovery` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | PDF에는 존재하지만 promotion 결과에서 entry가 0건인 9권(`PSA`, `ECC`, `SNG`, `OBA`, `NAM`, `MAL`, `PHM`, `2JN`, `3JN`)을 회복해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-pdf-index-diagnostics.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-candidate-promotion.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-book-section-map-candidate.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`AiReviewReferencePdfHeadingParser`가 책 이름이 붙은 heading성 라인과 한 줄에 두 책 경계가 섞인 라인을 인식하도록 보강한다. 기존 순수 장절 heading 파싱과 body line reject 정책은 유지한다.

보강 후 로컬 PDF candidate와 최종 book section map을 사용해 promotion을 다시 실행했을 때, 기존 0건이던 9권의 promoted entry count가 모두 1건 이상이 되도록 한다.

## 범위

- 순수 장절 heading(`1:1-7`) 파싱을 유지한다.
- 책 이름 prefix heading을 추가 인식한다.
  - `시편 8:3 -9:13`
  - `오바다 1:1 -7`
  - `나홍1 :1 -2`
  - `빌레몬서 1: 10-21`
  - `요한이서 1: 1-13`
- 한 줄에 이전 책과 다음 책 heading이 섞이면 rightmost book-heading pair를 사용한다.
  - `잠언 31 : 16-전도서 1 ’ 2` -> `1:2`
  - `전도서 12 :3 아가 1 :3` -> `1:3`
  - `스가랴 14 :20-말라기 1: 12` -> `1:12`
- 관찰된 OCR alias만 제한적으로 허용한다.
  - `오바다` -> `OBA`
  - `나홍` -> `NAM`
  - `벌레몬서`, `벌레몬 서` -> `PHM`
- `ai-review-reference-index-candidate.v1` schema와 promotion output schema는 변경하지 않는다.
- candidate entry의 `bookCode`는 기존처럼 `null`을 유지한다.
- report를 작성한다.

## 제외 범위

- HTTP API, OpenAPI, DB schema, Flyway migration 변경
- restricted storage 배포
- layer 2 prompt 연결 변경
- `reference-index.json` 운영 배포
- PDF 원본, candidate JSON, reference-index JSON 커밋
- 전체 PDF OCR 품질 개선 또는 임의 수동 referenceText 보정
- `unmapped=10` 비본문/색인성 후보 정리

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParser.java` | 책명 prefix/mixed boundary heading 파싱 보강 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfHeadingParserTest.java` | parser 회귀 및 신규 패턴 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferencePdfIndexCandidateGeneratorTest.java` | generator split 동작 회귀 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-heading-parser-recovery_report.md` | 실행 결과와 수동 검증 수치 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. `workflow-spec-runner` 기준으로 현재 브랜치와 작업트리를 확인한다.
3. parser 테스트에 책명 prefix, OCR alias, mixed boundary, body line reject 케이스를 추가한다.
4. generator 테스트에 책명 prefix/mixed boundary line split 회귀를 추가한다.
5. `AiReviewReferencePdfHeadingParser`를 최소 변경으로 보강한다.
6. focused test를 실행한다.
7. 로컬 PDF로 `aiReviewReferencePdfIndexDiagnostics`를 재생성한다.
8. 기존 최종 `ivp-book-section-map.json`으로 `aiReviewReferencePromoteCandidateIndex`를 실행한다.
9. promotion 결과에서 9권 모두 promoted count가 1건 이상인지 확인한다.
10. report를 작성한다.
11. 최종 focused test와 git 상태를 확인한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferencePdfHeadingParserTest` | `시편 8:3 -9:13` 같은 책명 prefix heading 파싱 |
| `AiReviewReferencePdfHeadingParserTest` | `오바다`, `나홍`, `벌레몬 서` OCR alias 파싱 |
| `AiReviewReferencePdfHeadingParserTest` | mixed boundary line에서 마지막 book-heading pair 선택 |
| `AiReviewReferencePdfHeadingParserTest` | 기존 broken/body line reject 정책 유지 |
| `AiReviewReferencePdfIndexCandidateGeneratorTest` | 책명 prefix line이 entry split을 만든다 |
| `AiReviewReferencePdfIndexCandidateGeneratorTest` | mixed boundary line이 새 entry를 만든다 |

## 수용 기준

- [ ] 기존 순수 장절 heading 파싱이 유지된다.
- [ ] 책명 prefix heading과 mixed boundary heading이 파싱된다.
- [ ] body line에 포함된 장절 언급은 heading으로 오탐하지 않는다.
- [ ] promotion CLI가 성공한다.
- [ ] `PSA`, `ECC`, `SNG`, `OBA`, `NAM`, `MAL`, `PHM`, `2JN`, `3JN`의 promoted count가 모두 1 이상이다.
- [ ] map/summary/promotion summary에 `referenceText`, `excerpt`, `본문`이 포함되지 않는다.
- [ ] PDF 원본과 `build/ai-review-reference/*.json`은 Git에 stage되지 않는다.
- [ ] focused test가 통과한다.
- [ ] report가 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- parser, generator, promotion 검증이 같은 내부 계약과 로컬 PDF 산출물에 묶여 있다.
- 테스트와 구현 파일이 `ai/internal` 안에서 겹치며 순차 확인이 더 안전하다.
- workflow 작성, 구현, CLI 재생성, report 작성까지 한 흐름으로 수치를 맞춰야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec 기준으로 TDD, 구현, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferencePdf*Test" --tests "*AiReviewReference*Promotion*Test"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePdfIndexDiagnostics --args="--source ..\doc\TalkFile_IVP성경배경주석.pdf.pdf --output build\ai-review-reference\ivp-reference-index-candidate.json --summary build\ai-review-reference\ivp-reference-index-diagnostics.json"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server aiReviewReferencePromoteCandidateIndex --args="--candidate build\ai-review-reference\ivp-reference-index-candidate.json --book-section-map build\ai-review-reference\ivp-book-section-map.json --output build\ai-review-reference\reference-index.json --summary build\ai-review-reference\reference-index-promotion-summary.json"
```

## 후속 작업으로 남길 항목

- 운영용 `reference-index.json` restricted storage 배포
- `validation_reference_jobs.indexStorageUri` 연결 검증
- 필요 시 `unmapped=10` 비본문/색인성 후보 정리
