# 리포트 — MSA service-bible qt·study 이전 (PR #2)

작성일: 2026-06-10 / 작성: Claude (Lead 강태오 워크스페이스) / 브랜치: `feature/msa-qt-study`

## 1. 요약

모놀리식 `qtai-server`의 `qt`·`study` 도메인을 `service-bible` 멀티모듈로 이전했다(Strangler,
원본 유지). PR #1 자동리뷰가 이월 인수조건으로 남긴 항목(통합테스트·인증 헬퍼·표준 페이징·
도메인 경계테스트·구체 예외)을 같은 PR에서 충족해, 첫 푸시부터 APPROVE 품질을 목표로 했다.

## 2. 변경 내역

### 신규 (service-bible)
- `domain/qt/**` — api/internal/web/client 전체 이전 (27파일)
- `domain/study/**` — api/internal/web 전체 이전 (50+파일)
- `domain/note/api/**` — qt가 의존하는 note 계약 최소 폐쇄집합 7파일
  (GetNoteUseCase, NoteCategory/Status/Visibility, NoteDetailResponse/DraftResponse/VerseItem)
- `domain/qt/client/note/GetNoteUseCaseMock.java` — note 통합 전 임시 구현(@Component, DRAFT 없음 응답)
- 테스트: `ControllerSecurityIntegrationTest`, `domain/qt/internal/QtServiceTest`,
  `domain/study/internal/QtStudyAvailabilityServiceTest`, `src/test/resources/application.yml`

### 신규 (lib-common)
- `common/dto/PageResponse.java` — Spring `Page<T>`를 고정 스키마 envelope으로 변환
- `common/security/AuthenticationSupport.java` — `requireMemberId` 인증 헬퍼

### 수정
- `service-bible/build.gradle.kts` — `spring-security-test` testImplementation 추가
- `bible/BibleServiceApplication.java` — `@EnableScheduling`
- `bible/SecurityConfig.java` — 인증 실패 401 / 인가 실패·denyAll 403 표준 응답(entry point·handler)
- `domain/praise/web/PraiseController.java` — `Page<T>` → `PageResponse<T>` 매핑
- `qt/internal/SuTodayPassageImportScheduler.java` — `@ConditionalOnProperty`(테스트 비활성)
- `bible/DomainBoundaryTest.java` — "도메인 상호 무의존"(더 이상 성립 안 함) → "타 도메인 internal import 금지"

## 3. 인수조건 충족 매트릭스 (PR #1 자동리뷰 이월)

| 이월 인수조건 | 충족 방식 |
|---|---|
| Controller MockMvc 통합테스트(qt/study + bible/music/praise) | `ControllerSecurityIntegrationTest` — 미인증 401, 인증 200, admin denyAll 403 |
| 권한 검증 헬퍼(admin_role 이중검증 추상화, denyAll 회귀 방지) | `AuthenticationSupport` + SecurityConfig denyAll + denyAll 회귀 테스트. admin_role 세부검증은 admin-server 담당(분리 설계) |
| `Page<T>` 대신 표준 페이징 envelope DTO | `PageResponse<T>` 도입, PraiseController 적용 |
| 도메인 서비스 단위테스트 + ArchUnit 도메인 경계테스트 | QtServiceTest/QtStudyAvailabilityServiceTest + DomainBoundaryTest(internal import 금지) |
| 광범위 catch(Exception) 금지 / 로그 민감정보 금지 | 이전 코드는 `RuntimeException` 등 구체 예외 사용 유지, Mock·필터 로그에 토큰/민감정보 없음 |

## 4. 주요 설계 결정·근거

- **note만 Mock, ai/member는 placeholder 유지**: 실제 코드에서 qt가 주입받는 cross-service
  의존은 `GetNoteUseCase` 하나뿐이다. `client/ai`·`client/member`는 모놀리식에서도 비어 있는
  TODO stub이라 그대로 이전했다(불필요한 계약·죽은 Mock 양산 회피). 근거: CLAUDE.md §4.
- **note 계약 최소 폐쇄집합만 반입**: `note.api` 전체가 아니라 qt가 닿는 7파일만 가져와
  service-bible에 불필요한 note 표면적을 만들지 않는다.
- **ArchUnit 규칙 정교화**: PR #2에서 qt→bible.api, study→qt.api 같은 합법적 api 의존이
  생겼다. "도메인 무의존"은 더 이상 맞지 않으므로, CLAUDE.md §3의 핵심 불변식("타 도메인 internal
  직접 import 금지")만 강제하도록 커스텀 ArchCondition으로 교체했다.
- **스케줄러 게이팅**: 통합테스트가 컨텍스트를 띄울 때 성서유니온 외부 HTTP가 호출되지 않도록
  `@ConditionalOnProperty(matchIfMissing=true)` + 테스트 yml `qt.today-source.sum.enabled=false`로
  빈 자체를 생성하지 않는다. 운영/로컬은 영향 없음.
- **SecurityConfig 401/403 표준화**: 기존 service-bible은 entry point 미설정이라 미인증이
  403으로 나갔다. lib-common `SecurityErrorResponseWriter`로 미인증 401·인가 실패 403을
  표준 ApiResponse로 통일(모놀리식 거동과 일치).

## 5. 리스크 & 후속 TODO

- **note 통합**: 현재 DRAFT 노트는 항상 "없음". service-note RestClient 어댑터가 진짜
  `GetNoteUseCase`로 등록되면 `GetNoteUseCaseMock` 삭제(CLAUDE.md §4).
- **music 시드 자산**: PR #1과 동일하게 `MUSIC_SEED_ENABLED` 기본 false(150MB 미복제).
- **StudyController**: 모놀리식에서 미구현 stub 상태 그대로 이전(엔드포인트 없음). 실제 목록
  엔드포인트가 필요하면 후속 PR에서 `PageResponse`로 구현.
- **SIMULATOR 시딩**: CLAUDE.md §6대로 00:05 내부 해설 job 시딩 범위에 미포함(불변).

## 6. 검증

호스트 gradlew `:service-bible:build` 통과. 테스트 32개 전부 통과(0 실패/에러/스킵).
빌드가 컴파일 단계에서 truncated 파일을 자동 검출하므로, 마운트 truncation으로 인한 손상은
빌드 GREEN으로 배제됨.
