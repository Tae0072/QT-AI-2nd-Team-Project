# 2026-06-12 나눔 피드 카드 디자인 개편 (feature/sharing-feed-redesign)

## 목표·배경
T 요청: 나눔 피드 화면을 시안(흰 배경 카드) 사진처럼. 기존 dev #543 Calm Paper 피드 카드 위에 ① 흰 배경 + 회색 필 검색바 + 검정 선택 칩, ② 카테고리 색상 배지, ③ 본문 범위 인용 박스를 얹는다.

## 작업 내용
### ① 피드 화면 (`sharing_feed_screen.dart`)
- Scaffold/AppBar 배경 흰색(`_bg`). 검색바를 회색 필(`_fieldBg`) 라운드 + 포커스 시 잉크 보더로. 빈 상태·구분선은 화면 로컬 토큰(`_muted`/`_divider`).
- 카테고리 칩을 `FilterChip` → 커스텀 칩(선택=검정 채움+체크+흰 텍스트, 비선택=흰 배경+회색 테두리). `Center`로 감싸 고정 높이에서 세로 늘어남 방지.
- dev #554 좋아요 낙관적 업데이트(`onLike`: `toggleLike` + 실패 시 `sharingLikeFailed`/`sharingUnlikeFailed` 스낵바) 그대로 보존.

### ② 카드 (`post_card.dart`)
- `CpBadge` → 색상 캡슐 `_CategoryBadge`(카테고리별 파스텔: 묵상 보라 / 설교 파랑 / 기도 초록 / 감사 황 / 회개 로즈). 묵상은 시안과 동일한 보라.
- 본문 범위 **인용 박스** 추가(`verseLabel` 있을 때만): 좌측 카테고리색 보더 + 인용 아이콘 + 범위 라벨.
- `onLike` 분리 `InkWell` 하트(카드 탭=상세 이동과 충돌 방지) + 좋아요/댓글 수 유지.

### ③ 모델 (`sharing_post_response.dart`)
- `SharingPostItem`에 `verseLabel`(JSON `verseSnapshot.rangeLabel`) 필드·파싱 추가, `copyWith`에도 보존(좋아요 낙관적 업데이트 시 누락 방지).

## 데이터 메모
- 피드 API는 본문 범위 **라벨**(`verseSnapshot.rangeLabel`, 예 "고전 6:7")만 제공 → 인용 박스는 라벨만 표시. 구절 본문 텍스트는 응답에 없어 미표시(백엔드 DTO 확장 시 추가 가능).

## 범위/주의
- 흰 배경은 시안(T 요청) 기준으로, 앱 전역 Calm Paper(베이지) 톤과 **의도적으로 다름** — 테마 일관성은 리뷰 판단 필요(필요 시 베이지 유지로 변경 가능).
- 나눔(sharing)은 본인(DevD) 담당.
- 나눔 공개 실패(`C0006`, service-note cross-service base-url)는 **dev에 이미 수정 반영됨**(docker-compose `QTAI_SERVICES_USER_BASE_URL`) — 본 PR 범위 아님. 로컬이 dev보다 stale이라 겪었던 이슈로, 트러블슈팅 문서로 별도 기록.

## 검증
- `flutter analyze lib/features/sharing` 무이슈.
- 에뮬레이터(Android API 37, `--dart-define-from-file=.env` 빌드): 나눔 탭 흰 배경 · 회색 검색바 · 검정 선택칩("전체") · 카테고리 색 배지(기도=초록) 확인. 인용 박스는 범위 있는 글에서 표시(현재 테스트 글은 범위 없어 미표시).

담당: DevD 이승욱
