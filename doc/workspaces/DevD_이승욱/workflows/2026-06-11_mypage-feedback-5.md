# 2026-06-11 마이페이지 피드백 5건 처리 (bugfix/member-mypage-feedback)

## 목표·배경
2026-06-10 팀원 마이페이지 사용 피드백 5건 처리. 핵심은 기록탭 작성 내용이 '나의 묵상' 통계에 반영되지 않는 버그(F-13) — 원인은 service-user 대시보드의 노트 통계가 MSA 통합 전 스텁/Mock으로 남아 있던 것.

## 범위
- 브랜치: `bugfix/member-mypage-feedback` (dev 분기, 09_Git_규칙 §1.2 컨벤션 — 최초 `fix/flutter-*`에서 정정), PR 대상 dev
- 변경: service-user 11파일 + flutter-app 4파일 + 문서 2파일 (한 PR로 통합 — 2026-06-11 T 결정)

## 처리 내역

| # | 피드백 | 처리 |
|---|---|---|
| 1 | 기록탭 작성 내용이 '나의 묵상'에 미반영 | **버그 수정** — service-user `MyPageController.loadStats`가 "notes 미구현" 스텁으로 항상 0 반환. `GetMeditationCalendarUseCaseMock`을 service-note 묵상 달력 호출 RestClient 어댑터로 교체(§4)하고 `MeditationStatsCalculator`로 주간/월간/연속 환산 |
| 2 | 이번주/이번달/연속 로직 설명 요청 | **로직 확정·문서화** — SAVED 노트(임시저장 제외) 1건 이상인 날 = 묵상 1일(카테고리 무관). `savedAt` 기준 **저장 즉시 반영**(익일 아님). 연속은 오늘 미저장 시 어제까지 인정(P1-9). 상세 진입 화면은 미구현(협의 항목) |
| 3 | 알림 더미데이터(발표용) | `DevNotificationSeedRunner` — dev 프로파일+dev-bypass 한정, 미읽음 2+읽음 3건, eventKey 멱등, 운영 미영향 |
| 4 | "미읽음만" 문구 | "안 읽은 알림만"으로 변경 (l10n ko) |
| 5 | 닉네임 변경 잠금 제거 | 7일 잠금 폐지, 즉시 변경 허용. ⚠️ 2026-05-19 팀 결정 변경 — **Lead 확인 필요**. `nicknameChangedAt` 기록은 온보딩 판별 의존으로 유지 |

## 검증

- `:service-user:test` 전체 통과 (신규: 계산기 4, 어댑터 3, 시드 2, 닉네임 연속변경 1 — ArchUnit web→internal 경계 준수로 계산기는 `member.web`에 배치)
- `flutter analyze` 무이슈

## 리뷰 포인트

- 통계 주간 기준: ISO 주(월요일 시작), 주가 이전 달에 걸치면 이전 달 달력 1회 추가 조회
- 어댑터 인증: 사용자 요청 맥락이라 `ServiceCallAuthForwarder`로 JWT 전달(시스템 토큰 아님) — 배치 경로 사용 불가를 javadoc에 명시
- 5번은 제품 결정 변경이므로 PR에서 Lead 승인 필수

## 미해결 / 후속
- 통계 상세 진입 화면(피드백 2 후반) 미구현 — 작업량 협의
- 닉네임 잠금 폐지 SSoT(07 v3.3 F-04/F-10) 갱신은 Lead 승인 후 docs PR

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
