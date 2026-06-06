# Report - 2026-06-05 ai-review-layer2-real-index-sample-run

## 요약

- 브랜치: `test/ai-reference-layer2-real-index-sample-run`
- 기준 브랜치: `dev`
- 목적: 실제 로컬 restricted index를 사용해 layer 2 검수 샘플 흐름을 실행하고, 선택된 참조 항목이 prompt metadata에 연결되며 저장 log에는 원문이 남지 않는지 검증
- 평가 파일: `qtai-server/restricted/validation/index/reference-index.json`
- 연결 URI: `restricted://validation/index/reference-index.json`

## 작업 결과

- `AiReviewReferenceLayer2RealIndexSampleManualTest`를 추가했다.
- manual test는 기본 비활성 상태이며 `QTAI_AI_REVIEW_REAL_SAMPLE=true` 또는 test JVM system property로만 실행된다.
- test는 dev profile, dev MySQL, 로컬 restricted root, 실제 `DeepSeekLlmClient`를 사용한다.
- test wrapper는 실제 LLM client에 delegate하면서 prompt metadata만 메모리에 캡처한다.
- sample DB row는 test transaction rollback 대상으로 생성했다.
- sanitized summary는 ignored build output인 `qtai-server/build/ai-review-reference/layer2-real-index-sample-summary.json`에만 생성했다.

## 사전 조건 확인

| 항목 | 결과 |
| --- | --- |
| MySQL container | `qtai-mysql` healthy |
| Redis container | `qtai-redis` healthy |
| localhost:3306 | reachable |
| DeepSeek API key | set, value not recorded |
| real index file | exists, `9423918` bytes |

## 운영형 샘플 실행 결과

| 항목 | 결과 |
| --- | --- |
| reference index schema | `ai-review-reference-index.v1` |
| source file hash | `sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b` |
| entry count | `3021` |
| sample verse | `JHN 3:16` |
| index URI | `restricted://validation/index/reference-index.json` |
| selected reference count | `2` |
| selected hash 1 | `sha256:f090c1fc624266a2fc1a8842aad1eafdc7c43f36dd8dfe904e81321882220a18` |
| selected hash 2 | `sha256:559619a06e4046fcf55d95653e9a64260475af38ac13324094f501b264f42926` |
| selected ranges | `요한복음 3:9-21`, `요한복음 3:14-30` |
| LLM call count | `1` |
| LLM delegate status | `THREW` |
| LLM failure code | `LLM_PROVIDER_REQUEST_REJECTED` |
| layer 2 result | `NEEDS_REVIEW` |
| raw text stored in checklist JSON | `false` |

실제 DeepSeek 호출은 1회 수행됐고, provider reject가 발생했다. 현재 layer 2 서비스는 이 provider 실패를 `NEEDS_REVIEW`로 남기는 경로까지 정상 수행했다. 이번 검증의 목표는 판정 품질이 아니라 real index read, prompt metadata 연결, 저장 경계 확인이므로 acceptance 기준에는 부합한다.

## 발견 및 보강

실제 promoted index의 `generatedAt` 값이 ISO 문자열이 아니라 numeric epoch seconds 형태였다. 기존 reader는 문자열만 허용해 실제 파일을 읽지 못했으므로 다음을 최소 보강했다.

- `AiReviewReferenceIndexReader`: `generatedAt`을 ISO 문자열 또는 numeric epoch seconds로 읽도록 호환 처리
- `AiReviewReferenceCandidatePromotionService`: future promoted index가 ISO 문자열 `generatedAt`을 쓰도록 직렬화 설정 추가
- 관련 reader/promotion 테스트 추가

## 검증 명령

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:QTAI_AI_REVIEW_REAL_SAMPLE='true'
$env:DEEPSEEK_MODEL='deepseek-chat'
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceLayer2RealIndexSampleManualTest" --rerun-tasks
```

결과: `BUILD SUCCESSFUL`

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewValidationServiceTest" --tests "*AiReviewReferenceIndexReaderTest" --tests "*AiReviewReferenceExcerptSelectorTest" --tests "*AiReviewReferenceCandidatePromotionServiceTest"
```

결과: `BUILD SUCCESSFUL`

## 안전 검증

- 실제 PDF, restricted index, build summary는 Git stage 대상에서 제외했다.
- report에는 prompt 전문, provider raw response, 참조 원문을 수록하지 않았다.
- 지정 금지 문자열 검색 결과: 출력 없음.
- staged 민감 산출물 확인 결과: 출력 없음.
- diff whitespace check 결과: 통과.

## 판정

`restricted://validation/index/reference-index.json`는 layer 2 실제 검수 흐름에서 reader를 통해 읽히고, `JHN 3:16` 산출물 verse range에 맞는 참조 항목 2건이 prompt metadata에 연결되는 것을 확인했다. 저장 log에는 원문 대신 URI/hash/range/count 중심 metadata만 남는 경계도 확인했다.

DeepSeek provider reject는 후속으로 provider 요청 형식이나 모델 설정을 조정해 별도 확인할 수 있다. 다만 이번 운영형 샘플 검증 범위에서는 실제 호출 시도와 실패 처리, 저장 경계가 확인되어 사용 연결 검증은 완료로 본다.
