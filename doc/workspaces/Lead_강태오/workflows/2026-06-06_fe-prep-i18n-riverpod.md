# FE 정비(①~⑤) 작업 워크플로우

> 2026-06-06 · 브랜치 `refactor/fe-i18n-setup` · 리포트: `reports/2026-06-06_fe-prep-i18n-riverpod_report.md`

## 진행 절차

1. **방향 확정**: 강사 직강 피드백(FE 정비 5단계 → MSA 선행)을 새 지침으로 수용. 기존 v1=Modular Monolith 유지.
2. **문서화 선행**: 로드맵 `27_프론트엔드_정비_로드맵.md` 신규(문서·구현 저장소), `03`·`README`에 방향 노트. → 문서 PR #16, 구현 PR #310 개설.
3. **① 국제화**: 인프라(`l10n`) 구축 → `flutter pub get`/`gen-l10n`/`analyze`로 토대 검증 → 기능 단위(auth→note→home/tts→onboarding→bible→sharing→mypage)로 문자열을 ARB로 외부화. 단계마다 `analyze` 통과 후 커밋·푸시.
4. **② Riverpod**: `setState` 11개 분류 → 데이터 조회 2개(`sharing_detail`, `bible_browser`)만 Provider로 전환, 9개 로컬 UI 상태는 유지.
5. **③④⑤**: 치수 토큰(`app_dimens`)·카테고리 라벨 공통함수 도입(대표 적용), 데드코드는 정적분석으로 검증(제거 0).
6. **마감**: 본 리포트/워크플로우 작성.

## 검증 방식

- 코드 작성: Claude. 빌드·정적분석·커밋·푸시·PR: Windows(`D:\TOOL\flutter`, `D:\Git`, `gh`)에서 직접 실행.
- 게이트: 각 단계 `flutter analyze` = No issues found! (런타임 기기 테스트는 T가 수행 — 리포트 §7 체크리스트).

## 커밋(15)

docs(27/03) → feat(i18n) 토대 → auth → note(목록/카테고리) → note(상세/작성) → note(위젯) → home/tts → onboarding/bible → sharing → mypage(대시보드/알림/공통) → mypage(설정/찬양/프로필/TTS) → sharing 칩 → refactor(riverpod: 나눔상세) → refactor(riverpod: 성경권목록) → refactor(치수토큰/카테고리 공통함수).

## 주의(gotcha)

- 마운트로 생성한 폴더는 Windows에서 ReadOnly 속성 → `gen-l10n` 실패. 속성 해제 후 재생성.
- Flutter 3.44는 `flutter_gen` 합성 패키지 미사용 → import `package:qtai_app/l10n/app_localizations.dart`.
- 구현 저장소는 리눅스 샌드박스 git 불가 → git/gh는 Windows PowerShell로.
