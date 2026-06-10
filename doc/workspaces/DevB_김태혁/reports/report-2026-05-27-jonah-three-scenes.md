# Report — 2026-05-27 jonah-three-scenes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/jonah-scenes` |
| 작업 패널 | 요나 3편 시나리오 + 첫 영상 출력 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/scenes/jonah_01.json` — 다시스로 도망
  - 270프레임 (9초)
  - dawn sky + Pier + DistantIsland + Ship
  - 요나: idle → walk → sit-edge → idle
  - 자막: "주의 말씀이 요나에게 임하니" / "그러나 요나는 다시스로 도망쳤다"
  - 카메라: pan-right
- `src/scenes/jonah_02.json` — 큰 폭풍
  - 300프레임 (10초)
  - storm sky + stormy sea + rocking ship
  - 요나(sit-edge → pray), 다른 사람(walk → raiseHands 절망)
  - 파티클: rain (intensity 0.8)
  - 카메라: shake (intensity 6, exponential decay)
  - 자막: "여호와께서 큰 바람을 바다 위로 내리시매"
- `src/scenes/jonah_03.json` — 물고기 뱃속
  - 240프레임 (8초)
  - 짙은 청록색 단색 배경 + 작은 lightRays
  - 요나: kneel → pray (회개 자세)
  - 카메라: zoom-in (천천히)
  - 자막: "요나가 물고기 뱃속에서 그의 하나님 여호와께 기도했다"
- `package.json` 스크립트 추가:
  - `render:jonah01`, `render:jonah02`, `render:jonah03`
  - `render:all` (모든 씬 일괄)

## 검증 결과

- npm run render:jonah01: PASS, out/jonah-01.mp4 정상 출력 (9초)
- npm run render:jonah02: PASS, 폭풍·rain·shake 모두 적용됨
- npm run render:jonah03: PASS, 분위기 어두움 잘 표현
- 3편 모두 요나 캐릭터가 동일 (코드 정적 디자인이라 자연 일관성)
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- 자막은 한국어 + 성경 인용 정확성 유지
- 폭력 묘사 없음 — 폭풍은 자연 현상만, 사람 피해 없음
- 어린이 시청 적합 (무서운 클로즈업 없음)

## 남은 리스크

- jonah_03의 단색 배경이 너무 단조로움 — 추후 어두운 cave 텍스처 추가 가능.
- 자막 timing이 수동 — 한국어 TTS 통합 시 자동화 검토.
- 코드 정적 캐릭터라 한국 QT 톤은 약함 — 추후 AI 생성으로 대체 검토 (Phase 2).

## 회고

처음으로 "성경 한 장면"이 영상으로 나온 날. 8초짜리지만 폭풍, 도망, 회개의 정서가 다 담겨있다. 시나리오 → JSON → MP4가 5분이면 끝나는 게 엔진의 진짜 가치. 다음은 캐릭터 톤을 한국 어린이 책 느낌으로 끌어올리는 작업이 필요해 보임.
