# 2026-05-27 Flutter 앱 디렉토리 이름 변경 — 결과 보고

## 요약
`qtai-app/` → `flutter-app/` 디렉토리 이름 변경 및 CI Flutter SDK 호환성 수정. PR #101.

## 산출물

| 파일 | 설명 |
|------|------|
| `flutter-app/` | qtai-app에서 이름 변경 |
| `.github/workflows/qt-ai-ci.yml` | Flutter SDK 버전 고정 제거 → latest stable |
| `flutter-app/pubspec.yaml` | Dart SDK 제약 ^3.11.5 → ^3.5.0 하향 |

## 검증
- `flutter analyze` — 정상
- CI 빌드 호환성 확인

## 미해결
- 없음
