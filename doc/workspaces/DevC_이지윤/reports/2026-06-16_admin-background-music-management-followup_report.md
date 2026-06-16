# 2026-06-16 관리자 배경음악 관리 후속 수정 리포트

연결 workflow: `doc/workspaces/DevC_이지윤/workflows/2026-06-15_admin-background-music-management.md`

## 수정 배경

- 관리자 배경음악 등록 화면에서 PDF 등 오디오가 아닌 파일을 선택해도 저장이 진행되는 문제가 확인되었다.
- 10 MiB 전후 음원 업로드 시 DB 컬럼/Multipart 제한 처리에 따라 서버 내부 오류로 보일 수 있는 경로를 보강했다.
- PR 리뷰에서 지적된 유휴 API 정합성 항목 중 `GET /api/v1/note-categories`는 제거하지 않고 Flutter 앱에서 실제로 연결하는 방향으로 정리했다.
- `GET /api/v1/bible/verses/by-ids`는 `service-note`, `service-ai` 클라이언트가 실제 사용하는 내부 조회 API로 확인되어 제거 대상에서 제외했다.
- `dev` 최신화 과정에서 Lead의 관리자 회원관리 복원 작업이 반영된 것을 확인하고, `07_요구사항_정의서.md`의 회원관리 범위를 최신 결정과 맞게 조정했다.

## 관리자 배경음악 업로드 보강

- `admin-web/src/pages/MusicTracksPage.tsx`
  - 허용 오디오 MIME/확장자 목록을 추가했다.
  - 파일 선택 시 비오디오 파일이면 `오디오 파일만 등록할 수 있습니다.` 안내를 표시하고 `Upload.LIST_IGNORE`로 저장 대상에서 제외한다.
  - 유효한 오디오 파일을 선택하면 파일 MIME을 form의 `mimeType`에 반영한다.
- `admin-web/scripts/admin-page-contracts.test.mjs`
  - multipart boundary를 브라우저/axios가 설정하도록 유지하는 계약 테스트를 추가했다.
  - 비오디오 파일 차단 UI 계약 테스트를 추가했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/music/web/AdminMusicTrackController.java`
  - 업로드 파일의 실제 multipart content type을 먼저 검증하도록 변경했다.
  - 요청 파라미터로 `mimeType=audio/mpeg`가 들어와도 파일 part가 `application/pdf`이면 `400 C0002`로 차단한다.
- `qtai-server/admin-server/src/main/java/com/qtai/common/exception/GlobalExceptionHandler.java`
  - `MaxUploadSizeExceededException`을 `400 C0002`로 매핑했다.
- `qtai-server/admin-server/src/main/resources/application.yml`
  - admin-server multipart 제한을 `max-file-size: 11MB`, `max-request-size: 12MB`로 설정했다.
  - 실제 비즈니스 한도는 10 MiB이며, 일반적인 초과 업로드는 controller 검증에서 도메인 오류로 응답하도록 resolver 제한을 약간 더 크게 두었다.
- `qtai-server/admin-server/src/main/resources/db/migration/V52__ensure_music_tracks_audio_longblob.sql`
  - 기존 환경에서 `music_tracks.audio_data`가 작은 binary 컬럼으로 생성된 경우를 보정하기 위해 `LONGBLOB` 보장 migration을 추가했다.
- `MusicTrack` 엔티티(admin-server/service-bible)
  - H2 create-drop 테스트 호환을 위해 엔티티의 `columnDefinition = "LONGBLOB"`는 사용하지 않고, `@JdbcTypeCode(SqlTypes.LONGVARBINARY)`와 Flyway migration으로 운영 스키마를 보장한다.

## 노트 카테고리 API 앱 연결

- `flutter-app/lib/features/note/models/note_models.dart`
  - `NoteCategoryOption` 모델과 fallback 작성 가능 카테고리 생성 함수를 추가했다.
- `flutter-app/lib/features/note/services/note_repository.dart`
  - `GET /api/v1/note-categories` 호출용 `getNoteCategories()`를 추가했다.
- `flutter-app/lib/features/note/providers/note_providers.dart`
  - `noteCategoriesProvider`를 추가했다.
- `flutter-app/lib/features/note/screens/note_category_select_screen.dart`
  - 기존 정적 목록 대신 서버 카테고리 중 `writableFromList=true` 항목을 표시하도록 변경했다.
  - API 실패 또는 빈 응답 시 기존 `PRAYER`, `REPENTANCE`, `GRATITUDE` fallback을 사용한다.
- 테스트
  - `flutter-app/test/features/note/services/note_repository_test.dart`
  - `flutter-app/test/features/note/screens/note_category_select_screen_test.dart`

## 관리자 회원관리 범위 문서 정합화

- `doc/프로젝트 문서/07_요구사항_정의서.md`
  - 관리자 회원관리는 Lead 결정(`doc/workspaces/Lead_강태오/workflows/2026-06-15_admin-member-management-restore.md`)에 따라 MVP 관리자 웹 범위에 포함한다고 정리했다.
  - 포함 범위: 회원 목록·상세 조회, 신고 처리와 연결된 제재/해제, 닉네임 변경 이력, 회원 활동 조회.
  - 제외 범위: 개인정보 직접 수정, 관리자 계정 생성·권한 변경 화면.

## dev 최신화 및 push

- `origin/dev`를 현재 브랜치 `feature/admin-background-music-management`에 merge했다.
- 충돌은 최신 `dev`의 관리자 회원관리/배경음악 구조를 기준으로 정리하고, 오늘 수정한 업로드 검증·노트 카테고리 연결 변경을 다시 반영했다.
- migration 번호 충돌을 피하기 위해 `V46__ensure_music_tracks_audio_longblob.sql`을 `V52__ensure_music_tracks_audio_longblob.sql`로 조정했다.
- 커밋:
  - `23e1f2b9 fix(admin-music): harden uploads and align note categories`
- push:
  - `feature/admin-background-music-management`

## 검증

- 사용자 요청에 따라 push 직전 로컬 테스트 재실행은 생략했다.
- 이전 확인 사항:
  - PDF 업로드 API 수동 호출 시 `400 C0002` 응답 확인.
  - 관리자웹에서 비오디오 파일 선택 시 저장 전 안내 및 차단 동작 확인.
  - `:admin-server:test --tests "*Music*"` 이전 통과 확인.
  - `admin-web npm test`, `npm run typecheck` 이전 통과 확인.

## 남은 리스크

- 오늘 추가한 Flutter `note-categories` 연결 테스트는 push 직전 재실행하지 않았다. CI 또는 별도 로컬 실행으로 확인 필요.
- `.env.example`, `tmp/`는 작업 범위 밖 로컬 변경으로 커밋/푸시에서 제외했다.
