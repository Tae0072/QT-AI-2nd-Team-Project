# 2026-06-01 사용자 설정 API 구현 (F-13)

## 목표
마이페이지 설정 화면에서 사용할 사용자 설정 API(알림 수신, 폰트 크기)를 구현한다.

## 작업 내용
1. **V20 DDL** — `member_settings` 테이블 (member_id UK, notification_enabled, font_size)
2. **MemberSettings Entity + FontSize enum** — SMALL/MEDIUM/LARGE
3. **MemberSettingsRepository** — `findByMemberId`
4. **MemberSettingsService** — 첫 조회 시 기본값 자동 생성 (notification=true, fontSize=MEDIUM), PATCH partial update
5. **UseCase + DTO** — GetSettingsUseCase, UpdateSettingsUseCase, SettingsResponse, SettingsUpdateRequest
6. **MemberSettingsController** — `GET/PATCH /api/v1/me/settings`, `@Valid` 검증
7. **MemberSettingsServiceTest** — 7건 (조회, 기본값 생성, 알림만/폰트만/둘 다 변경, 잘못된 fontSize 예외, 소문자 처리)
8. **MemberSettingsControllerTest** — 3건 (GET 조회, PATCH 수정, 잘못된 fontSize 400)

## 리뷰 BLOCK 대응
- FontSize.valueOf 예외 → `parseFontSize()` + DTO `@Pattern` + Controller `@Valid`
- Controller 테스트 누락 → `MemberSettingsControllerTest` 3건 추가
- `@AuthenticationPrincipal` 바인딩 → 기존 프로젝트 패턴 동일 적용, 테스트 검증

## 범위
- 브랜치: `feature/member-settings`
- PR: (Open, 리뷰 대기)
- 커밋: `feat(member): 사용자 설정 API 구현 — GET/PATCH /api/v1/me/settings`
- 변경: 14파일 532줄
- 관련: F-13 마이페이지

## 미해결
- PR 머지 대기
- Flutter 설정 화면(M-06)은 후속 PR

## 담당
- Lead 강태오 (T)
