# Report — 2026-05-26 scene-json-renderer

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/scene-json` |
| 작업 패널 | JSON 씬 정의 + 범용 SceneRenderer |
| PR | 로컬 커밋 |
| 최종 상태 | DONE |

## 변경 내용

- `src/types.ts`에 Scene 관련 타입 추가:
  - `Scene` — id, title, reference, durationFrames, fps, scenery, camera?, characters[], subtitles?[], particles?[], transitionIn?, transitionOut?, events?[]
  - `CharacterPlacement` — character(이름), timeline[], lookAt?[], ik?[], positionX?
  - `SubtitleEntry`, `SceneEvent`, `LookAtKeyframe`, `IKKeyframe`
- `src/compositions/SceneRenderer.tsx` 작성 — 범용 렌더러:
  - Sky variant 적용
  - SceneryExtra[] 순회해 배경 합성
  - Camera transform 적용 (Camera.tsx 사용)
  - 각 character.timeline로 getCurrentPose 계산
  - 캐릭터 이름 → 컴포넌트 dispatcher (Jonah/Moses/David/Peter/Jesus)
  - Particles 오버레이
  - Subtitles 하단 표시
- `src/scenes/all.ts` — JSON 파일들 import 후 `ALL_SCENES` export
- `src/scenes/jonah_01.json` 초안:
  - 240프레임 (8초)
  - storm sky + stormy sea + rocking ship
  - 요나가 갑판에서 idle → walk → sit-edge
  - 자막: "요나는 다시스로 가는 배에 올랐다"
- `src/Root.tsx`에 `{ALL_SCENES.map((scene) => <Composition id={scene.id} ...>)}` 추가:
  - id는 `_` → `-` 변환 (jonah_01 → jonah-01)

## 검증 결과

- jonah_01 자동 등록 확인 (Studio 컴포지션 목록에 jonah-01 표시)
- 미리보기에서 캐릭터 + 배경 + 자막 정상 출력
- `npm run render:jonah01` 시도 → MP4 출력 성공 (8초)
- `npx tsc --noEmit`: PASS

## 확인한 금지선

- 시나리오 작성 시 코드 변경 0건 — JSON만 추가하면 됨
- 씬 ID는 a-z/A-Z/0-9/- 만 사용 (Remotion 규칙)

## 남은 리스크

- 캐릭터 이름 dispatcher가 if/else로 되어있음 — 캐릭터 늘어나면 lookup 테이블로 전환.
- 자막은 단순 하단 텍스트 — 강조/색상 옵션은 추후 확장.

## 회고

오늘이 진짜 "엔진"이 된 날. 시나리오 작성자가 JSON 한 파일만 추가하면 8초짜리 영상이 자동으로 만들어진다. SceneRenderer가 모든 것을 추상화해주니, 콘텐츠 제작이 코드 작업이 아니라 글쓰기가 됐다. 이게 1달 MVP의 핵심 — 코드는 한 번만, 콘텐츠는 무한.
