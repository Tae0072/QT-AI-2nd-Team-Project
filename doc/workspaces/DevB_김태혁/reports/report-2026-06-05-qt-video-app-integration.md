# Report — 2026-06-05 qt-video-app-integration

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | QT-AI-2nd-Team-Project `dev` |
| 작업 패널 | QT 본문 기반 영상 앱 연동 |

## 변경 내용

### 1. QT 영상 전용 백엔드 구조 추가
- 시뮬레이터와 분리된 QT 영상 테이블을 추가했다.
- `source_videos`: 원본 성경 영상 메타데이터 저장.
- `qt_passage_video_clips`: QT 본문별 승인 영상 URL, 구간, 상태 저장.
- `simulator_clips`나 simulator API에 연결하지 않았다.

### 2. QT 영상 API 추가
- `GET /api/v1/qt/{qtPassageId}/video`를 추가했다.
- 응답은 `READY` 또는 `MISSING` 상태로 내려간다.
- `READY` 응답에는 `videoUrl`, `sourceVideoId`, `bookCode`, `videoVersion`, `startMs`, `endMs`, `clipStatus`를 포함한다.
- 개발 환경에서는 `X-Dev-User-Id` 헤더로 인증 사용자를 주입해 테스트한다.

### 3. 책 전체 영상 기준 계산 서비스 추가
- QT 본문의 verse 목록을 조회한다.
- 같은 책 안에서 연속된 본문인지 확인한다.
- `source_videos.verse_duration_ms`와 `start_offset_ms` 기준으로 재생 구간을 계산한다.
- 테스트 기준은 절당 10초다.
- 현재 6월 17일 샘플 영상은 고린도전서 11:1-14 범위 테스트용으로 직접 clip seed를 넣어 검증했다.

### 4. Flutter 오늘 QT 화면 하단 영상 섹션 추가
- `video_player` 패키지를 추가했다.
- 오늘 QT 화면 맨 아래에 `QT 영상` 섹션을 추가했다.
- `GET /api/v1/qt/{qtPassageId}/video` 호출 결과가 `READY`면 mp4를 재생한다.
- `MISSING`이면 `QT 영상 준비중` 상태를 표시한다.
- `startMs/endMs` 구간 기준으로 진행바와 구간 시간을 표시한다.
- Android debug/profile 빌드에서 HTTP 로컬 영상 재생을 위해 cleartext traffic을 허용했다.

### 5. 로컬 영상 서버 및 에뮬레이터 검증
- 테스트 파일: `bible-engine/public/videos/corinthians11_v14_10sec_exact.mp4`
- PC URL: `http://localhost:8787/videos/corinthians11_v14_10sec_exact.mp4`
- Android emulator URL: `http://10.0.2.2:8787/videos/corinthians11_v14_10sec_exact.mp4`
- 영상 길이: 약 150.55초
- 해상도: 854x480
- Range 요청: `206 Partial Content` 확인

## 검증 결과
- `./gradlew.bat test --tests "*QtVideoServiceTest"`: PASS
- `./gradlew.bat clean test`: PASS
- `flutter analyze`: PASS
- `flutter test`: PASS
- `GET /api/v1/qt/4/video`: `READY` 응답 확인
- `GET /api/v1/qt/5/video`: `READY` 응답 확인
- Android 에뮬레이터에서 오늘 QT 하단 `QT 영상` 섹션 렌더링 확인
- Android 에뮬레이터에서 mp4 프레임, 재생 버튼, 진행바 표시 확인
- 스크린샷: `QT-AI-2nd-Team-Project/flutter-app/build/qt-video-screen.png`

## 확인한 금지선
- AI 영상 생성/검증 로직 추가 없음
- simulator API와 연결하지 않음
- `simulator_clips` 저장 구조 사용하지 않음
- YouTube 저장/재생 방식 사용하지 않음
- 운영 파일 저장소 확정 변경 없음
- 다른 팀원 담당 기능의 기존 로직은 필요한 연결 지점 외에는 건드리지 않음

## 남은 리스크
- H2 로컬 seed는 재시작하면 사라진다.
- 운영 적용 전에는 `source_videos` 66개 원본 영상 등록 방식이 필요하다.
- QT 본문 파싱/조회 담당 코드가 실제 배포 데이터와 맞는지 추가 확인이 필요하다.
- 1차 샘플은 고린도전서 11:1-14 영상이라, 1권 전체 영상 기준 자동 구간 계산은 실제 고린도전서 전체 영상이 준비된 뒤 재검증해야 한다.
- `GET /api/v1/qt/today/full`에 QT 영상까지 포함할지는 후속 결정 사항이다. 현재는 프론트가 별도로 `/qt/{qtPassageId}/video`를 호출한다.
