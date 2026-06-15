# 리포트 — 앱 마이페이지 미션 블록 분리 + 나의 찬양(F-09) 제거

- 일자: 2026-06-15
- PR: `QT-AI-2nd-Team-Project#677` (branch `feature/app-mypage-mission-block` → dev)
- 관련 워크플로우: `workflows/2026-06-15_app-mypage-mission-praise.md`
- 선행 통합: PR #674(관리자 찬양 큐레이션 → 배경음악 통합, dev 머지 완료)

## 한 줄 요약
마이페이지에서 연속/이번 주/이번 달 묵상 일수를 **독립 '미션' 블록**으로 분리하고, **'나의 찬양'(F-09 사용자 노출)을 제거**했다(앱 UI 한정, 서버 F-09 보존).

## 리뷰 반려(REQUEST_CHANGES) 대응
| 지적 | 대응 |
|---|---|
| ① F-09 찬양 전체 제거 요구사항/Lead 근거 부재 | 워크플로 문서에 **Lead(T) 2026-06-15 결정 근거 + 범위 한정(앱 UI만, 서버 보존) + 요구사항 문서 반영 후속** 명시 |
| ② feat 테스트 전무 + 사유 누락 | 위젯 테스트 `mypage_screen_test.dart` 신규(미션 블록 표시 / 나의 찬양 미노출 2케이스) |
| ③ 워크플로/리포트 링크 누락 | 본 워크플로·리포트 문서 작성 + PR 본문에 링크 |

## 변경 파일 (flutter-app)
- `lib/features/mypage/screens/mypage_screen.dart` — 미션 블록 분리 + 찬양 행 제거
- `lib/l10n/app_en.arb`·`app_ko.arb`·`app_localizations*.dart`(3) — missionTitle
- `lib/routes/app_router.dart`, `lib/features/dev/dev_mode_screen.dart` — praise 라우트 제거
- `lib/features/mypage/providers/mypage_providers.dart`, `services/mypage_repository.dart` — praise provider/메서드 제거
- 삭제: `lib/features/mypage/screens/praise_screen.dart`, `lib/features/mypage/models/praise_response.dart`
- 신규 테스트: `test/features/mypage/screens/mypage_screen_test.dart`

## F-09 결정 요약 (감사 추적용)
- 결정자/일자: Lead T / 2026-06-15
- 내용: 사용자 앱 '나의 찬양' 노출 제거. 음악(브금/찬송가) 일원화 방침의 연장.
- 범위: Flutter UI만. 서버 `/me/praise-songs` 등 F-09 엔드포인트·admin praise 백엔드 보존.
- 후속: 문서 저장소 07/F-09 반영(요구사항 변경) — Lead 검토 항목.

## 검증
- 샌드박스 Flutter 미설치 → 코드 대조 + 잔존 참조 0건 확인. 머지 전 `flutter pub get && flutter analyze && flutter test` 필수.
