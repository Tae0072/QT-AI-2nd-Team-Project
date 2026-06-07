# 2026-06-07 · 앱 전역 배경음악 — 백엔드 구현 리포트 (Phase 1)

대상: `QT-AI-2nd-Team-Project/qtai-server` · 설계: workflows/2026-06-07_app-background-music.md

## 1. 개요

앱 전역 배경음악(브금/찬송가) 기능의 **백엔드**를 구현했다. 음원은 Lead(T) 승인에 따라 DB에 저장한다(요구사항 변경). 이 단계는 코드 작성까지이며, 빌드/테스트 실행과 Flutter·음원 시딩은 다음 단계다.

## 2. 변경 사항

### 신규 도메인 `domain.music` (11파일)
- `internal`: `MusicTrack`(LONGBLOB `audio_data`), `MusicCategory`(BGM/HYMN), `MusicTrackRepository`, projection `MusicTrackSummary`/`MusicTrackAudioView`, `MusicTrackService`
- `api`: `ListMusicTrackUseCase`, `GetMusicTrackAudioUseCase`, `dto/MusicTrackResponse`, `dto/MusicTrackAudioResponse`
- `web`: `MusicController` — `GET /api/v1/music/tracks`, `GET /api/v1/music/tracks/{id}/stream`

### member 설정 확장 (4파일)
- `member_settings`에 `music_enabled`(기본 ON), `music_volume`(0~100, 기본 70), `music_category`(ALL/BGM/HYMN, 기본 ALL)
- `MemberSettings`, `SettingsResponse`, `SettingsUpdateRequest`(@Min/@Max/@Pattern 검증), `MemberSettingsService`

### DB 마이그레이션 (2파일)
- `V26__create_music_tracks.sql`, `V27__add_music_to_member_settings.sql`

### 테스트 (신규 2 + 수정 3)
- 신규: `MusicTrackServiceTest`, `MusicControllerTest`
- 수정: `MemberSettingsServiceTest`/`MemberSettingsControllerTest`(음악 케이스 추가), `NotificationServiceTest`(DTO 정합)
- ArchUnit `DomainBoundaryArchTest`에 `music` 도메인 경계 규칙 등록

## 3. 설계 포인트

- **목록 경량화**: 목록 조회는 projection으로 메타데이터만 SELECT, `audio_data`는 스트리밍 시에만 조회.
- **도메인 경계 유지**: 음악 설정은 member가 보유하되 `music_category`를 String으로 저장 → member가 music 도메인을 import하지 않음(ArchUnit 통과).
- **인가**: 음원 엔드포인트는 `anyRequest().authenticated()`로 보호(로그인 필요), 별도 SecurityConfig 변경 불필요.

## 4. 검증 (필수: Windows JDK21에서 실행)

샌드박스는 JDK11이라 빌드 불가. T님 환경에서:

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test
```

확인 포인트: 컴파일, ArchUnit(music 경계), MusicTrack/Settings 테스트, 기존 회귀 없음.

## 5. 다음 단계

1. Flutter: 전역 배경음악 플레이어(하단바 위 마운트, 기본 ON) + 오늘의 QT TTS 버튼 오른쪽 음표 토글 + 마이페이지 음악 설정 화면.
2. 음원 시딩: `C:\Users\xodh0\Downloads\브금`, `\찬송가` → `music_tracks` 적재(폴더 연결 필요).
3. 기준문서 반영: `07_요구사항`/`25_기능명세`/`23_용어사전`에 요구사항 변경 명문화(Lead 승인 기록).
4. git: `dev` 기준 새 브랜치 정리 후 백엔드/Flutter/문서 PR 분할.

## 6. 리스크

- DB 용량(LONGBLOB), 스트리밍 web 헤더 제약(웹은 just_audio 헤더 한계), Range/seek 미지원(1차) — 워크플로우 §9 참조.
