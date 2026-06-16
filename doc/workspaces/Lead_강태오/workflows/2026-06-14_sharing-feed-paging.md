# 2026-06-14 나눔 피드 페이징(10개씩 · 목차 5개)

## 요청
- 나눔 피드에 페이징을 넣는다. 한 페이지 **10개**씩, 목차 번호는 **5개씩** 보이게.

## 구현 (프론트 전용 — 서버는 이미 페이징 지원)
- 서버 `GET /api/v1/sharing-posts`는 이미 `page`/`size`와 `totalPages`를 반환한다. 백엔드 변경 없음.
- `SharingRepository.getSharingPosts`에 `size` 파라미터 추가(기본 **10**).
- 모델 `SharingPostListResponse`에 `page`·`totalPages` 추가(JSON에서 파싱, 로컬 생성 시 기본값 0/1).
  - 낙관적 갱신(좋아요·저장 토글)·저장 목록 제거 시 페이징 정보를 그대로 유지하도록 생성자 호출 보정(안 하면 토글 후 페이저가 1페이지로 어긋남).
- 상태: `sharingPageProvider`(StateProvider<int>) 추가. `SharingFeedNotifier.build`가 page를 watch해 해당 페이지를 서버에서 다시 불러온다.
  - 카테고리·검색어 변경 시 페이지를 0으로 리셋(피드 화면에서 처리).
- 위젯 `PageNavigator`: 한 블록 **5개** 번호(블록 단위) + 이전/다음 화살표. 전체 1페이지 이하면 숨김. 첫/마지막 페이지에서 화살표 비활성.
- 피드 화면: 목록 아래에 `PageNavigator` 배치. 로딩 중에도 직전 값(`valueOrNull`)을 써 깜빡임 방지.

## 검증
- `flutter analyze` 무이슈, `flutter test` 전체 **314개 통과**(신규 `PageNavigator` 위젯 테스트 5건 포함).
- 리포지토리 시그니처 변경(size 추가)에 맞춰 기존 페이크(피드/북마크/today_qt 테스트 3곳) 오버라이드 보정.

## Git/PR
- 브랜치 `feature/sharing-feed-paging` → PR 대상 `dev`. 커밋 `feat(sharing): 나눔 피드 페이징(10개/목차 5개)`.

## 후속
- 다음: 나눔 #태그(멘션) — 저장 버튼 옆 '#태그' 버튼으로 내가 태그된 글만 보기. 별도 PR.
