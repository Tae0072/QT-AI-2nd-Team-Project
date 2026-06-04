# Report - 2026-06-04 ai-review-reference-service

## Summary

- 브랜치: `feature/ai-review-reference-service`
- PR 대상: `dev`
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-service.md`
- layer 2 검수 AI가 최신 ACTIVE `validation_reference_jobs` metadata를 prompt/log에 연결하도록 내부 조회 통로를 추가했다.
- v1 범위에 맞춰 PDF 원문 파싱, 인덱스 조회, 성경 범위별 excerpt 생성은 구현하지 않았다.

## 구현 내용

- `AiReviewReferenceService`를 추가해 최신 ACTIVE validation reference job metadata를 조회하도록 했다.
- `ValidationReferenceJobRepository`에 `findFirstByStatusOrderByCreatedAtDescIdDesc(...)`를 추가했다.
- `AiReviewValidationService`가 reference metadata를 LLM `userPrompt`의 `reference` 블록에 포함하도록 변경했다.
- layer 2 검증 로그 저장 시 `validationReferenceJobId`를 함께 저장하도록 변경했다.
- ACTIVE reference job이 없으면 LLM을 호출하지 않고 `ADVISOR/NEEDS_REVIEW`, `AI_REVIEW_REFERENCE_NOT_FOUND` 로그를 남기도록 했다.

## 제외한 작업

- DB schema 변경 없음
- 관리자 API 또는 OpenAPI 변경 없음
- PDF 업로드, 파싱, 임베딩, 인덱스 조회 구현 없음
- 산출물 성경 범위와 PDF 해설 범위 매칭 구현 없음
- PDF 원문, 긴 excerpt, provider raw response, prompt 원문, secret/token/password/privateKey 저장 없음

## 테스트

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceServiceTest"
```

- 결과: PASS

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewValidationServiceTest"
```

- 결과: PASS

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceServiceTest" --tests "*AiReviewValidationServiceTest"
```

- 결과: PASS

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

- 결과: PASS

## 참고

- Gradle 실행 종료 시 Windows file watcher의 `NativeException: error = 1784` 로그가 출력되었지만, Gradle task 결과는 `BUILD SUCCESSFUL`이었다.
- 검수 AI에 전달되는 reference 정보는 metadata이며, 실제 PDF excerpt 주입은 후속 PR에서 처리한다.

## 후속 작업

- PDF 범위별 해설 추출/인덱스 조회 구현
- 산출물 target range와 PDF 해설 range 매칭 구현
- reference excerpt 또는 summary를 prompt에 주입하는 후속 PR
- `validation_reference_jobs`와 checklist version의 명시적 연결 정책 검토
