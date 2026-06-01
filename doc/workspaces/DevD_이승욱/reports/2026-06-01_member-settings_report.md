# 2026-06-01 사용자 설정 API 구현 — 결과 보고

## 요약
마이페이지 설정 화면용 `GET/PATCH /api/v1/me/settings` API 구현. 알림 수신 ON/OFF + 폰트 크기(SMALL/MEDIUM/LARGE) 2개 항목. 첫 조회 시 기본값 자동 생성(Lazy init).

## 산출물

| 파일 | 설명 |
|------|------|
| `V20__create_member_settings.sql` | member_settings DDL |
| `MemberSettings.java` | Entity (member_id UK, notification_enabled, font_size) |
| `FontSize.java` | enum (SMALL, MEDIUM, LARGE) |
| `MemberSettingsRepository.java` | findByMemberId |
| `MemberSettingsService.java` | 조회(기본값 자동 생성) + 수정(partial update) |
| `GetSettingsUseCase.java` | 조회 UseCase |
| `UpdateSettingsUseCase.java` | 수정 UseCase |
| `SettingsResponse.java` | 응답 DTO |
| `SettingsUpdateRequest.java` | 요청 DTO (null=미변경) |
| `MemberSettingsController.java` | GET/PATCH /api/v1/me/settings |
| `MemberSettingsServiceTest.java` | 7건 테스트 (조회, 기본값, 알림/폰트/둘 다, 잘못된 fontSize, 소문자) |
| `MemberSettingsControllerTest.java` | 3건 테스트 (GET 조회, PATCH 수정, 잘못된 fontSize 400) |

## 검증
- `gradlew build` — BUILD SUCCESSFUL
- `MemberSettingsServiceTest` — 7건 통과
- `MemberSettingsControllerTest` — 3건 통과
- PR Guard — 14파일/532줄, 금지 항목 없음

## 리뷰 BLOCK 대응
- FontSize.valueOf 예외 미처리 → `parseFontSize()` + `@Pattern` + `@Valid`
- Controller 테스트 누락 → `MemberSettingsControllerTest` 3건 추가
- `@AuthenticationPrincipal` 바인딩 → 기존 패턴 동일 적용, 테스트 검증

## 미해결
- PR 머지 대기
