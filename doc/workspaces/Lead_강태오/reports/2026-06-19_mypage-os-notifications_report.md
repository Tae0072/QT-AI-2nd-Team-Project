# 리포트 — 마이페이지 알림 기기(OS) 로컬 알림 안정화 (2026-06-19)

- 작성자: Lead 강태오
- 브랜치: `feature/mypage-os-notifications` → `dev`
- 관련 F-ID: F-05
- 워크플로우: `workflows/2026-06-19_mypage-os-notifications.md`

## 결과 요약

| 항목 | 결과 | 비고 |
|---|---|---|
| 증상 | "새 알림 일부만 가끔 뜸"(Android) | 폴러의 메모리 기준선·요약 묶음이 원인 |
| 마지막 본 id 영속화 | ✅ SharedPreferences `notif_last_seen_id` | 앱 재시작에도 누락 없음 |
| 최초 1회만 기준선 | ✅ | 과거 알림 폭주 방지 |
| 개별 배너 + 상한 | ✅ 5건 초과 시 요약 | 각 알림이 개별로 울림 |
| 판단 로직 분리·테스트 | ✅ 순수 함수 + 단위테스트 5건 | analyze/test GREEN |
| 권한/플랫폼 | 확인 — Android 기존 정상 | POST_NOTIFICATIONS + 디슈가링 + 런타임 권한 |
| F-05 문서 정합 | ✅ Lead 결정 반영 | 로컬 알림 제공 / FCM 푸시 v1 제외 |

## 변경 파일

- `lib/core/notifications/notification_poller.dart` (재작성)
- `test/core/notifications/notification_poller_logic_test.dart` (신규)
- `doc/프로젝트 문서/07_요구사항_정의서.md` (F-05 문구 정합)

> 제약 준수: `theme_providers.dart` 등 테마/설정 파일 미수정(작업1 충돌 방지). `local_notification_service.dart`도 무변경(이미 동작).

## 검증 로그

```
flutter analyze (변경 2파일)        → No issues found!
flutter test (poller 순수 로직 5)   → All tests passed!
```

## 남은 한계 / 후속

1. 앱 완전 종료 중 실시간 수신은 FCM 필요 → MVP 범위 밖(별도 결정).
2. 문서 저장소(`2nd-Team-Project`) 07 요구사항 SSoT 동기화(후속 docs PR).
3. (선택) 폴러 통합 동작 위젯테스트는 싱글톤/타이머 의존이 커 순수 로직 테스트로 갈음.
