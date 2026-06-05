# 2026-06-05 · W3 Day2 Flutter 개념 정리 학습노트

> 오늘 구현: N-04(상세/수정/삭제), N-01 달력 탭, 외부공유, 서식툴바, 나눔 댓글/신고 보완
> 이 노트는 "그날 새로 쓴 Flutter 개념"만 정리. table_calendar는 별도 가이드 참조:
> [2026-06-05_table_calendar-초보자-완전가이드.md](2026-06-05_table_calendar-초보자-완전가이드.md)

---

## 1. FutureProvider.family + autoDispose — "인자별 데이터"

```dart
final noteDetailProvider =
    FutureProvider.autoDispose.family<NoteDetail, int>((ref, noteId) {
  return ref.watch(noteRepositoryProvider).getDetail(noteId);
});
// 사용: ref.watch(noteDetailProvider(42))
```
- **family** = "인자(id/월)마다 다른 provider를 찍어내는 틀"(붕어빵 틀). `(42)`와 `(99)`는 각각 따로 캐시.
- `<결과타입, 인자타입>` 순서. `autoDispose` = 화면 떠나면 캐시 정리 → 다시 들어오면 최신 재조회.
- 적용: `noteDetailProvider`(noteId), `meditationCalendarProvider`(monthKey).

## 2. ref.invalidate — "캐시 버리고 다시 받아"

```dart
await Navigator.pushNamed(...);     // 다른 화면에서 데이터 변경
ref.invalidate(noteDetailProvider(noteId)); // 돌아오면 캐시 폐기 → 재조회
ref.invalidate(notesProvider);
```
- **남의 화면(편집/삭제)에서 데이터가 바뀌면 내 화면 캐시는 낡는다(stale)** → invalidate로 강제 새로고침.
- 달력: 월 넘기면 `_monthKey`가 바뀌어 family 인자가 달라짐 → 자동 재조회(이건 invalidate 아님, "인자 변경"). 삭제 후엔 같은 달이라 invalidate 필요.

## 3. 폼 prefill — didChangeDependencies + 1회 가드

```dart
bool _initialized = false;
@override
void didChangeDependencies() {
  super.didChangeDependencies();
  if (_initialized) return;       // ★ build/didChange는 여러 번 불림
  _initialized = true;
  // 라우트 인자 읽고, 편집모드면 getDetail로 폼 1회 채움
}
```
- **`build`/`didChangeDependencies`는 여러 번 실행됨.** 컨트롤러를 매번 채우면 **사용자가 친 글자가 옛값으로 덮어써짐.**
- 그래서 가드로 "첫 1회만" 채우고, 그 뒤엔 `TextEditingController`(사용자 입력)가 주인.
- 라우트 인자는 `initState`보다 `didChangeDependencies`에서 안전하게 읽음.

## 4. 타입 있는 라우트 인자 (NoteEditArgs)

```dart
class NoteEditArgs { final String? category; final int? noteId; bool get isEdit => noteId != null; }
// 작성: NoteEditArgs(category: 'PRAYER')  /  수정: NoteEditArgs(noteId: 42)
```
- arguments를 String/int로 타입 구분하면 헷갈림 → **전용 클래스**로 "무엇을 넘기는지" 명확히.

## 5. RepaintBoundary → 위젯을 이미지로

```dart
final _cardKey = GlobalKey();
RepaintBoundary(key: _cardKey, child: 카드위젯),
// 캡처:
final boundary = _cardKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
final image = await boundary.toImage(pixelRatio: 3.0);
final bytes = (await image.toByteData(format: ui.ImageByteFormat.png))!.buffer.asUint8List();
```
- **RepaintBoundary로 감싸야 그 부분이 독립 레이어가 되어 `toImage()`로 픽셀을 떠낼 수 있다.**
- `pixelRatio` 높이면 고해상도. PNG bytes → 임시파일 → 공유.

## 6. share_plus 12.x (외부 공유)

```dart
await SharePlus.instance.share(ShareParams(text: '내용'));          // 텍스트
await SharePlus.instance.share(ShareParams(files: [XFile(path)])); // 파일/이미지
```
- 12.x는 `SharePlus.instance.share(ShareParams(...))` 형태(구버전 `Share.share`와 다름).
- 이미지는 실제 파일 경로가 안정적 → `path_provider`의 `getTemporaryDirectory()`에 저장 후 공유.

## 7. 마크다운 서식 툴바 (컨트롤러 조작)

- 백엔드 `body`가 평문이라 진짜 리치텍스트(색/크기) 불가 → **마커 삽입**으로 구조·강조만.
```dart
// 선택 감싸기(굵게): 선택 구간을 marker로 감싼 새 text + 커서 재설정
controller.value = TextEditingValue(text: newText, selection: TextSelection.collapsed(offset: cursor));
// 줄 머리(제목/목록): 현재 줄 시작 = text.lastIndexOf('\n', pos-1) + 1
```
- `controller.selection`으로 커서/선택 위치를 알고, `replaceRange`로 마커를 끼움.
- 색·크기·하이라이트는 마크다운 불가 → 회의 안건(리치텍스트 = body 저장포맷 변경, 팀 결정).

## 8. showModalBottomSheet (공유/신고 시트)

```dart
final result = await showModalBottomSheet<String>(
  context: context, builder: (_) => ...ListTile(onTap: ()=>Navigator.pop(context, '값'))...,
);
```
- 아래에서 올라오는 시트. `Navigator.pop(context, 값)`으로 **선택 결과를 await로 받음**(신고 사유 선택 등).

---

## 오늘의 한 줄 정리
- 데이터 조회는 **family provider**, 변경 후엔 **invalidate**.
- 폼은 **1회만 prefill**(가드), 그 뒤는 컨트롤러가 주인.
- 위젯→이미지는 **RepaintBoundary.toImage**, 공유는 **share_plus 12.x**.
- 계약(body=평문)을 깨는 기능(리치텍스트)은 **혼자 말고 팀 결정**.

## 명세 대조에서 배운 것
- N-04 [수정]을 `isFreeNote`(=묵상만 제외)로 열면 **설교노트 PATCH 시 verseIds 미전송 → 절 삭제**.
- → `writableNoteCategories`(기도/회개/감사)로만 좁힘. **"PATCH에 안 보낸 컬렉션은 비워질 수 있다"** 를 항상 의심.
