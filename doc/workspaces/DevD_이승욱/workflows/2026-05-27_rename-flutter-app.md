# 2026-05-27 Flutter 앱 디렉토리 이름 변경

## 목표
`qtai-app/` 디렉토리를 `flutter-app/`으로 이름 변경하고, CI Flutter SDK 호환성을 수정한다.

## 작업 내용
1. **디렉토리 이름 변경** — `qtai-app/` → `flutter-app/`
2. **CI Flutter SDK 호환 수정** — Flutter SDK 버전 고정 제거 → latest stable 사용
3. **Dart SDK 제약 하향** — `^3.11.5` → `^3.5.0`으로 하향 조정

## 범위
- 브랜치: `chore/rename-flutter-app`
- PR: #101
- 커밋 3건: 디렉토리 이름 변경, CI SDK 버전 수정, Dart SDK 제약 하향

## 미해결
- 없음

## 담당
- DevD 이승욱
