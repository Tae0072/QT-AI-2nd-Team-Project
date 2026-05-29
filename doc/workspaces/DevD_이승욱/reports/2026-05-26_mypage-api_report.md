# 2026-05-26 마이페이지 백엔드 API 구현 — 결과 보고

## 요약
마이페이지 백엔드 API를 member(8개), notification(3개), praise(5개) 3개 도메인에 걸쳐 구현했다. 대시보드 위젯별 부분 실패 허용, 닉네임 7일 잠금, 회원 탈퇴 PII 익명화, 찬양 deviceSongKey UK 등 비즈니스 정책 반영.
PR Guard Round 5 완료 — 가장 많은 수정이 발생한 Phase. Clock 빈 주입, @PrePersist 제거, eventKey 멱등성 등 핵심 패턴 전환 포함.

## 산출물

| 파일 | 설명 |
|------|------|
| `MemberController.java` | GET/PATCH /me, DELETE /me, GET /members/{id}, GET /me/nickname/available |
| `MyPageController.java` | GET /me/dashboard(위젯별 부분 실패), GET /me/meditation-calendar(NOT_IMPLEMENTED) |
| `NotificationController.java` | GET /notifications, PATCH /notifications/{id}/read, PATCH /notifications/read-all |
| `PraiseController.java` | GET /praise-songs, POST /admin/praise-songs, GET/POST/DELETE /me/praise-songs |
| `MemberService.java` | 프로필 수정, 닉네임 7일 잠금, 탈퇴 PII 익명화 |
| `NotificationService.java` | send(eventKey 멱등), countUnread, markAsRead, markAllAsRead(@Param readAt) |
| `PraiseService.java` | 큐레이션/디바이스곡 분리, deviceSongKey UK 중복 검증 |
| `V6~V8` | notifications, praise_songs, member_praise_songs DDL |

## 검증
- `gradlew compileJava test` — BUILD SUCCESSFUL, 148건 전체 통과
- 테스트: MemberServiceTest 16, NotificationServiceTest 11, PraiseServiceTest 12, Controller MockMvc 16건
- 도메인 경계 위반 — 없음 (cross-domain은 Long FK만 사용)
- 금지 기술/데이터 — 위반 없음

## 미해결
- String→enum 전환 (status/sourceType/type) — 리팩토링 시
- admin_role 세부 권한 — admin 도메인 구현 후
- 대시보드 DashboardUseCase 분리 — notes 도메인 연동 시
