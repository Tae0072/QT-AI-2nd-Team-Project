# 2026-06-05 feature/20260604 반영 현황 리포트

## 요약

`feature/20260604` 브랜치에 지금까지 반영된 QT/성경본문 앱 연동, 인증 오류 envelope 정렬, QT slice 테스트 보강, 그리고 현재 작업 트리에 남아 있는 Today QT 후속 앱 화면/스터디 용어 게시 계약 작업을 정리했다.

이 리포트 작성 시점 기준 브랜치는 `origin/dev` 대비 커밋 3개가 앞서 있고, 최신 `origin/dev`에는 추가 커밋 16개가 있어 dev 최신화 병합이 필요하다.

## 작업 브랜치

| 항목 | 내용 |
| --- | --- |
| 기능 브랜치 | `feature/20260604` |
| 원격 브랜치 | `origin/feature/20260604` |
| 기준 dev | `origin/dev` `5c88352` |
| 기준일 | 2026-06-05 |

## 이미 커밋된 반영 내용

| 커밋 | 반영 내용 |
| --- | --- |
| `7211316 feat(app): connect QT and bible passage data` | Flutter 앱에서 QT/성경본문 실제 데이터 흐름을 연결하고, 성경 브라우저 화면과 라우팅, 네트워크 오류 처리, QT import 스케줄러 보강을 반영 |
| `7774799 fix(security): standardize auth error envelope` | 인증/인가 실패 응답을 공통 envelope 규칙에 맞게 정렬 |
| `3556cea fix(test): include security envelope writer in qt slice` | QT web slice 테스트에서 security envelope writer 구성을 포함해 테스트 실패를 보정 |

## 현재 작업 트리 반영 내용

| 영역 | 반영 내용 |
| --- | --- |
| Flutter 성경 브라우저 | 장/절 입력 필드를 picker 기반 선택 UI로 변경하고, 장별 절 수 조회, 영어 본문 토글, 조회 결과 분리 패널을 추가 |
| Flutter Today QT | 영어 본문 기본 숨김/토글 표시, 해설 화면 이동, 노트 작성 화면 이동을 추가 |
| Flutter QT 노트 | QT 본문 기반 노트 작성 화면, 제목/본문 편집, 글씨 크기/굵게/하이라이트/목록/구절 삽입 툴바, `@성경 1:1` 구절 삽입 흐름을 추가 |
| Flutter QT 해설 | `GET /qt/{qtPassageId}/study-content` 응답 모델과 해설/단어 풀이 표시 화면을 추가 |
| Flutter API 테스트 | API client 인증 헤더, 성경 브라우저 picker, Today QT 토글/라우팅, 노트 편집기 동작 테스트를 추가/수정 |
| Server dev 편의 | `dev` profile + `qtai.security.dev-bypass=true` 조건에서 회원이 없을 때 개발용 회원을 1건 seed하는 runner와 테스트를 추가 |
| Server study 계약 | AI 승인 용어를 `GlossaryTerm`으로 게시하고 기존 APPROVED 용어를 HIDDEN 처리하는 use case/interface/DTO/service/repository locking query와 테스트를 추가 |
| 문서 정리 | DevA note-domain completion workflow/report 삭제 상태와 QT integrated API mock report 추가 상태가 작업 트리에 포함됨 |

## 주요 변경 파일

| 파일 | 적용 내용 |
| --- | --- |
| `flutter-app/lib/features/bible/screens/bible_browser_screen.dart` | 성경/장/절 picker, 장별 절 수 로딩, 영어 토글, 결과 패널 구성 |
| `flutter-app/lib/features/bible/screens/today_qt_screen.dart` | 해설/노트 라우팅과 영어 본문 토글 추가 |
| `flutter-app/lib/features/note/screens/qt_note_editor_screen.dart` | QT 노트 작성 화면과 구절 삽입 편집 흐름 추가 |
| `flutter-app/lib/features/study/screens/qt_study_content_screen.dart` | QT 해설/단어 풀이 화면 추가 |
| `flutter-app/lib/features/bible/models/bible_models.dart` | `hasExplanation`, `QtStudyContent`, 해설/용어 DTO 추가 |
| `flutter-app/lib/features/bible/services/bible_repository.dart` | 장별 본문 조회와 QT study-content 조회 API 추가 |
| `qtai-server/src/main/java/com/qtai/domain/study/api/**` | 용어 게시/숨김 UseCase와 command/result DTO 추가 |
| `qtai-server/src/main/java/com/qtai/domain/study/internal/GlossaryTermService.java` | AI 승인 용어 게시/숨김 정책 구현 |
| `qtai-server/src/main/java/com/qtai/domain/member/internal/DevMemberSeedRunner.java` | dev-bypass 개발 회원 seed 추가 |
| `flutter-app/test/**`, `qtai-server/src/test/**` | 변경 흐름 회귀 테스트 추가/수정 |

## 검증 계획

| 명령 | 목적 |
| --- | --- |
| `flutter test test/core/network/api_client_test.dart test/features/bible test/features/note/screens/qt_note_editor_screen_test.dart` | Flutter API/성경/Today QT/노트 화면 회귀 확인 |
| `./gradlew -p qtai-server test --tests "*GlossaryTermServiceTest" --tests "*DevMemberSeedRunnerTest"` | 서버 신규 service/runner 단위 테스트 확인 |
| `./gradlew -p qtai-server build` | 서버 전체 빌드 확인 |

## dev 최신화 전 주의사항

- 최신 `origin/dev`에는 Flutter 테마/탭바, TTS, AI 참조자료, Today QT 노출 흐름 테스트가 추가되어 있어 Flutter 라우팅/Today QT 화면에서 충돌 가능성이 높다.
- 작업 트리에 삭제된 DevA 문서 2건과 새로 추가된 report 1건이 함께 있어 문서 정리 의도 확인이 필요하다.
- `$otion_report.md` 빈 임시 파일은 리포트/코드 범위와 무관하므로 커밋 대상에서 제외한다.
