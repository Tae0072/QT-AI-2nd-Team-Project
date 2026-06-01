# 2026-05-26 마이페이지 백엔드 API 구현

## 목표
마이페이지에 필요한 백엔드 API를 3개 도메인(member, notification, praise)에 걸쳐 구현한다. 대시보드 위젯별 부분 실패 허용, 닉네임 7일 잠금, 회원 탈퇴 PII 익명화, 찬양 큐레이션/디바이스곡 분리 등 비즈니스 정책을 반영한다.

## 작업 내용
1. **Member API 8개** — GET /me, GET /members/{id}, PATCH /me, PATCH /me/nickname(7일 잠금), GET /me/nickname/available, DELETE /me(PII 익명화), GET /me/dashboard(위젯별 부분 실패), GET /me/meditation-calendar(NOT_IMPLEMENTED)
2. **Notification API 3개** — GET /notifications(unreadOnly 필터, 페이징), PATCH /notifications/{id}/read, PATCH /notifications/read-all
3. **Praise API 5개** — GET /praise-songs(큐레이션), POST /admin/praise-songs(ADMIN), GET /me/praise-songs, POST /me/praise-songs(중복 검증), DELETE /me/praise-songs/{id}
4. **신규 Entity/Flyway** — Notification(V6), PraiseSong(V7), MemberPraiseSong(V8, deviceSongKey UK)
5. **Clock 빈 주입** — 서비스 레이어 `LocalDateTime.now(clock)` 패턴, @PrePersist 이중 사용 방지
6. **탈퇴 PII 익명화** — nickname→"탈퇴회원_{id}", kakaoId→`-getId()`, email/profileImage→null

## 범위
- 브랜치: `feature/mypage-api`
- 작업 기간: 2026-05-22 ~ 2026-05-26
- 변경 규모: 52 files, +2776 / -275
- PR Guard 리뷰: Round 5 완료 (가장 많은 수정이 발생한 Phase)
- 테스트: 7클래스 63건 전체 통과 (Service 39건 + Controller MockMvc 16건 + 기존 테스트)

## 미해결
- String→enum 전환 (PraiseSong.status/sourceType, Notification.type) — 리팩토링 시 전환 예정
- admin_role 세부 권한 — admin 도메인 구현 후 추가
- 대시보드 컨트롤러 책임 — notes 도메인 연동 시 DashboardUseCase 서비스 분리

## 담당
- DevD 이승욱
