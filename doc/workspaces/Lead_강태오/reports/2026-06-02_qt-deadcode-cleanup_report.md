# 리포트 — qt 묵상 CRUD 유물 + 데드 인터페이스 제거

- 작업자: Lead 강태오 · 날짜: 2026-06-02
- 브랜치: `refactor/qt-deadcode-cleanup` → PR 대상 `dev`
- 관련: 체크리스트 #26(qt CRUD)→제거로 전환 + #29(데드코드 정리) 일부

## 1. 한 줄 요약

"qt CRUD"는 구현이 아니라 **note 도메인과 중복되는 미구현 유물**이라 제거했다. 함께 격리된 데드 인터페이스(member settings·report·qt simulator/study)도 정리. 전체 회귀 626건 통과.

## 2. 판단 근거

- qt 묵상 CRUD 스텁(주석상 "QT=회원 묵상 기록")은 `note` 도메인(MEDITATION 등 `/api/v1/notes`)으로 이미 완성 → 구현 시 테이블·API 중복.
- API 명세에 `/api/v1/qt` CRUD 정의 없음, `qt` 테이블 마이그레이션 없음.

## 3. 제거 파일 (12개)

| 도메인 | 파일 |
|------|------|
| qt | `internal/Qt.java`, `api/{CreateQt,ListMyQt,UpdateQt,DeleteQt}UseCase.java`, `api/dto/{QtCreateRequest,QtUpdateRequest}.java`, `api/{GetQtSimulatorUseCase,GetQtStudyContentUseCase}.java`(데드, 진짜는 study.api) |
| member | `api/{GetMemberSettingsUseCase,UpdateMemberSettingsUseCase}.java`(실사용은 GetSettingsUseCase) |

+ `qt/web/QtController.java` TODO 주석 정리(묵상 작성은 note 담당 명시).

> 본 PR은 강태오 단독 소유 경로(qt·member)만 건드려 CODEOWNERS 타인 리뷰 없이 머지되도록 범위를 한정했다.

## 4. 보류 (타 도메인 조율 필요)

- `report.api/GetReportUseCase`(미참조 데드) — report 도메인은 김지민·이승욱·이지윤 공동 소유(CODEOWNERS)라, 이 PR에서 빼고 report 소유자와 별도 정리.
- `GetQtUseCase`·`QtResponse`·`QtVisibility` — ai/note/sharing의 `client/qt/GetQtUseCaseMock`와 `AiDailyQtVerseExplanationSeedService`가 참조. 제거 시 그쪽 빌드 영향 → 소유자(강상민·김지민·이승욱) 조율 후 별도 정리.

## 5. 검증

- `./gradlew test` → **BUILD SUCCESSFUL, 626건 통과(실패 0)**. compileJava 단독도 통과(삭제가 컴파일 무손상).
