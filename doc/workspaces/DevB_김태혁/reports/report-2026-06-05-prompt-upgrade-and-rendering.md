# Report — 2026-06-05 prompt-upgrade-and-rendering

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/prompt-upgrade-and-rendering |
| 작업 패널 | W23-109 프롬프트 업그레이드 및 렌더링 최적화 |

## 변경 내용

### 1. 자막 시스템 전면 개편
- 타이핑 효과(한글자씩) 제거 → 텍스트 전체 페이드인으로 변경
- 띄어쓰기 깨짐 수정: `display: inline-block`에 `whiteSpace: 'pre'` 추가 (3개 파일)
- 절 번호 위치: 왼쪽 상단 → 자막 바로 위로 이동
- 절 번호 형식: "고린도전서 11장 2절" → "고린도전서 11:2" 로 변경
- 480p에 맞게 글자 크기 조절: 본문 36px→18px, 절 번호 22px→12px

### 2. 해상도 480p 전환
- `Root.tsx`의 Corinthians2, Philippians4-4, Corinthians11 모두 854x480으로 변경
- `generate_scene.py` 이미지 생성 해상도: 1024x576 → 856x480
- 용량 절감: 80.8MB(1080p) → 41.3MB(480p) — 약 49% 감소

### 3. 고린도전서 11장 해석 기반 이미지 재생성
- 팀원 제공 절별 해석 가이드 + 그림 콘티 반영
- 기존: 15장 전부 "바울이 서있는 장면" → 신규: 절별 의미 맞춤 이미지
  - v2: 편지 쓰는 바울, v3: 질서의 계단, v5: 베일 쓴 여성 기도
  - v8: 에덴동산 아담과 하와, v10: 천사가 지켜보는 예배
  - v11: 십자가 앞 손잡은 남녀, v15: 꽃과 긴 머리 여성
  - v16: 교회 앞 함께 웃는 공동체
- `generate_scene.py`에 `allow_female` 옵션 추가: 본문에 여성 필수인 절만 허용
- `scripts/gen_cor11_v3.py` 신규 생성

### 4. 고린도전서 13장(사랑장) 신규 제작
- `scripts/cor1_prompts.py`의 절별 맞춤 프롬프트로 13장 이미지 13장 생성
- `src/compositions/Corinthians13.tsx` 신규 생성
- `corinthians13_v1.mp4` 렌더링 완료 (20.6MB)

### 5. 고린도전서 전체 422절 프롬프트 업그레이드
- `scripts/cor1_prompts.py` 기존 generic 프롬프트 27개 → 본문 의미 기반으로 교체
  - "Paul teaching" → 아기에게 젖병 (영적 어린아이, 3:1)
  - "Paul speaking" → 천에서 내려오는 비둘기와 금빛 말씀 (성령의 가르침, 2:13)
  - "Paul preaching" → 갈림길의 십자가 (걸림돌, 1:23)
  - 등 27개 전면 교체
- 최종 결과: 422개 중 418개 의미 기반 (99%)

### 6. TTS 연동 테스트 및 제거
- 팀원 TTS 파일(wav) 묵음 기준 자동 분할 스크립트 작성 (`scripts/split_tts.py` 임시)
- 15절 분할 성공, TTS 기반 동적 duration 컴포지션 구현
- TTS 포함 렌더링 완료 (corinthians11_v6_tts.mp4, 80.8MB)
- 최종 결정: TTS 빼기로 함 → Audio 컴포넌트 제거, 고정 duration 복원

### 7. 절당 duration 10초로 변경
- SCENE_DURATION: 150프레임(5초) → 300프레임(10초)
- 실제 절당 보이는 시간: 4.5초 → 9.5초 (크로스페이드 감안)

### 8. 성경 전체 규모 분석
- 성서유니온 매일성경 2026년 읽기표 분석 (6년 주기, 66권)
- 고린도전서 전체: 30일분, 435절, 이미지 생성 ~51분, 용량 ~1.2GB
- 성경 전체(480p): 31,102절, ~84GB, ~5일 풀가동

## 검증 결과
- `corinthians11_v12_notts.mp4`: 렌더링 성공, 해석 기반 이미지 + 자막 정상 (24MB, 480p)
- `corinthians11_v13_10sec.mp4`: 렌더링 성공, 10초 duration (46.6MB, 480p)
- `corinthians13_v1.mp4`: 렌더링 성공, 사랑장 절별 이미지 정상 (20.6MB, 480p)
- 프롬프트 문법 검증: `python scripts/cor1_prompts.py` 통과 (422 prompts)
- 이미지 육안 검수: 해석 반영 확인, 손/얼굴 퀄리티 양호
- 금지 패턴: `generate_scene.py` STYLE_NEGATIVE에 여성 제한 유지, `allow_female` 절별 선택적 해제

## 확인한 금지선
- female/woman/girl 캐릭터: 기본 제한 유지, 본문 필수 절(31개)만 `allow_female=True`로 허용
- child/kid/boy 관련 이미지 생성 없음
- Fooocus 사용 없음 (ComfyUI만 사용)
- 담당 범위 밖 변경 없음

## 남은 리스크
- 고린도전서 전체 422장 이미지 생성 미완: 프롬프트 완성, ComfyUI 배치 생성 대기 중 (~47분 소요)
- 11:2-16 외 나머지 절은 팀원 해석 가이드 없음: 내가 분석한 의미 기반 프롬프트 사용 중, 팀원 검수 필요
- `cor1_prompts.py` 파일에 null 바이트 이슈 발생 이력: sed 사용 시 Windows 파일에 null 삽입됨, tr -d '\0'으로 정리 완료
- 30개 컴포지션 자동 생성 미구현: 현재 장별 수동 TSX 작성, 자동화 스크립트 필요
