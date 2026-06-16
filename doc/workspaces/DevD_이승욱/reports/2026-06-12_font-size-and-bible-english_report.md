# 2026-06-12 폰트 크기 적용 + 성경 본문 영어 영역 정합 — 결과 보고

## 요약
① 마이페이지 "폰트 크기" 설정이 화면에 안 먹던 버그를 고쳤다 — 값이 백엔드에 저장만 되고 적용 코드가 없었던 게 원인. 다크 모드와 같은 로컬 저장 패턴의 `fontSizeProvider`를 신설하고 `MaterialApp.builder`에서 `MediaQuery.textScaler`로 앱 전역에 적용. ② 성경 본문 보기의 영어 본문을 오늘의 QT와 동일한 `CpSubBox`(sunken sub-box)로 정합했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `core/theme/font_scale_provider.dart` (신규) | `fontSizeProvider`(로컬 저장) + `fontScaleOf` 매핑(0.9/1.0/1.2) |
| `main.dart` | 메인 MaterialApp `builder`에 textScaler 적용 |
| `features/mypage/screens/settings_screen.dart` | 드롭다운 로컬 즉시 반영 + 백엔드 저장 + 서버→로컬 동기화 |
| `features/bible/screens/bible_passage_screen.dart` | 영어 본문을 `CpSubBox`(QT _VerseTile과 동일 스타일)로 |
| `test/core/theme/font_scale_provider_test.dart` (신규) | 매핑·초기값·보정·저장값 6건 |
| `test/features/bible/screens/bible_passage_screen_test.dart` | 영어 토글 전 미노출 / 토글 시 CpSubBox 노출 2건 추가 |

## 검증
- `flutter analyze lib` + 테스트 파일 무이슈, 관련 테스트 11건 통과.
- 에뮬레이터(Android): 설정 폰트 크기 변경 시 본문 배율 반영, 성경 영어 토글 시 QT와 동일 sub-box 확인(T 확인 완료).

## 미해결 / 후속
- 성경 화면은 이지윤님 담당 → PR 리뷰 필요.
- 폰트 배율은 보수 범위(0.9~1.2). 더 큰 접근성 배율이 필요하면 매핑값만 조정 가능.
- 작업 폴더의 `data/bible-json/KJV.json`·`KorRV.json`(§8 금지 번역본) untracked 파일은 본 PR에서 제외. 별도 정리 권장.

담당: DevD 이승욱
