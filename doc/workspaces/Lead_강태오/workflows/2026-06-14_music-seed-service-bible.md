# 2026-06-14 배경음악 음원 0개 원인 + 시드 수정

## 원인(진단)
- 사용자 앱의 음악 API(`GET /api/v1/music/tracks`)는 **service-bible**가 서빙한다(`MusicController`는 service-bible에만 존재).
- 음원 시드는 `MusicSeedRunner`가 클래스패스의 `seed/music/{bgm,hymn}/*.mp3`를 읽어 `music_tracks`에 적재한다.
- 그런데 **service-bible의 `src/main/resources/seed/music/`에는 mp3가 0개**였다(admin-server 쪽에만 실제 음원 32곡이 존재). → service-bible 기동 시 적재할 음원이 없어 0곡, 목록이 비었다.
- 조회 경로(`MusicTrackService.listEnabled` / `MusicTrackRepository.findByEnabledTrue...` / `MusicController`)는 정상. 즉 **데이터 누락**이 원인이며 코드 수정은 불필요.

## 조치
- admin-server에 이미 있던 실제 음원(BGM 19곡 + HYMN 13곡, 한글/영문 제목)을 **service-bible의 동일 시드 경로로 복사**했다. 이제 service-bible가 기동 시 스스로 32곡을 시드한다.
- mp3는 `.gitattributes`상 binary로 처리되어 줄바꿈 변환 영향 없음.
- 코드/스키마 변경 없음(시드 데이터만 추가). 멱등 시드 러너라 `music_tracks`가 비었을 때만 적재.

## 검증
- 로컬/배포에서 service-bible 기동 후 `GET /api/v1/music/tracks`가 32곡을 반환하는지 확인 필요(데이터 시드라 단위테스트 대상 아님).
- service-bible의 기존 `MusicTrackServiceTest`는 그대로 통과(로직 무변경).

## Git/PR
- 브랜치 `feature/music-seed-service-bible` → PR 대상 `dev`. service-bible/src/main/resources/seed/music/ 의 mp3만 추가.
