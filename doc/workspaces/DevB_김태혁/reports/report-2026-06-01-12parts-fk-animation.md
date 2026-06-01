# Report — 2026-06-01 12parts-fk-animation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/12parts-fk-animation |
| 작업 패널 | W22-102 12파츠 FK 스켈레탈 애니메이션 구현 |

## 변경 내용
- **8파츠 → 12파츠 전환**: 기존 arm/leg 단일 이미지를 upper/lower로 분리하여 관절(무릎·팔꿈치) 꺾임이 자연스럽게 동작하도록 변경
- **draw_parts_v2.py 작성**: PIL로 12개 독립 파츠 이미지를 처음부터 새로 그리는 스크립트 (기존 사진 크롭 아님)
  - head, torso, upper_arm_L/R, lower_arm_L/R, upper_leg_L/R, lower_leg_L/R, foot_L/R
- **animate.html 재작성**: Joint-rotation FK 방식의 12파츠 애니메이션 시스템
  - REST 관절 좌표 기반, 부모→자식 순서로 회전 delta 누적
  - smooth-step 보간으로 자연스러운 모션
  - 4종 애니메이션: idle(서기), walk(걷기), wave(인사), jump(점프)
  - 뼈대/파츠 토글 표시 기능
- **manifest.json 업데이트**: 12파츠 매니페스트 (파츠명, 파일, 크기, 피벗 좌표)
- **다리 X자 교차 문제 해결**: 8파츠 시스템에서 단일 다리 이미지가 FK 무릎 관절과 불일치하던 문제를 upper/lower 분리로 근본 해결

## 검증 결과
- **브라우저 테스트 (localhost:9877/v2/animate.html)**: 4종 애니메이션 모두 시각 확인 완료
  - idle: 미세한 호흡 모션 정상 동작
  - walk: 다리 교대 스윙 + 팔 반대 스윙 정상, X자 교차 없음
  - wave: 오른팔 머리 위 들어올림 + 하완 흔들기 정상
  - jump: rootY 오프셋으로 공중 점프 + 착지 정상
- **FPS**: 60fps 안정 (탭 포커스 시)
- **파츠 독립성**: 각 파츠가 100% 독립 — 팔은 팔만, 다리는 다리만, 겹침 없음

## 확인한 금지선
- AI 자유 챗봇/SSE/RAG 없음
- Kafka/Kubernetes/Helm 없음
- 금지 번역본·가사·음원·직접 YouTube URL 저장 없음
- 담당 범위 밖 변경 없음
- Fooocus 미사용 (금지 도구)
- child/kid/boy 등 금지 프롬프트 미사용

## 남은 리스크
- 발(foot) 파츠의 피벗·각도가 정지 시 약간 V자 형태 — 미세 조정 필요
- 걷기 애니메이션 무릎 꺾임 각도 추가 튜닝 여지 있음
- 의상(로브) 레이어 미구현 — 현재 맨몸 + 반바지 상태
- Remotion 컴포넌트 통합 미완료
