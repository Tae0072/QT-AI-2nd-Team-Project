# FE 정비(①~⑤) 작업 리포트

> **작성일:** 2026-06-06
> **브랜치:** `refactor/fe-i18n-setup` (총 15커밋)
> **PR:** 구현 [#310](https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/310) (base `dev`) · 문서 [#16](https://github.com/Tae0072/2nd-Team-Project/pull/16) (base `master`)
> **근거:** 강사 직강 피드백(2026-06-06) — FE 정비 5단계가 MSA(v2) 분리 선행. `27_프론트엔드_정비_로드맵.md` 참조.
> **검증 환경:** Flutter 3.44.1 / Dart 3.12.1 (Windows). 매 커밋 `flutter analyze` = **No issues found!**

---

## 1. 완료 요약

| 단계 | 내용 | 상태 | 주요 커밋 |
| --- | --- | --- | --- |
| ① 국제화 | 전 화면/위젯 문자열 외부화 + 다국어 인프라 | **완료** | 12커밋 (ca1bf89…0f8daca) |
| ② Riverpod | 데이터 조회를 화면 setState→Provider로 일원화 | **완료** | 6862e42, 435b919 |
| ③ 치수 변수화 | 치수 토큰(`app_dimens`) 단일 소스 도입 | **토대 완료**(점진 적용) | a42c903 |
| ④ 컴포넌트화 | 중복된 카테고리 라벨 `switch`를 공통 함수로 추출 | **대표 1건 완료** | a42c903 |
| ⑤ 데드코드 | 정적 분석 + 미사용 파일 점검 | **검증 완료**(제거 대상 0) | — |

---

## 2. ① 국제화 (완료)

- **인프라:** `flutter_localizations` + `intl`, `pubspec generate: true`, `l10n.yaml`, `lib/l10n/app_ko.arb`·`app_en.arb`(약 150키, 복수/숫자 placeholder 포함), 기본 로케일 한국어. (Flutter 3.44는 구 `flutter_gen` 합성 패키지 미사용 → import는 `package:qtai_app/l10n/app_localizations.dart`)
- **적용 범위:** 스플래시·로그인·닉네임·온보딩·홈 탭바·오늘 QT·성경 브라우저·노트(목록/카테고리/상세/작성/공유시트/서식툴바/달력)·나눔(피드/내나눔/상세)·마이페이지(대시보드/알림/찬양/설정/프로필편집/TTS설정)·공통 위젯 — **전부**.
- **검증:** 최종 화면/위젯의 한글 문자열 리터럴 **0개** 확인.

## 3. ② Riverpod 일관화 (완료)

- 화면이 직접 repository를 호출해 `setState`로 보관하던 **데이터 조회**를 Provider로 옮김:
  - `sharing_detail` → `sharingPostDetailProvider(postId)` (상세+댓글). 좋아요/댓글/삭제/신고는 repo 호출 후 `invalidate`로 새로고침.
  - `bible_browser` 성경 권 목록 → `bibleBooksProvider` (FutureBuilder 제거).
- **나머지 9개 `setState`는 유지가 정답**입니다(순수 로컬 UI 상태): 홈 탭 인덱스, 온보딩 페이지, 달력 선택일, 로그인·닉네임·노트작성·프로필편집의 제출/편집 플래그·컨트롤러, TTS 버튼, 공유 시트. 이를 Riverpod으로 옮기면 오히려 안티패턴.

## 4. ③ 치수 변수화 (토대)

- `lib/core/theme/app_dimens.dart` 신규 — `AppGap`(간격), `AppRadius`(반경), `AppPad`(패딩) 토큰. 하드코딩 수치의 단일 소스.
- 대표 적용: `sharing_detail`의 패딩을 `AppPad.all16`로 치환.
- **후속(권고):** 전 화면의 `EdgeInsets`/`SizedBox` 수치를 토큰으로 점진 치환. 한 번에 63개 파일을 바꾸면 PR이 과대해지고 충돌 위험이 커서, 화면을 손볼 때마다 점진 적용을 권장.

## 5. ④ 컴포넌트화 (대표 1건)

- `lib/core/constants/post_category.dart` 신규 — 카테고리 코드→한글 라벨 매핑. `my_sharing`·`sharing_detail`에 복사돼 있던 동일 `switch`를 제거하고 공통 함수로 통일.
- **후속(권고):** 반복되는 위젯(목록 타일, 카드 등) 추출은 런타임 확인이 필요해 #310 머지 후 화면 단위로 진행 권장.

## 6. ⑤ 데드코드 (검증 완료)

- `flutter analyze` 무경고(미사용 import/변수/요소·dead_code 없음). import 기준 **미사용 dart 파일 0개**.
- 제거할 명백한 데드코드 없음. (미사용 public API까지 보려면 `dart_code_metrics` 등 추가 린트가 필요 — 후속 선택)

---

## 7. 로컬 검증 체크리스트 (T 권장 — 런타임은 미실행)

> 정적 분석(analyze)은 전부 통과했으나, ②부터는 **런타임 동작 변경**이 있어 기기 스모크 테스트를 권장합니다.

```bash
cd flutter-app
flutter pub get
flutter gen-l10n
flutter analyze   # No issues found! 기대
```

- [ ] 로그인 → 온보딩 → 홈 진입
- [ ] 오늘 QT 로드 / 성경 브라우저 권 목록·조회
- [ ] 노트 작성·수정·삭제·공유
- [ ] **나눔 상세**: 로드, 좋아요/취소, 댓글 작성/삭제, 신고, 글 삭제 (← ② 변경 핵심)
- [ ] 마이: 설정·프로필·닉네임 변경·찬양·알림·탈퇴
- [ ] (선택) 기기 언어를 영어로 바꿔 텍스트 표시 확인

## 8. 의도적으로 남긴 것 (후속 정리 대상)

- 모델 카테고리 라벨맵(`note_models` 등)·repository/service 로그·예외 메시지·날짜 포맷(년/월/일·상대시간)의 다국어 — 사용자 화면 chrome이 아니라 후속.
- `app_dimens` 전면 적용, 추가 위젯 컴포넌트화 — #310 머지 후 점진.

## 9. 다음 단계

1. PR #310·#16 리뷰/머지 (#310이 i18n+Riverpod 일관화를 담은 FE 정비 PR).
2. 머지 후 ③ 전면 적용·④ 위젯 추출을 화면 단위 소규모 PR로.
3. 이후 MSA(v2) 분리 설계 착수 (로드맵 27 기준).
