# 2026-06-07 · 앱 전역 배경음악(브금·찬송가) — 최종 리포트

대상: `QT-AI-2nd-Team-Project` (qtai-server + flutter-app) · 설계: workflows/2026-06-07_app-background-music.md

## 1. 결과 요약

앱 전역 배경음악 기능의 **코드 구현을 완료**했다(백엔드 + Flutter + 개발용 시드 러너). 빌드·실행·시드 적재는 T님 환경(JDK21 / Flutter)에서 수행한다.

사용자 경험:

- 하단바 5탭과 모든 하위/상위 화면에서 배경음악이 끊기지 않고 재생된다(HomeScreen에 전역 플레이어 마운트).
- 오늘의 QT 화면 상단, TTS 버튼 **오른쪽에 음표 토글**이 생겨 음악을 끄고 켤 수 있다.
- 기본값은 **ON** — 로그인/온보딩 후 메인 진입 시 자동 재생(웹은 브라우저 정책상 첫 탭에서 시작될 수 있음).
- 마이페이지 > 설정 > **음악 설정**에서 켜기/끄기·볼륨·종류(전체/브금/찬송가)를 조절하며, 서버(`member_settings`)에 저장된다.

## 2. 음원 현황 (Downloads)

- 브금: 19곡(약 62MB), 찬송가: 13곡(약 89MB), 전부 mp3. 합 약 **151MB**.
- 결정대로 DB(`music_tracks.audio_data` LONGBLOB)에 저장한다(로열티프리/직접제작, `license_note`에 기록).

## 3. 구현 범위

### 백엔드 (qtai-server)
- 신규 도메인 `domain.music`: `MusicTrack`(LONGBLOB) / projection 2종 / `MusicTrackService` / UseCase 2종 / DTO 2종 / `MusicController`
  - `GET /api/v1/music/tracks` (목록, 메타데이터만), `GET /api/v1/music/tracks/{id}/stream` (스트리밍)
- `member_settings` 확장: `music_enabled`(기본 ON)/`music_volume`(70)/`music_category`(ALL) → `GET/PATCH /api/v1/me/settings`
- Flyway `V26 music_tracks`, `V27` settings 컬럼(H2 호환 위해 ALTER 분리, 인라인 INDEX)
- 시드 러너 `MusicSeedRunner`: 레포 번들 음원(`src/main/resources/seed/music/{bgm,hymn}`)을 **클래스패스**에서 읽어 DB가 비면 자동 적재(멱등). 로컬 OS 경로 비의존. `qtai.music.seed.enabled=false`로 끔(테스트는 off)
- ArchUnit `DomainBoundaryArchTest`에 `music` 도메인 등록
- 테스트: `MusicTrackServiceTest`, `MusicControllerTest` 신규 + 기존 설정/알림 테스트 정합

### Flutter (flutter-app) — 신규 `lib/features/music/`
- `models/music_track.dart`, `services/music_repository.dart`
- `providers/music_providers.dart` — 전역 `MusicController`(just_audio, 서버 스트리밍 + JWT 헤더, loop 재생, 서버 설정 동기화)
- `widgets/music_toggle_button.dart` — 오늘의 QT 음표 토글
- `widgets/background_music_host.dart` — 하단바에 마운트되는 보이지 않는 전역 호스트(자동재생)
- `screens/music_settings_screen.dart` — 음악 설정 화면
- 연결: `home_screen`(전역 호스트), `today_qt_screen`(음표 버튼), `settings_screen`(설정 진입), `app_router`(라우트), `settings_response`/`mypage_repository`(음악 필드)

## 4. 실행 방법 (T님)

1) 백엔드 빌드/검증 (JDK21):
```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test
```

2) 음원 시드 — **자동/번들**. 음원 32개(약 157MB)가 레포에 포함(`qtai-server/src/main/resources/seed/music/{bgm,hymn}`)돼 있어, DB가 비어 있으면 서버 시작 시 클래스패스에서 자동 적재된다. 별도 폴더·설정 불필요(어느 PC에서든 git pull 후 실행만).
- 로컬 기본 프로파일은 H2 in-memory라 **재시작마다 재적재**(157MB)된다. 데모 시 JVM 힙을 넉넉히(`-Xmx2g`) 주거나, 영속 보관·반복 재적재 회피는 MySQL 프로파일(`run-dev-web.sh`=dev) 사용 권장.
- 끄려면 `qtai.music.seed.enabled=false`.

3) Flutter 실행: 기존 방식대로(run-dev-web 등). 로그인 후 메인 진입 시 자동 재생.

## 5. 검증 체크 (T님)
- `./gradlew -p qtai-server build test` 통과(컴파일/ArchUnit music 경계/음악 테스트/회귀 없음)
- `flutter analyze`(music 피처 오류 없음), 앱 실행 후 ① 메인 자동재생 ② 음표 토글 ③ 설정 화면 on/off·볼륨·종류

## 6. 남은 일 (follow-up)
- **기준 문서 반영**: `07_요구사항`/`25_기능명세`/`23_용어사전`에 "music 도메인 음원 DB 저장(F-09 예외)" Lead 승인 명문화 — 문서 PR 별도.
- **git 정리**: 현재 체크아웃 브랜치(`feature/flutter-web-run-support`)가 커밋 없는 비정상 상태. `dev` 기준 새 작업 브랜치 생성 후 커밋 권장. PR은 백엔드/Flutter/문서로 분할(CLAUDE.md §12: ≤10파일/≤500줄 지향).
- **레포 용량**: 음원 약 157MB가 레포에 포함된다(파일당 100MB 미만이라 GitHub 푸시 가능). 히스토리 비대화가 부담되면 Git LFS로 mp3 추적 권장: `git lfs track "qtai-server/src/main/resources/seed/music/**/*.mp3"` (커밋 전에 설정).

## 7. 리스크
- 웹 스트리밍은 audio 헤더 제약으로 JWT 첨부가 어려움 → 1차 **안드로이드 우선**(웹은 서명 URL 등 후속).
- DB 용량(약 157MB BLOB), 스트리밍 Range/seek 미지원(배경음 루프엔 영향 적음).

## 8. 후속 변경 (2026-06-07 추가 요청 반영)

- **빌드 검증 완료**: T 로컬에서 `bootRun` 정상 기동. (초기 `audio_data` schema-validation 오류는 엔티티를 `@JdbcTypeCode(SqlTypes.LONGVARBINARY)`로 고쳐 longblob과 일치시켜 해결.)
- **기본 카테고리 ALL→BGM**: `MemberSettings.createDefault`, `V28__default_music_category_bgm.sql`(기존 ALL행 보정 + DEFAULT 변경), Flutter `MusicState`/`SettingsData` 기본값.
- **자동재생 보완**: 네이티브는 진입 즉시, **웹은 첫 터치/클릭에 재생 시작**(브라우저 자동재생 정책 대응 — HomeScreen `Listener`→`notifyUserGesture`).
- **음표 버튼 길게누르기 → 음원 목록 시트**(`MusicTrackSheet`): 카테고리 칩(전체/브금/찬송가) + 곡 목록(현재곡 강조), 탭 시 해당 곡 재생(`MusicController.playByIndex`).
- 적용: 서버 재시작(V28 적용 → 기존 회원 설정 행 BGM 보정) + Flutter hot restart.
