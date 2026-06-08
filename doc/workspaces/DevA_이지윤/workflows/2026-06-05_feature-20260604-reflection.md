# 2026-06-05 feature/20260604 반영 현황 workflow

## 1. 목적

`feature/20260604` 브랜치에 이미 반영된 QT/성경본문 앱 연동, Today QT 후속 화면, QT 노트/해설 진입점, study 용어 게시 계약, dev 최신화 병합 상태를 정리한다.

이 문서는 원래 report 작성 전에 만들어야 하는 workflow 문서지만, 이번 작업에서는 리포트가 먼저 작성되어 사후 보강 문서로 작성한다. 따라서 구현 계획보다는 현재 브랜치 반영 범위와 검증 기준을 맞추는 데 목적을 둔다.

## 2. 작업 브랜치

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/20260604` |
| PR 대상 | `dev` |
| 관련 F-ID | F-01, F-03, F-08, F-12, F-16 |
| 트리거 | `feature/20260604` 브랜치 반영 내용과 dev 최신화 상태를 문서화 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |

## 3. 범위

- Flutter Today QT 화면에서 한글 본문 기본 표시, 영어 본문 토글 표시를 유지한다.
- Today QT에서 해설 화면과 QT 노트 작성 화면으로 이동할 수 있게 한다.
- 성경 브라우저에서 성경/장/절 picker 기반 조회 흐름을 유지한다.
- QT 노트 작성 화면에서 본문 참조, 서식 툴바, 구절 삽입, 저장/임시저장 흐름을 제공한다.
- QT 해설 화면에서 summary, 절별 해설, 단어 풀이를 표시한다.
- 서버 study 도메인에 AI 승인 용어 게시/숨김 UseCase와 service 테스트를 추가한다.
- dev-bypass 개발 환경에서 기본 회원 seed runner를 추가한다.
- 최신 `origin/dev`를 병합하고 TTS/일반 노트 화면/탭바 변경과 충돌 없이 공존시킨다.
- 작업 결과를 report 문서로 정리한다.

## 4. 제외 범위

- 실제 AI 해설 생성, 검수, 승인 플로우 구현은 제외한다.
- 시뮬레이터 상세 화면 구현은 제외한다.
- 노트 공유, 달력, 일반 자유노트 전체 플로우는 dev 병합분을 유지하되 이번 workflow의 직접 구현 범위로 보지 않는다.
- 실제 성경 본문 seed, 금지 번역본 fixture, 외부 본문 텍스트 저장은 추가하지 않는다.
- OpenAPI 계약 전체 갱신은 이번 문서 보강 범위에서 제외한다.

## 5. 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `flutter-app/lib/features/bible/screens/today_qt_screen.dart` | Today QT 액션, 영어 토글, TTS 병합 |
| Modify | `flutter-app/lib/features/bible/screens/bible_browser_screen.dart` | 성경/장/절 picker UI |
| Create | `flutter-app/lib/features/note/screens/qt_note_editor_screen.dart` | QT 묵상 노트 작성 화면 |
| Create | `flutter-app/lib/features/study/screens/qt_study_content_screen.dart` | QT 해설/단어 풀이 화면 |
| Modify | `flutter-app/lib/features/bible/models/bible_models.dart` | Today QT와 study-content DTO |
| Modify | `flutter-app/lib/features/bible/services/bible_repository.dart` | QT/성경본문/study-content API 호출 |
| Create/Modify | `qtai-server/src/main/java/com/qtai/domain/study/**` | 용어 게시/숨김 UseCase와 구현 |
| Create | `qtai-server/src/main/java/com/qtai/domain/member/internal/DevMemberSeedRunner.java` | dev-bypass 기본 회원 seed |
| Test | `flutter-app/test/**`, `qtai-server/src/test/**` | 변경 흐름 회귀 테스트 |
| Create | `doc/workspaces/DevA_이지윤/reports/2026-06-05_feature-20260604-reflection_report.md` | 반영 현황 리포트 |

## 6. 구현 순서

1. `feature/20260604` 브랜치의 기존 커밋과 작업 트리 변경을 확인한다.
2. 현재 반영 내용을 Flutter, server, docs 영역으로 나눠 정리한다.
3. QT 노트/해설/성경 브라우저 후속 변경을 하나의 커밋으로 묶는다.
4. `origin/dev` 최신을 fetch하고 현재 브랜치에 병합한다.
5. Flutter 충돌은 dev의 TTS/일반 노트/탭바 변경과 QT 노트/해설 진입점을 모두 살리는 방향으로 정리한다.
6. 중복된 `QtStudyContent` 모델과 repository 메서드를 하나로 합친다.
7. 변경 범위 테스트와 build/analyze를 실행한다.
8. report를 작성하고 원격 `feature/20260604`에 push한다.

## 7. 수용 기준

| 기준 | 완료 조건 |
| --- | --- |
| 문서 | workflow/report가 같은 날짜와 slug 기준으로 존재한다. |
| dev 최신화 | `origin/dev` 병합 후 원격 feature 브랜치와 로컬이 일치한다. |
| Flutter | Today QT, 성경, QT 노트, 라우터 관련 테스트가 통과한다. |
| Server | 신규 study/member 테스트와 전체 build가 통과한다. |
| 품질 | `git diff --check`, `flutter analyze`가 통과한다. |
| 금지 데이터 | 실제 성경 본문 seed와 금지 번역본 데이터가 추가되지 않는다. |

## 8. Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 문서는 사후 보강용 workflow라 실제 병렬 구현 이점이 작다.
- 충돌 해결은 Flutter 라우팅, Today QT, TTS, 노트 화면이 서로 맞물려 있어 메인 에이전트가 한 번에 보는 편이 안전하다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 문서 작성, 병합 충돌 해소, 검증, push를 직접 수행한다.

## 9. 검증 계획

- `git diff --check`
- `flutter test test/core/network/api_client_test.dart test/features/bible test/features/note/screens/qt_note_editor_screen_test.dart test/routes/app_router_test.dart`
- `flutter analyze`
- `./gradlew -p qtai-server test --tests "*GlossaryTermServiceTest" --tests "*DevMemberSeedRunnerTest"`
- `./gradlew -p qtai-server build`

## 10. 후속 작업

- GitHub Actions에서 feature 브랜치 기준 CI 결과를 확인한다.
- OpenAPI에 QT study-content와 노트 진입점 계약 반영이 필요한지 별도 확인한다.
- 시뮬레이터 실제 화면/상세 API 연동은 별도 workflow로 분리한다.
