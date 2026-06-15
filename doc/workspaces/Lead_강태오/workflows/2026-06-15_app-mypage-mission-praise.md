# 2026-06-15 앱 마이페이지: 미션 블록 분리 + 나의 찬양(F-09) 제거

연계 PR: `QT-AI-2nd-Team-Project#677` (branch `feature/app-mypage-mission-block` → dev)

## 요청 (Lead T, 2026-06-15 구두 결정)
- 미션(연속/이번 주/이번 달 묵상 일수)이 마이페이지에서 닉네임 부제에 같이 들어가 있는데, **별도 미션 블록**으로 분리.
- **'나의 찬양'은 필요 없으니 제거**.

## F-09 제거 결정 근거 (리뷰 BLOCK ① 대응)
- **Lead(T) 결정(2026-06-15)**: 사용자 앱의 '나의 찬양'(F-09 큐레이션 저장/노출)을 제거한다.
- **배경/일관성**: 같은 날 관리자 '찬양 큐레이션(AD-05)'을 '배경음악 관리(AD-12, 브금/찬송가)'로 **통합·제거**(PR #674, dev 머지 완료)했다. 그 방침의 연장으로, 사용자 앱에서도 찬양 메뉴를 없애고 음악(브금/찬송가)로 일원화한다. CLAUDE.md §8의 "F-09 음원·가사·URL 미저장" 정책과 별개로, **'나의 찬양 큐레이션' 사용자 기능 자체를 축소**하는 제품 결정이다.
- **범위 한정(중요)**: 본 PR은 **사용자 앱(Flutter) UI 제거**만 한다. 서버 F-09 엔드포인트(`/me/praise-songs`, `/praise-songs` 등)와 admin praise 백엔드 도메인은 **보존**한다(회귀/복원 여지). dashboard 응답의 `praiseSummary` 필드도 무해하여 유지(미사용).
- **요구사항 문서 반영(후속 필요)**: 이는 `07_요구사항_정의서`/F-09 변경에 해당한다. 문서 저장소 07/F-09에 "사용자 앱 찬양 큐레이션 노출 제거(2026-06-15 Lead 결정)"를 반영해야 한다. (Lead 검토 표시)

## 구현 (flutter-app)
- `mypage_screen.dart`: 닉네임 부제에 접어두던 통계를 제거하고, `CpSectionTitle(미션)` + `_MissionBlock`(연속/이번 주/이번 달, Calm Paper `CpSubBox` 3지표)으로 분리. '나의 찬양' `CpRow` 제거.
- l10n `missionTitle` 추가(ko 미션 / en Mission) — arb 2 + 생성물 3.
- 찬양 제거: `app_router`(praise route/case/import), `dev_mode_screen`(찬양 dev route), `mypage_providers`(curationSongs/myPraiseSongs provider + import), `mypage_repository`(찬양 4메서드 + import), `praise_screen.dart`·`praise_response.dart` 삭제.

## 테스트 (리뷰 BLOCK ② 대응)
- 신규 위젯 테스트 `test/features/mypage/screens/mypage_screen_test.dart`:
  - 미션 블록이 연속/이번 주/이번 달 지표(라벨+값)를 표시한다(닉네임 부제와 분리).
  - '나의 찬양' 항목이 더 이상 노출되지 않는다(알림 행은 유지).
- 기존 mypage 테스트(`dashboard_response_test`, `quick_menu_card_test` 등) 영향 없음(모델/위젯 미변경).

## 검증
- ⚠️ 샌드박스는 Flutter SDK 미설치 → 코드 대조 + 잔존 참조 0건 확인(PraiseScreen/PraiseSong/MyPraiseSong/AppRouter.praise/curationSongsProvider 등). CI `Flutter Analyze & Test`로 최종 검증.
- PR 전/머지 전: `cd flutter-app && flutter pub get && flutter analyze && flutter test`.

## 후속 추적 — 07/F-09 SSoT 반영 (리뷰 WARN 대응, 머지 비차단)
- CLAUDE.md §2: 요구사항 변경은 Lead 검토 후 문서(SSoT=07_요구사항_정의서) 반영. 본 PR은 코드가 먼저 나갔으므로 **문서 반영 후속 PR이 즉시 따라가야** 한다.
- 추적 항목(머지 후 바로 진행):
  - 작업: 문서 저장소(2nd-Team-Project) `07_요구사항_정의서` F-09 "사용자 앱 찬양 큐레이션 노출 제거(2026-06-15 Lead T 결정)" 반영.
  - 담당: Lead 강태오(T). 기한: 본 PR 머지 후 1영업일 내.
  - 예정 브랜치/PR: `docs/f09-app-praise-removal` → 문서레포 master.
- 서버 F-09 엔드포인트·admin praise 백엔드는 이번 범위에서 제거하지 않음(보존). 완전 폐기 여부는 위 문서 반영 시 함께 결정.

## Git/PR
- branch `feature/app-mypage-mission-block` → PR #677(dev). 보완 커밋: 위젯 테스트 + 본 문서.
- 2차 보완 커밋: 잔여 마커 주석 제거 + 본 후속 추적 섹션 추가(리뷰 WARN 반영).
