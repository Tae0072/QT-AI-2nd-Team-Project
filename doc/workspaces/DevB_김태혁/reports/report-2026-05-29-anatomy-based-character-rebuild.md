# Report — 2026-05-29 anatomy-based-character-rebuild

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/anatomy-character` |
| 작업 패널 | 분리 가능한 T-pose 예수 재생성 시도 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE (실패 — 다른 경로로) |

## 변경 내용

- ComfyUI에서 T-pose 예수 생성 8장 시도.
  - 프롬프트에 `T-pose, arms extended fully outward horizontally` 추가
  - FluxGuidance 3.5, denoise 1.0 (txt2img)
- 결과: T-pose는 일부 성공했지만 캐릭터 디자인 변동성 큼.
  - 흰 로브 + 빨간 띠 패턴이 자주 깨짐
  - 얼굴 비율 변동
- ControlNet OpenPose 시도하려 했으나 외부 모델 다운로드 불가.
- 결론: T-pose 강제는 캐릭터 일관성과 trade-off 너무 큼.

## 검증 결과

- 8장 중 T-pose 자세 + 캐릭터 일관성 모두 만족하는 게 1장도 없음
- 시드/CFG/denoise 조합 4회 변경 시도 — 개선 미미
- 자세는 강제됐지만 캐릭터가 "다른 사람"이 됨

## 확인한 금지선

- 외부 ControlNet 다운로드 불가
- 캐릭터 디자인 일관성 우선 (자세보다 더 중요)

## 남은 리스크

- T-pose 없이는 부위 자동 컷팅 불가 — 부위 리깅 경로 포기.
- 대안: 자세별 통짜 PNG (idle.png, walk.png, pray.png ...).

## 회고

T-pose는 리깅 친화적이지만 캐릭터 일관성을 깨뜨린다. 우리 캐릭터의 시그니처 (흰 로브 + 빨간 띠 + 자연 자세)를 유지하면서 부위 분리하는 건 현재 도구로 불가능. 자세별 통짜 PNG 방식이 더 실용적.
