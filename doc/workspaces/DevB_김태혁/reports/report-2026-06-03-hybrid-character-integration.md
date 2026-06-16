# Report — 2026-06-03 hybrid-character-integration

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/hybrid-character` |
| 작업 패널 | 하이브리드 캐릭터 시스템 + 예수 sprite 통합 |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- **SpriteCharacter** (`src/characters/sprite/SpriteCharacter.tsx`)
  - 범용 스프라이트 렌더러 — `characterId`로 아무 캐릭터나 지원
  - 액션별 PNG 자동 로드 (idle.png, walk.png 등) + idle 폴백
  - 8프레임 (≈ 0.27초) 크로스페이드 전환
  - Pose transform 적용 (positionX, positionY, scale, bodyTilt, headTilt, bodyBend)
- **렌더 모드 레지스트리** (`src/characters/renderMode.ts`)
  - 캐릭터별 'canvas' 또는 'sprite' 지정
  - 현재 매핑: Jesus → sprite, 나머지 → canvas
- **하이브리드 디스패처** (`src/scenes/_renderer/CharacterByName.tsx`)
  - 캐릭터 이름 + 모드 → 적절한 컴포넌트 선택
  - 새 캐릭터를 sprite로 전환하려면 renderMode.ts만 수정 + PNG 제공
- **엔진 개선**:
  - `positionY` 추가 (Pose, drawing.ts, IK 모두 반영)
  - `walk target` 시스템 — walk/run 시 이전→다음 이벤트 위치 보간 (자동 누적 제거)
  - `sit-edge` 포즈 — 절벽/선박 가장자리 앉기
  - ChildBoy (scale 0.7) / ChildGirl (scale 0.65) 등록
- **테스트 씬**: `src/scenes/jesus_peter_test.json`
  - Jesus(sprite) + Peter(canvas) 동일 씬 정상 렌더
  - `npm run render:hybrid` 명령으로 8초 영상 출력
- **예수 idle PNG**: `public/characters/jesus/idle.png` (배경 투명)
- **TypeScript 컴파일 에러 0건** — 클린 빌드 확인

## 검증 결과

- jesus_peter_test 8초 영상 렌더 성공 — 두 캐릭터 자연스럽게 공존
- Jesus가 idle → walk → sit 전환 시 8프레임 크로스페이드 부드럽게 진행
- 자세 PNG 부재 시 idle.png 폴백 동작 확인
- positionY 변경으로 점프 효과 시각 확인
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- Canvas 캐릭터 코드 변경 없음 — 기존 시스템 그대로 유지
- 새 sprite 캐릭터는 PNG만 있으면 추가 — 코드 1줄 (renderMode.ts) 변경
- Remotion 결정론 호환 — 이미지 preload 후 캐싱

## 남은 리스크

- 자세 PNG 컬렉션 미완성 — 현재 idle만 있음. walk/pray/raiseHands/speak 추가 필요.
- SpriteCharacter는 통짜 PNG 전환만 지원 — 표정/입 모양 변화는 불가능.
- 추후 사용자가 더 풍부한 동작을 요구하면 부위별 PNG로 확장 검토.

## 회고

오늘 작업으로 엔진의 두 세계가 합쳐졌다. 코드로 그리는 캐릭터(요나·모세·다윗·베드로)와 AI가 만든 캐릭터(예수)가 한 씬에 자연스럽게 공존. renderMode.ts 한 곳에서 모드를 관리하니 추후 다른 캐릭터도 점진적으로 AI로 전환 가능. 이게 1달 MVP의 결승선 — 시나리오 → 영상 자동화의 모든 부품이 다 모였다.
