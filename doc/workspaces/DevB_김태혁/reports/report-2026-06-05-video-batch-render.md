# Report — 2026-06-05 video-batch-render

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/video-batch-render |
| 작업 패널 | W23-110 고린도전서 전체 영상 배치 렌더링 |

## 변경 내용

### 1. 고린도전서 전체 437장 이미지 생성 완료
- `scripts/gen_all_cor1.py` 배치 스크립트로 409장 자동 생성 (약 48분 소요)
- `scripts/gen_cor11_v3.py`로 11:2-16 해석 기반 15장 별도 생성
- `scripts/batch_generate.py`로 13:1-13 사랑장 13장 별도 생성
- 총 437장 (`public/scenes/cor01_v01.png` ~ `cor16_v24.png`)

### 2. 프롬프트 전체 업그레이드
- `scripts/cor1_prompts.py` 422절 프롬프트 중 27개 generic 프롬프트 → 본문 의미 기반으로 교체
- 최종: 418/422 의미 기반 (99%)
- 업그레이드 예시:
  - 3:1 "Paul speaking" → "아기에게 젖병 (영적 어린아이 비유)"
  - 2:13 "Paul speaking" → "비둘기에서 금빛 말씀이 내려오는 장면"
  - 1:23 "Paul preaching" → "갈림길의 십자가 (걸림돌/어리석음)"

### 3. 절당 10초 정확 맞춤
- `CROSSFADE = 15`, `SCENE_DURATION = 300 + CROSSFADE = 315`
- 절 간 간격 정확히 300프레임(10초)
- 고린도전서 11장 15절 기준: 총 150.6초 = 절당 10.04초 (검증 완료)

### 4. 문서 작성
- `docs/troubleshooting-animation.md` — 관절/일관성/이미지분리 트러블슈팅 14건, 상세 서술
- `docs/development-flow.md` — 전체 개발 플로우 8챕터 (셋업, FK, AI이미지, 분리시도, 최종파이프라인, 프롬프트고도화, 파일구조, 설치환경)
- `workflow-2026-06-05-prompt-upgrade-and-rendering.md`
- `report-2026-06-05-prompt-upgrade-and-rendering.md`

## 검증 결과
- 이미지 437장 파일 존재 확인: PASS (409 + 15 + 13)
- `corinthians11_v14_10sec_exact.mp4`: 렌더링 성공, 150.6초, 절당 10.0초 (48.6MB)
- `corinthians13_v1.mp4`: 렌더링 성공, 사랑장 13절 정상 (20.6MB)
- `scripts/cor1_prompts.py` 문법 검증: `python -c "import py_compile; py_compile.compile()"` PASS
- 프롬프트 generic 비율: 4/422 (0.9%) — 맥락상 적절한 "speaking" 포함

## 확인한 금지선
- female/woman/girl: 기본 제한 유지, `allow_female` 31절만 허용
- child/kid/boy: 제한 유지
- Fooocus: 사용 안 함
- 담당 범위 밖 변경 없음

## 남은 리스크
- **컴포지션 30개 미생성**: 이미지는 437장 완료됐으나, 영상을 만들려면 30일분 컴포지션(.tsx) 자동 생성 + Root.tsx 등록 + 배치 렌더링 필요. 내일 작업 예정
- **이미지 검수 미완**: 437장 중 손/얼굴 문제 이미지가 있을 수 있음. 전수 검사 미실시
- **11장 외 해석 가이드 미반영**: 11장만 팀원 해석 기반, 나머지 29일분은 자체 의미 분석 프롬프트. 팀원 검수 시 재생성 필요할 수 있음
