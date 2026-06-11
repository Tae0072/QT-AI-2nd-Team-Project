# 2026-06-11 성경 본문 목차 화면 overflow 수정 + Calm Paper 리디자인 — 결과 보고

## 요약
성경(본문) 탭 목차 화면의 권 목록 `BOTTOM OVERFLOWED` 문제를 고정 높이 제거로 해소하고, 타사앱과 유사하던 하드코딩 디자인을 앱 공통 Calm Paper 테마로 통일했다. 기능 없는 헤더 닫기(X) 버튼도 제거. F-01 범위.

## 산출물
| 파일 | 설명 |
|------|------|
| `flutter-app/lib/features/bible/screens/bible_browser_screen.dart` | overflow 수정(minHeight 기반) + Calm Paper 토큰 전면 적용 + X 버튼 제거 + 결과 시트 정리 |
| `flutter-app/test/features/bible/screens/bible_browser_screen_test.dart` | 헤더 기대값 `성경 본문`으로 갱신(key·렌더 포맷 유지) |

## 검증
- `flutter analyze` 무이슈 / `flutter test`(해당 파일) 2건 통과
- overflow: `_BibleBookRow` 고정 `height:45` 및 숫자열 `itemExtent:34` → `minHeight`+패딩으로 교체해 구조적으로 제거

## 미해결 / 후속
- 실기기/큰 글자 배율 수동 스크린샷 보강 권장
- 저장소 untracked `data/bible-json/{KorRV,KJV}.json`은 §8 금지 데이터로 보임 — 본 작업과 무관해 미스테이징, 별도 정리 필요

담당: DevD 이승욱
