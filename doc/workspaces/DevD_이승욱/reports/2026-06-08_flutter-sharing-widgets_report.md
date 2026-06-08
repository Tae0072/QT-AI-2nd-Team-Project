# 2026-06-08 나눔 상세 화면 위젯 컴포넌트화 — 결과 보고

## 요약
나눔 상세 화면(S-02)의 인라인 UI를 도메인 `widgets/`로 분리하는 순수 구조 리팩토링. 동작 변화 없이 `sharing_detail_screen`을 369→286줄로 축소했다. 본 PR은 코드 4파일을 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `sharing/widgets/sharing_snapshot_card.dart` | (신규) 작성자/카테고리/제목 스냅샷 카드 — `AppGap`/`AppRadius` 토큰 적용 |
| `sharing/widgets/sharing_comment_tile.dart` | (신규) 댓글 1건 타일 — `onDelete` null이면 삭제 버튼 미표시 |
| `sharing/widgets/sharing_comment_input.dart` | (신규) 댓글 입력 줄 — 입력값/전송중 상태는 상위 화면 소유 |
| `sharing_detail_screen.dart` | 인라인 위젯 → 공통 위젯 사용, 369→286줄 |

## 변경 성격
- 순수 구조 리팩토링 — UI/동작/사용자 노출 문자열 변경 없음 (위젯 추출 + 공개화)
- 카테고리 라벨은 기존 `core/constants/post_category` 그대로 사용
- 간격/반경은 dev에 도입된 `core/theme/app_dimens`(`AppGap`/`AppRadius`) 점진 적용

## 정책 확인
- 공유 스냅샷 정책 유지 (07 §F-10) — 카드가 스냅샷 필드만 표시, 원본 재조회 없음
- 사용자 노출 문자열은 기존 l10n(`app_ko`/`app_en`) 키 그대로 사용

## 검증
- `flutter analyze lib` — No issues found
- 나눔 전용 위젯 테스트는 현재 없음(기존에도 sharing 테스트 부재) → 동작 무변경 리팩토링으로 회귀 위험 낮음

## 미해결
- PR 머지 대기
