# 2026-06-15 관리자 배경음악 관리 기능 구현 리포트

대상 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-15_admin-background-music-management.md`

## 요약

- 기존 `music_tracks` 테이블과 `domain.music` 엔티티를 재사용해 관리자 배경음악 관리 API를 추가했다.
- 관리자 웹에 `배경음악 관리` 메뉴와 목록/등록/수정/노출/숨김 화면을 추가했다.
- 사용자 앱 신규 화면이나 재생 UX는 수정하지 않았다. 기존 사용자 조회는 `enabled=true`인 음원만 노출되는 흐름을 유지한다.

## 백엔드

- 추가 API
  - `GET /api/v1/admin/music-tracks`
  - `POST /api/v1/admin/music-tracks`
  - `PATCH /api/v1/admin/music-tracks/{id}`
  - `POST /api/v1/admin/music-tracks/{id}/publish`
  - `POST /api/v1/admin/music-tracks/{id}/hide`
- 등록/수정은 `multipart/form-data`를 사용한다.
  - POST는 `file` 필수
  - PATCH는 `file` 선택, 포함 시 음원 교체
- 등록은 `HIDDEN`으로 시작하고, `publish`/`hide`로 `ACTIVE`/`HIDDEN`을 전환한다.
- 관리자 목록은 projection을 사용해 `audio_data`를 읽지 않는다.
- 감사 로그는 `MUSIC_TRACK_*` action type으로 남기며 음원 바이트는 snapshot에 저장하지 않는다.

## 관리자 웹

- `admin-web/src/api/musicTracks.ts` 추가
- `/music-tracks` 라우트와 `배경음악 관리` 메뉴 추가
- `MusicTracksPage.tsx`에서 목록, 상태 필터, 등록/수정 모달, 노출/숨김 액션, toast를 구현했다.

## 문서

- `qtai-server/apis/api-v1/openapi.yaml`에 Admin Music 경로와 schema 추가
- `doc/프로젝트 문서/04_API_명세서.md`에 AD-12, §4.7.7 배경음악 관리, API 요약/enum/매핑/변경 이력 반영

## 검증

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-ai-review-reference.ps1 -SourceIndex "C:\Users\G\Downloads\reference-index (1).json"`
  - 통과. `restricted://validation/index/reference-index.json`, entries 3021 연결 확인.
- `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*Music*"`
  - RED: 신규 관리자 music 계약 타입 부재로 컴파일 실패 확인.
  - GREEN: 구현 후 통과.
- `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test --tests "*Music*"`
  - 통과. 기존 사용자 배경음악 조회/스트리밍 테스트 회귀 없음.
- `npm.cmd test` (`admin-web`)
  - RED: `MusicTracksPage.tsx` 부재로 계약 테스트 실패 확인.
  - GREEN: 구현 후 통과.
- `npm.cmd run typecheck` (`admin-web`)
  - 통과.
- `.\qtai-server\gradlew.bat -p qtai-server build`
  - 통과.
- `.\qtai-server\gradlew.bat -p qtai-server test jacocoTestReport`
  - 통과.
- `.\qtai-server\gradlew.bat -p qtai-server jacocoTestCoverageVerification`
  - 통과.
- `npx.cmd @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml`
  - 실행 불가. 저장소 루트에 `apis/`와 `.spectral.yaml`가 없고, 실제 OpenAPI 경로는 `qtai-server/apis/*/openapi.yaml`이다.
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`
  - 실행 불가. Spectral ruleset 파일이 저장소에 없다.
- `gitleaks detect --source . --redact --exit-code 1`
  - 실행 불가. 현재 로컬에 `gitleaks` 명령이 설치되어 있지 않다.
- `git diff --check`
  - 통과. 줄끝 변환 경고만 출력됨.
