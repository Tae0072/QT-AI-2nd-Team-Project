# Report - 2026-05-26 bible-read-api-spec

## 작업 요약

Bible Read API와 Study 승인 해설 read model 작업에 대해 코드 리뷰 지적 사항을 반영했다.
이번 보정의 핵심은 누락 테스트 추가, 인증 검증 보강, 도메인 경계 테스트 확대, OpenAPI 타입과 DTO 타입 정합성 보정이다.

## 관련 F-ID

- F-01 로그인 강제 정책
- F-03 묵상/해설 조회 연계
- F-08 성경 본문 조회
- F-16 검증 통과 콘텐츠만 노출

## 리뷰 반영 내용

- `BibleService.getVerse(Long verseId)` 정상/실패 경로 테스트를 추가했다.
- `VerseExplanationService.listApprovedByVerseIds()`의 `null`, empty 입력 경계 테스트를 추가했다.
- `BibleControllerSecurityTest`를 추가해 인증 없음 401, 인증 사용자 200 경로를 검증했다.
- `ArchitectureBoundaryTest`가 `bible`뿐 아니라 `study`의 `internal/web` 직접 import도 검증하도록 확장했다.
- `BibleBookResponse.id`, `displayOrder`를 `Integer`로 변경해 OpenAPI `int32` 계약과 맞췄다.
- `@Cacheable("bible-books")`를 제거해 eviction 정책 없는 캐시 지적을 해소했다.
- `VerseExplanationReadModelTest`에서 `bible.internal` 직접 import를 제거하고 `bibleVerseId` 기반 fixture로 단순화했다.
- Bible Read API PR 범위와 무관한 `qt-integrated-api-mock` workflow 문서 2개를 PR diff에서 제거했다.
- 테스트 fixture 본문은 실제 성경 본문이나 금지 번역본이 아닌 더미 문자열만 사용했다.

## 주요 변경 파일

- `qtai-server/src/main/java/com/qtai/domain/bible/api/dto/BibleBookResponse.java`
- `qtai-server/src/main/java/com/qtai/domain/bible/internal/BibleService.java`
- `qtai-server/src/test/java/com/qtai/domain/bible/internal/BibleServiceTest.java`
- `qtai-server/src/test/java/com/qtai/domain/bible/web/BibleControllerTest.java`
- `qtai-server/src/test/java/com/qtai/domain/bible/web/BibleControllerSecurityTest.java`
- `qtai-server/src/test/java/com/qtai/domain/study/internal/VerseExplanationServiceTest.java`
- `qtai-server/src/test/java/com/qtai/domain/study/internal/VerseExplanationReadModelTest.java`
- `qtai-server/src/test/java/com/qtai/common/ArchitectureBoundaryTest.java`
- `doc/workspaces/DevA_이지윤/workflows/2026-05-26_qt-integrated-api-mock.md` 삭제
- `doc/workspaces/DevA_이지윤/workflows/2026-05-26_qt-integrated-api-mock(수정).md` 삭제

## 검증 결과

성공:

```bash
.\gradlew.bat test --tests "*Bible*" --tests "*VerseExplanation*" --tests "*ArchitectureBoundaryTest"
```

결과:

- `BUILD SUCCESSFUL`
- Bible 관련 테스트 통과
- Study VerseExplanation 관련 테스트 통과
- ArchitectureBoundaryTest 통과

## 금지 데이터 확인

- 테스트에는 `Genesis Korean`, `test korean body`, `test explanation` 같은 더미 문자열만 사용했다.
- 개역개정, ESV, NIV 본문 데이터는 추가하지 않았다.
- 성서유니온/두란노 본문 텍스트도 추가하지 않았다.

추가 성공:

```bash
.\gradlew.bat build
git diff --check
```

## 남은 리스크 / 후속 작업

- coverage 태스크는 현재 Gradle 스크립트에 정의되어 있지 않아 별도 설정 PR이 필요하다.
- spectral은 저장소 루트와 서버 루트에 `.spectral.yaml`이 없어 실행하지 못했다.
- gitleaks는 로컬 명령이 없어 실행하지 못했다.
