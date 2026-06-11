# 2026-06-11 마이페이지 피드백 5건 처리 — 결과 보고

## 요약
2026-06-10 팀원 마이페이지 사용 피드백 5건을 한 브랜치(`fix/flutter-mypage-feedback`)로 처리했다. 핵심은 '나의 묵상' 통계가 항상 0이던 버그 수정 — service-user의 노트 Mock 어댑터를 service-note 묵상 달력 실호출로 교체하고 주간/월간/연속 환산기를 추가했다(F-13). 부수로 발표용 dev 알림 시드, 알림 필터 문구 개선, 닉네임 7일 잠금 폐지(⚠️ 팀 결정 변경, Lead 확인 필요)를 포함한다.

## 산출물
| 파일 | 설명 |
|------|------|
| `service-user/.../mission/client/note/GetMeditationCalendarRestClientAdapter.java` | Mock 대체 — service-note 묵상 달력 HTTP 어댑터(사용자 JWT 전달) |
| `service-user/.../mission/client/note/GetMeditationCalendarUseCaseMock.java` | 삭제(§4: 실구현 등록 시 Mock 제거) |
| `service-user/.../member/web/MeditationStatsCalculator.java` | 주간(월~오늘)/월간/연속 환산 — web 패키지(ArchUnit web→internal 금지 준수) |
| `service-user/.../member/web/MyPageController.java` | loadStats 스텁 → 실집계 연동, 부분 실패 시 `widgetErrors: stats` |
| `service-user/.../notification/internal/DevNotificationSeedRunner.java` | dev+dev-bypass 한정 알림 더미 5건(미읽음 2+읽음 3, eventKey 멱등) |
| `service-user/.../member/{internal,api,web}` 닉네임 관련 4파일 | 7일 잠금 폐지 — 즉시 변경. `nicknameChangedAt` 기록은 온보딩 판별용으로 유지 |
| `flutter-app/lib/l10n/app_ko.arb` 외 1 | "미읽음만" → "안 읽은 알림만" |
| `flutter-app/.../stats_card.dart`, `profile_edit_screen.dart` | 낡은 주석 정정(미구현 표기 제거, 잠금 폐지 반영) |
| 테스트 4파일 | 계산기 4 + 어댑터 3 + 시드 2 + 닉네임 연속변경 1 |

## 변경 성격
- **버그 수정(F-13)**: 통계 위젯 0 고정 해소. 집계 의미 확정 — SAVED 노트 1건 이상인 날 = 묵상 1일(카테고리 무관, DRAFT 제외), savedAt 기준 저장 즉시 반영, streak은 오늘 미저장 시 어제까지 인정(P1-9 동일).
- **시연 보조**: 알림 더미는 운영 경로에 영향 없음(@Profile("dev") + dev-bypass 게이트, DevMemberSeedRunner와 동일 패턴).
- **정책 변경(F-04/F-10)**: 닉네임 7일 잠금 폐지는 요구사항 정의서 v3.3 확정 사항의 변경 — PR에서 Lead 승인 필수로 표시.

## 검증
- `:service-user:test` 전체 통과(ArchUnit 도메인 경계 포함), `flutter analyze` 무이슈.
- 수동: dev 프로파일 기동 시 알림 시드 멱등 동작 확인 예정(시연 리허설에서).

## 미해결 / 후속
- 이번주/이번달/연속 상세 진입 화면(피드백 2 후반)은 미구현 — 작업량 협의 항목.
- 닉네임 잠금 폐지의 SSoT(07 v3.3 §F-04/F-10, 06 화면 문구) 갱신은 Lead 승인 후 별도 docs PR.

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
