# 워크플로우 — qt 묵상 CRUD 유물 + 데드 인터페이스 제거

- 작업자: Lead 강태오
- 날짜: 2026-06-02
- 대상: QT-AI-2nd-Team-Project (`qtai-server`)
- 기준: CLAUDE.md §3(중복 금지)·§9(관련 없는 변경 최소). 체크리스트 #26→#29 통합

## 1. 배경

체크리스트 #26 "qt CRUD 구현"을 착수하려 했으나, 조사 결과 **구현이 아니라 제거 대상**임이 확인됐다:
- `qt/internal/Qt.java`·`Create/Get/ListMy/Update/DeleteQtUseCase`·`QtCreateRequest/QtResponse/QtUpdateRequest`는 전부 본문이 TODO 주석인 미구현 스텁.
- 주석상 "QT = 회원 묵상 기록"인데, 이 기능은 이미 **note 도메인(MEDITATION/PRAYER/REPENTANCE/GRATITUDE/SERMON, `/api/v1/notes` CRUD)** 으로 완성됨 → 구현 시 중복.
- `04_API_명세서.md`에 `/api/v1/qt` CRUD(POST/PUT/DELETE) 정의 없음. `qt` 테이블 마이그레이션도 없음.

## 2. 범위 (참조 점검 후 안전한 것만)

- 제거: `Qt`(엔티티 스텁), `CreateQt/ListMyQt/UpdateQt/DeleteQtUseCase`, `QtCreateRequest`/`QtUpdateRequest`, qt.api 데드 `GetQtSimulatorUseCase`/`GetQtStudyContentUseCase`(진짜는 study.api), `member.api/GetMemberSettingsUseCase`·`UpdateMemberSettingsUseCase`(실사용은 GetSettingsUseCase), `report.api/GetReportUseCase`. + QtController TODO 주석 정리.
- **보류**: `GetQtUseCase`·`QtResponse`·`QtVisibility` — ai/note/sharing의 client Mock·AI seed 서비스가 참조 중. 제거 시 타 도메인 빌드 영향 → 해당 소유자 조율 필요(재분배 항목으로 남김).

## 3. 절차

1. 각 후보의 src 전역 참조 grep → 자기 파일·주석 외 참조 없는 것만 선별.
2. `refactor/qt-deadcode-cleanup` 브랜치, 12개 파일 `git rm` + QtController 주석 정리(qt 묵상 작성은 note 담당 명시).
3. `./gradlew test` 전체 회귀로 컴파일·테스트 무손상 확인.

## 4. 검증

```powershell
cd qtai-server
.\gradlew.bat test --no-daemon
```
