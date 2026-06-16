# Workflow — 2026-05-29 anatomy-based-character-rebuild

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/anatomy-based-character |
| 작업 패널 | W22-90 해부학 기반 캐릭터 시스템 재구축 |
| 기능 ID | bible-engine / character-animation |
| 기준 문서 | docs/ANATOMY_REFERENCE.md |

## 작업 목표
- 인체 해부학 비율·관절 위치·가동범위를 기반으로 파츠 분리 스크립트 완전 재작성
- 상완/전완, 대퇴/하퇴 분리로 팔꿈치·무릎 관절 추가 (8파츠 → 12파츠)
- 관절 겹침(overlap) 영역을 원형으로 처리하여 회전 시 빈 틈 제거
- ROM 데이터 기반 걷기/인사/점프 애니메이션 재작성
- Canvas preview로 결과 시각 검증

## 수정 예정 경로
- scripts/gen_layered_character.py — split_body_parts() 해부학 기반 재작성
- scripts/spine/build_spine_json.py — 12파츠 본 계층 + ROM 기반 애니메이션
- public/characters/jesus/layered/spine_config.json — 새 본 계층 구조
- public/characters/jesus/layered/parts/** — 재생성될 파츠 이미지들
- public/characters/jesus/layered/spine/** — atlas, skeleton, preview 재생성
- docs/ANATOMY_REFERENCE.md — 이미 작성 완료

## 검증 계획
- python scripts/gen_layered_character.py 실행 → 12파츠 생성 확인
- python scripts/spine/build_spine_json.py 실행 → skeleton.json 유효성
- canvas_preview.html에서 idle/walk/wave/jump 시각 검증
- 관절 겹침 부분 확대 검사 (빈 틈 없는지)
- Toggle Bones로 관절 위치 해부학적 정확성 확인

## 막힌 점
- 없음
