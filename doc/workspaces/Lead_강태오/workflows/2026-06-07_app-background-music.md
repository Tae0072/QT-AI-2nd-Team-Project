# 2026-06-07 · 앱 전역 배경음악(브금·찬송가) 기능 — 워크플로우

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현)

## 1. 요약 (한 줄)

하단바 5탭과 모든 하위/상위 화면에서 배경음악(브금·찬송가)이 재생되도록 만들고, 오늘의 QT 화면 TTS 버튼 오른쪽에 음표 토글(켜기/끄기)을 추가하며, 마이페이지에 음악 설정 화면을 만든다. 음원은 **DB에 저장**해 서버가 스트리밍한다.

## 2. 요구사항 변경 결정 (가장 중요)

원래 기준 문서들은 **음원 파일의 서버/DB 저장을 금지**한다.

- `07_요구사항_정의서.md` §6.10 F-09: "가사와 음원 파일은 서버에 저장하지 않는다."
- `25_기능_명세서.md` §3 F-09: "가사·음원 저장 금지".
- `23_도메인_용어사전.md`: `domain.praise` — "가사·음원 저장 금지".
- `CLAUDE.md` §8 금지 기능: "찬양 가사, 음원 파일, 직접 YouTube URL 입력·저장" (임시 구현도 금지).
- 이 규칙의 근거는 **저작권 리스크**다.

**변경 결정 (2026-06-07, Lead T 승인):**

- 이번 기능은 기존 F-09(찬양 큐레이션, 참조만 저장)와 **별개의 신규 기능**(앱 전역 배경음악)이다.
- Lead 판단으로, **로열티프리 또는 직접 제작한 음원에 한해** 음원 바이트를 DB에 저장하는 것을 허용한다.
- 즉 "음원 서버/DB 저장 금지"의 예외를 신규 도메인 `music`에 한정해 연다. F-09(찬양 큐레이션)의 금지 규칙은 그대로 유지한다.
- 저작권 안전장치: `music_tracks.license_note`에 곡마다 출처/라이선스(로열티프리·직접제작)를 기록한다.
- 이 변경은 기준 문서(07/25/23)에도 반영 필요 → 문서 PR 별도 진행(작업 8단계).

> 참고: 이 결정은 Lead 권한 행사다. 추후 강사 직강/팀 합의에서 재검토될 수 있으며, 그 경우 `music` 도메인만 비활성화하면 롤백 가능하도록 경계를 좁게 설계했다.

## 3. 설계 — DB 스키마 (Flyway)

### V26 `music_tracks` (신규)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| title | VARCHAR(150) | 곡 제목 |
| category | VARCHAR(20) | `BGM` / `HYMN` |
| mime_type | VARCHAR(60) | 예: `audio/mpeg` |
| byte_size | BIGINT | 음원 크기 |
| duration_sec | INT NULL | 길이(초) |
| sort_order | INT | 재생/표시 순서 |
| enabled | BOOLEAN | 노출 여부(기본 TRUE) |
| license_note | VARCHAR(300) | 출처/라이선스 메모 |
| audio_data | LONGBLOB | **음원 바이트** |
| created_at/updated_at/deleted_at | DATETIME | BaseEntity 공통 |

설계 포인트: 목록 조회는 메타데이터만 SELECT(`audio_data` 제외)하고, 스트리밍 시에만 바이트를 읽는다 → 목록이 무거워지지 않는다.

### V27 `member_settings` 컬럼 추가

- `music_enabled BOOLEAN NOT NULL DEFAULT TRUE` (기본 ON)
- `music_volume INT NOT NULL DEFAULT 70` (0~100)
- `music_category VARCHAR(10) NOT NULL DEFAULT 'ALL'` (`ALL`/`BGM`/`HYMN`)

## 4. 설계 — API 계약

