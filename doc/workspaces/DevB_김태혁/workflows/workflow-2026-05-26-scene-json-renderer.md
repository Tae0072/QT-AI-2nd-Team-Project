# Workflow — 2026-05-26 scene-json-renderer

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/scene-json` |
| 작업 패널 | JSON 씬 정의 + 범용 SceneRenderer |
| 기능 ID | F-08 |
| 기준 문서 | `STYLE_GUIDE.md`, 요나서 1장 시나리오 초안 |

## 작업 목표

- 시나리오를 JSON으로 작성하면 React 코드 변경 없이 영상이 렌더되는 시스템을 구축한다.
- Scene 타입에 caps: id, title, reference, durationFrames, fps, scenery, characters, subtitles?, particles?, transition?, camera?, events?
- 단일 `SceneRenderer.tsx`가 모든 JSON 씬을 처리.
- `src/scenes/all.ts`에 자동 등록 — 새 JSON 파일 추가만으로 Composition 자동 생성.

## 수정 예정 경로

- `src/types.ts` — Scene, CharacterPlacement, SubtitleEntry, SceneEvent 타입
- `src/compositions/SceneRenderer.tsx`
- `src/scenes/all.ts`
- `src/scenes/jonah_01.json` (요나 1장 시작 — 폭풍 직전)
- `src/Root.tsx` — ALL_SCENES.map으로 자동 Composition 등록

## 검증 계획

- jonah_01.json 작성 → Studio에서 자동으로 컴포지션 목록에 표시.
- 8초 미리보기에서 캐릭터 + 배경 + 자막 표시.
- npm run render:jonah01 → MP4 출력.

## 막힌 점

- Composition id는 a-z/A-Z/0-9/- 만 허용 — JSON 파일명에 `_` 사용 시 id 변환 필요.
- 캐릭터 이름(`character: "Jonah"`)을 컴포넌트로 매핑하는 dispatcher 필요.
