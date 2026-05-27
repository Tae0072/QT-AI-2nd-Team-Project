# 2026-05-27 note-domain-completion BLOCK 수정 리포트

## 수정 대상
- PR 자동 리뷰 BLOCK: 폐기된 `qtai-app/` 경로가 PR diff에 포함됨
- PR 자동 리뷰 BLOCK: Android 로컬 SDK 경로 파일이 커밋 대상에 포함됨

## 원인
- Flutter 앱 경로가 `qtai-app`에서 `flutter-app`으로 변경되었는데, 이전 경로의 Flutter/Android 생성 파일과 로컬 설정 파일이 브랜치에 추가되어 있었다.
- `local.properties`는 개발자 개인 SDK 경로를 담는 로컬 파일이라 저장소에 포함되면 안 된다.

## 수정 내용
- `qtai-app/` 하위 추적 파일을 PR diff에서 제거했다.
- 폐기 경로 재유입 방지를 위해 루트 `.gitignore`에 `qtai-app/`를 추가했다.
- Android 로컬 설정 재유입 방지를 위해 루트 `.gitignore`에 `**/local.properties`를 추가했다.
- 로컬 작업공간에 남아 있던 `qtai-app/` 캐시 잔여물도 삭제했다.

## 인접 경로 점검
- `git ls-files "qtai-app/**"` 기준으로 폐기 경로 추적 파일 제거 여부 확인 대상에 포함했다.
- `git ls-files "*local.properties"` 기준으로 로컬 설정 파일 추적 여부 확인 대상에 포함했다.
- `flutter-app/`는 현재 유효 경로로 남기고, 이번 BLOCK 수정 범위에서는 파일 이동이나 생성 파일 보정은 하지 않았다.

## 검증 결과
- `git diff --check`: 통과
- `git ls-files "*local.properties"`: 추적 파일 없음
- `Test-Path qtai-app`: `False`
- `.\qtai-server\gradlew.bat -p qtai-server build`: 통과
- 서버 운영 코드 변경 없음. 이번 수정은 PR diff 정리와 ignore 보강이므로 Flutter analyze/test와 OpenAPI/Docker Compose 검증은 실행 대상에서 제외했다.

## 남은 WARN/INFO
- `GeneratedPluginRegistrant` 관련 WARN은 폐기된 `qtai-app/` 경로 제거로 함께 해소되는 범위다.
- PR 제목과 서버 코드 포함 여부 WARN/INFO는 이번 BLOCK 수정 범위 밖으로 두었다.
