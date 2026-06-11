# 2026-06-08 성경탭 설교 노트 진입점 workflow

## 작업 목적

성경탭에서 사용자가 조회한 본문 범위를 기준으로 노트 작성 흐름에 진입할 수 있는 버튼을 제공한다.

노트 작성 화면 자체는 김지민 담당 범위이므로, 이번 작업은 성경탭에서 노트탭의 설교 분류로 이동하는 진입점까지만 구현한다.

## 기준

- 작업 브랜치: `feature/20260604`
- 최신화 기준: `origin/dev` `1397f4e`
- 관련 화면: Flutter 성경탭, 홈 하단 탭, 노트 목록 설교 필터
- 관련 분류: `SERMON`

## 수행 범위

- `origin/dev` 최신 변경 병합
- 성경탭 조회 결과 영역에 `노트` 버튼 추가
- `노트` 버튼 클릭 시 `noteCategoryFilterProvider`를 `SERMON`으로 설정
- 홈 하단 탭 안에서 호출된 경우 노트 탭으로 전환
- 성경 화면 단독 라우트에서 호출된 경우 노트 목록 라우트로 이동
- 라우트 fallback과 홈 탭 콜백 경로에 대한 Flutter widget test 추가

## 제외 범위

- 노트 작성 화면 UI/저장 플로우 변경
- 설교 노트 작성 양식 및 상세 동작 변경
- 백엔드 API 변경
- 노트/나눔 도메인 정책 변경

## 협의 필요 여부

별도 협의 필요 없음.

이번 작업은 성경탭 진입점만 추가하며, 노트 작성 화면 구현 및 설교 노트 상세 작성 정책은 기존 담당 범위에 남긴다.

## 검증 계획

- `flutter test --no-pub test\features\bible\screens\bible_browser_screen_test.dart`
- `flutter analyze --no-pub`
- `flutter test --no-pub`
