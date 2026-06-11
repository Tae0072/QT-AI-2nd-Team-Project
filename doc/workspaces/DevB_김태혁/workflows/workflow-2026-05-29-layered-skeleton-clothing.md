# Workflow — 2026-05-29 layered-skeleton-clothing

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/layered-skeleton-clothing |
| 작업 패널 | W22-81 뼈대+옷 레이어 분리 캐릭터 시스템 |
| 기능 ID | bible-engine / LayeredSkinned |
| 기준 문서 | report-2025-05-28, jesusRig.ts, PartBasedSkinned.tsx |

## 작업 목표
- LoRA로 "몸 레이어"(피부색 팔·다리·몸통)와 "옷 레이어"(로브·샌들)를 분리 생성
- 몸 레이어에서 팔·다리를 독립 파츠로 분할 → 팔 애니메이션 해결
- 옷 레이어를 몸 위에 오버레이하여 최종 캐릭터 합성
- LayeredSkinnedCharacter 컴포넌트 구현 (body parts + clothing overlay)
- 같은 뼈대에 옷만 교체하여 다른 캐릭터 확장 가능한 구조 설계

## 수정 예정 경로
- scripts/gen_layered_character.py — 몸/옷 분리 생성 + 분할 파이프라인 (신규)
- public/characters/jesus/layered/** — 몸 파츠 + 옷 파츠 이미지 (신규)
- src/characters/skinned/LayeredSkinnedCharacter.tsx — 다중 레이어 렌더러 (신규)
- src/compositions/LayeredSkinnedTest.tsx — 테스트 컴포지션 (신규)
- src/Root.tsx — 새 컴포지션 등록

## 검증 계획
- Remotion Studio DOM 검사: 몸 파츠(7개+) + 옷 파츠(3개+) 전부 렌더링 확인
- 팔 회전 시 옷 아래에서 팔이 자연스럽게 움직이는지 시각 확인
- idle/walk/talk 3사이클 애니메이션 동작 검증
- npx remotion render LayeredSkinnedTest — mp4 렌더링
- 금지 패턴 검색 (child/kid 프롬프트 확인)

## 막힌 점
- LoRA가 "옷 없는 몸"을 잘 생성할지 미지수 → 프롬프트 실험 필요
- 몸과 옷의 윤곽이 정확히 일치해야 이음새가 자연스러움 → 같은 seed + 다른 프롬프트로 해결 시도