| 메서드 | 경로 | 설명 | 인가 |
|---|---|---|---|
| GET | `/api/v1/music/tracks` | 활성 음원 목록(메타데이터 + streamUrl) | 인증 |
| GET | `/api/v1/music/tracks/{id}/stream` | 음원 바이트 스트리밍 | 인증 |
| GET | `/api/v1/me/settings` | 음악 설정 포함 조회(기존 확장) | 인증 |
| PATCH | `/api/v1/me/settings` | 음악 on/off·볼륨·카테고리 수정 | 인증 |

모든 응답은 공통 `ApiResponse` envelope(스트리밍 바이트 응답 제외).

## 5. 설계 — 백엔드 도메인/패키지

신규 `com.qtai.domain.music` (기존 13개 + 1):

- `internal`: `MusicTrack`(entity, BaseEntity 상속), `MusicTrackRepository`, `MusicCategory`(enum), `MusicTrackSummary`/`MusicTrackAudioView`(projection), `MusicTrackService`
- `api`: `ListMusicTrackUseCase`, `GetMusicTrackAudioUseCase`, `dto/MusicTrackResponse`, `dto/MusicTrackAudioResponse`
- `web`: `MusicController`

음악 설정은 사용자 설정이므로 **member 도메인**(`MemberSettings`)에 둔다. member는 music 도메인을 import하지 않도록 `music_category`를 String으로 저장(도메인 경계 유지).

`DomainBoundaryArchTest`에 `"music"` 도메인을 등록한다(경계 테스트 커버).

## 6. 설계 — Flutter

- `pubspec.yaml`에 오디오 재생 패키지 추가(`just_audio` 등).
- 하단바(`home_screen.dart`) 위에 전역 배경음악 플레이어를 마운트 → 탭 이동/하위 화면에서도 재생 유지.
- 오늘의 QT(`today_qt_screen.dart`) TTS 버튼 오른쪽에 음표 토글 버튼(`QtTtsButton` 패턴 미러링).
- 마이페이지 `settings_screen.dart`에 "음악 설정" 진입점 + `music_settings_screen.dart` 신규(`tts_settings_screen.dart` 패턴).
- 상태관리: Riverpod `music_providers` + `music_repository`(서버 streamUrl 재생).
- 기본 ON: 첫 진입 시 `music_enabled=true`.

## 7. 작업 순서 (체크리스트)

1. [x] 설계 + 요구사항 변경 문서 (이 문서)
2. [ ] DB 마이그레이션 V26/V27
3. [ ] music 도메인 백엔드(entity/repo/service/usecase/dto/controller)
4. [ ] member 설정 확장 + 보안 확인
5. [ ] 백엔드 테스트 + 빌드 검증(Windows JDK21)
6. [ ] Flutter 전역 플레이어 + 음표 토글
7. [ ] Flutter 음악 설정 화면 + 연동
8. [ ] 음원 시딩(Downloads 폴더 연결) + 기준문서 반영 + 최종 리포트

## 8. 검증 계획

- `MusicTrackService` 단위테스트, `MusicController` slice 테스트, 설정 업데이트 테스트.
- ArchUnit 도메인 경계(`music` 포함) 통과.
- `./gradlew -p qtai-server build test` (Windows JDK21).
- 음원 목록 조회 시 `audio_data` 미로딩 확인(show-sql).

## 9. 리스크 / 주의

- **DB 용량**: 음원 LONGBLOB 저장은 DB가 커진다. 곡 수/용량이 커지면 추후 오브젝트 스토리지로 이전 검토.
- **스트리밍 인증**: 네이티브(안드로이드)는 just_audio가 JWT 헤더 전송 가능. Flutter 웹은 audio 엘리먼트 헤더 제약이 있어 별도 처리(서명 URL 등) 필요 — 1차는 안드로이드 기준.
- **Range/seek**: 1차 스트리밍은 전체 바이트 응답(seek 미지원). 배경음 루프엔 영향 적음, 추후 개선.
- **PR 분할**: 변경 파일이 10개를 넘으므로 백엔드/Flutter/문서로 PR을 나눈다(`09_Git_규칙`/CLAUDE.md §12).
