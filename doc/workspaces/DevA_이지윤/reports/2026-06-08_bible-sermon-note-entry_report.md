# 2026-06-08 성경탭 설교 노트 진입점 report

## 요약

`feature/20260604` 브랜치를 최신 `origin/dev` `1397f4e` 기준으로 병합하고, 성경탭 조회 결과에서 노트탭의 설교 분류로 이동하는 진입점을 추가했다.

노트 작성 화면은 별도 담당 범위이므로 수정하지 않았다. 이번 변경은 성경탭에서 `노트` 버튼을 제공하고, 선택된 성경 범위를 조회한 상태에서 노트탭 설교 목록으로 넘어가는 연결까지만 포함한다.

## dev 최신화

| 항목 | 내용 |
| --- | --- |
| 기준 dev | `origin/dev` `1397f4e` |
| 병합 방식 | `feature/20260604`에서 `origin/dev` merge |
| 충돌 파일 | `flutter-app/lib/features/bible/screens/bible_browser_screen.dart`, `flutter-app/test/features/bible/screens/bible_browser_screen_test.dart` |
| 충돌 처리 | dev의 성경 화면 구조를 유지하고 설교 노트 진입점만 재적용 |

## 구현 내용

| 파일 | 내용 |
| --- | --- |
| `flutter-app/lib/features/bible/screens/bible_browser_screen.dart` | 조회 결과 영역에 `노트` 버튼 추가, `SERMON` 필터 설정 후 노트 목록/노트 탭으로 이동 |
| `flutter-app/lib/features/home/screens/home_screen.dart` | 성경탭에서 전달받은 콜백으로 하단 노트 탭 전환 |
| `flutter-app/test/features/bible/screens/bible_browser_screen_test.dart` | 단독 라우트 fallback, 홈 탭 콜백, `SERMON` 필터 적용 검증 |

## 정책 및 범위 확인

- 노트 작성 화면/저장 플로우는 변경하지 않음.
- 성경탭에서 노트탭 설교 분류로 넘어가는 진입점만 구현.
- `SERMON` 필터 적용은 기존 `noteCategoryFilterProvider`를 사용.
- 백엔드 API, DB, OpenAPI 변경 없음.
- 금지 번역본 seed/test fixture 추가 없음.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `flutter test --no-pub test\features\bible\screens\bible_browser_screen_test.dart` | 통과 |
| `flutter analyze --no-pub` | 통과 |
| `flutter test --no-pub` | 통과 |
| `.\gradlew.bat build` (`qtai-server`) | 통과 |

## 남은 리스크 / 후속

- 노트 작성 화면에서 실제 설교 노트 작성 UX는 노트 화면 담당 작업에서 이어서 확정한다.
- dev 병합으로 들어온 타 담당자 변경은 해당 PR/CI에서 최종 확인한다.
- 현재 로컬의 `flutter-app/pubspec.lock`, `docker-compose.override.yml` 변경은 이번 작업 범위가 아니므로 커밋 대상에서 제외한다.
