# Workflow — 2026-06-01 animation-tuning-remotion

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/animation-tuning-remotion |
| 작업 패널 | W23 애니메이션 튜닝 + Remotion 통합 |
| 기능 ID | bible-engine / 2d-skeletal-animation |
| 기준 문서 | TASK_SCHEDULE.md, report-2026-06-01 |

## 작업 목표
- 발(foot) 파츠 피벗 각도 보정 — 정지 시 V자 → 수평 정렬
- 걷기 애니메이션 무릎 꺾임 각도 미세 튜닝
- 의상 레이어(로브, 샌들) 추가 — 맨몸 위에 옷 파츠 오버레이
- 12파츠 FK 시스템을 Remotion SkeletalCharacter 컴포넌트로 통합
- 추가 캐릭터(제자 등) 확장 가능한 구조 설계

## 수정 예정 경로
- public/characters/jesus/layered/v2/animate.html — 애니메이션 키프레임 튜닝
- scripts/draw_parts_v2.py — 의상 레이어 파츠 추가
- public/characters/jesus/layered/v2/parts/ — 의상 파츠 이미지
- src/components/SkeletalCharacter.tsx — Remotion 컴포넌트 신규 생성
- src/compositions/ — 테스트 컴포지션 추가
- docs/TASK_SCHEDULE.md — 진행 상황 업데이트

## 검증 계획
- 브라우저 테스트: localhost에서 4종 애니메이션 시각 확인
- Remotion Studio: npx remotion studio로 렌더링 프리뷰 확인
- 파츠 독립성 검증: 각 파츠 이미지가 겹침 없이 독립적인지 확인
- 의상 레이어 z-order: 몸 파츠 위에 옷이 정확히 올라가는지 확인

## 막힌 점
- 없음
