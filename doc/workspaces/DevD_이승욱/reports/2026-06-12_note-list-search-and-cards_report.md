# 2026-06-12 기록 목록 검색 + 카드 재디자인 + bodyPreview — 결과 보고

## 요약
기록 탭 노트 목록을 참고 사진에 맞춰 재구성했다. 상단 검색바(서버 검색 연동), 카드는 좌측 카테고리 색 세로선 + 날짜·시각 스택 + 배지 2개(오전/오후 + 카테고리) + 제목 + 본문 2줄 미리보기. 미리보기를 위해 노트 목록 API에 `bodyPreview` 필드를 추가했다(note 도메인, T 담당).

## 산출물
| 파일 | 설명 |
|------|------|
| `qtai-server/.../note/api/dto/NoteListItem.java` | record에 `bodyPreview` 추가 |
| `qtai-server/.../note/internal/NoteService.java` | `buildBodyPreview`(body/섹션 발췌·80자 truncate) |
| `qtai-server/apis/api-v1/openapi.yaml` | NoteListItem 스키마에 `bodyPreview`(nullable) |
| `flutter-app/.../note/services/note_repository.dart` | getNotes에 `q` |
| `flutter-app/.../note/providers/note_providers.dart` | `noteSearchQueryProvider` + notesProvider q 연동 |
| `flutter-app/.../note/screens/note_list_screen.dart` | 검색바(Stateful), 달력 유지, 검색 빈 상태 |
| `flutter-app/.../note/widgets/note_card.dart` | 세로선·날짜시각·배지2·미리보기 재디자인 |
| `flutter-app/.../note/models/note_models.dart` | NoteListItem `bodyPreview` |
| `flutter-app/.../core/utils/date_format_utils.dart` | dateDot/clock/amPmKo helper |
| `flutter-app/lib/l10n/app_*.arb` | noteSearchHint/noteSearchEmpty |

## 검증
- `flutter analyze lib` 무이슈, 관련 테스트 21건 통과(date util·note_card·note_list_screen, q 연동 테스트 포함).
- 백엔드 `:service-note:compileJava` 성공, service-note docker 재빌드·healthy.
- 에뮬레이터에서 카드·검색바 확인(T 확인).

## 미해결 / 후속
- 좌측 세로선·배지 accent 색은 카테고리별 고정 톤. 디자인 토큰화는 후속 가능.
- bodyPreview는 admin-server 복사본 미반영(독립 컴파일이라 영향 없음). 필요 시 동기화.
- 참고 사진의 썸네일(이미지)은 노트에 이미지가 없어 세로선으로 대체(승인됨).

담당: DevD 이승욱
