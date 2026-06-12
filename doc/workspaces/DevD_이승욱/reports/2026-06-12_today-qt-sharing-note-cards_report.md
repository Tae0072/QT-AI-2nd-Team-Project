# 2026-06-12 오늘 QT·나눔·기록 화면 프로토타입 정합 — 결과 보고

## 요약
DESIGN_PROTOTYPE.html에 맞춰 ① 오늘 QT 절의 영어를 sunken sub-box로 분리, ② 나눔 피드를 작성자·카테고리·좋아요/댓글 카드로, ③ 기록 목록을 카테고리 배지 카드로 바꿨다. 공통 컴포넌트(`calm_paper.dart`의 CpSubBox/CpBadge 등, #531에서 도입)를 재사용했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `features/bible/screens/today_qt_screen.dart` | 절: 좌측 번호 + 본문 + 영어 `CpSubBox` 분리 |
| `features/sharing/screens/sharing_feed_screen.dart` | 피드 `_PostCard`(작성자/배지/제목/미리보기/좋아요·댓글), 검색바·빈상태 토큰화 |
| `features/note/screens/note_list_screen.dart` | 목록 `_NoteCard`(제목+카테고리 배지+날짜 메타+나눔 배지) |

## 검증
- `flutter analyze lib` 무이슈 / `flutter test` 181건 통과
- 에뮬레이터 확인: QT 좌측번호+본문, 나눔 검색바·칩 정리, 기록 카드("dsfsd"+기도 배지+6월 10일)

## 미해결 / 후속
- 오늘 QT는 이지윤님 담당 → PR 리뷰 필요.
- 나눔 피드는 게시글이 없어 카드 자체 시각 확인은 빈 상태만 봄(코드·테스트로 검증).
- 절별 "해설"·피드 인용(snap)은 응답 데이터에 없어 미표시 — 백엔드 제공 시 추가 가능.
- **찬양 화면 JSON 파싱 오류(_Map is not List)** 기존 버그 발견 — 별도 처리 필요.

담당: DevD 이승욱
