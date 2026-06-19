# 리포트 — QT 영상 한시적 고정 결과

실행 2026-06-19 · F-12(QT 영상)/F-06(관리자 운영) · 브랜치 feature/qt-video-temp-today-fallback

## 결과
- 오늘 본문에 영상이 없을 때 가장 최근 등록(APPROVED) 영상(현 데이터 기준 6/15 등록분)을 한시적으로 노출.
- 오늘 본문이 아니거나(과거), 본문에 클립이 있으면(READY/HIDDEN/FAILED) 폴백 미적용 — 기존 동작 유지.

## 변경 파일
| 파일 | 내용 |
| --- | --- |
| QtVideoService.java | 오늘 미생성 시 최근 APPROVED 클립 폴백(임시) + Clock 주입 |
| QtVideoClipRepository.java | findTopByStatus...(최근 1건) 추가(임시) |
| QtVideoServiceTest.java | 고정 시계 + 폴백 검증 2건 추가 |

## 임시/원복
- 모든 추가 코드에 `[임시 2026-06-19]` 마커. 원복 절차는 워크플로우 문서 참조(또는 PR revert).

## 주의 / 한계
- 폴백 응답의 qtPassageId/clipId는 "오늘 본문"이 아니라 "폴백 클립(6/15)"의 값이다(앱은 videoUrl로 재생하므로 표시엔 무해).
- "오늘(KST)"에만 적용. 영구 로직 아님.

## 검증
- BUILD SUCCESSFUL(34s). 단위 2 신규 + 8 기존 + 통합(QtVideoControllerTest) 통과.

## 리뷰 반영 (#694)
- (경계 테스트 보강) "오늘 본문 + 최근 등록 영상도 없음 → MISSING" 단위 테스트 추가(`todayMissing_noRecentClip_staysMissing`).
- (앱 의존성 확인) 앱은 영상 응답의 `clipId`/`qtPassageId`를 텔레메트리·후속 호출에 사용하지 않음 — 영상은 `videoUrl`로만 재생/캐시하고, 해설·시뮬레이터 등 후속 버튼은 `/qt/today`의 오늘 본문 id(`data.qtPassageId`)를 사용. 따라서 폴백 클립 id 불일치는 무해.
- (임시 재확인) 영구 로직화 금지 — QT 영상 생성 정상화 시 즉시 원복(코드 마커 `[임시 2026-06-19]` + 본 문서/워크플로우 절차).
