# Workflow — 2026-06-05 prompt-upgrade-and-rendering

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/prompt-upgrade-and-rendering |
| 작업 패널 | W23-109 프롬프트 업그레이드 및 렌더링 최적화 |
| 기능 ID | bible-engine / scripts, compositions |
| 기준 문서 | Task #106, 팀원 해석 가이드 (고린도전서 11장 콘티) |

## 작업 목표
- 고린도전서 11장 해석 기반 이미지 15장 재생성 (팀원 제공 콘티 반영)
- 고린도전서 전체 422절 프롬프트를 본문 의미 기반으로 업그레이드
- 자막 시스템 개선 (타이핑 → 페이드인, 절 번호 위치 변경)
- 해상도 480p 전환 (1920x1080 → 854x480)
- TTS 연동 테스트 후 제거 결정
- 절당 duration 10초로 변경
- 고린도전서 13장 사랑장 영상 신규 제작

## 수정 예정 경로
- `scripts/cor1_prompts.py` — 422절 프롬프트 업그레이드 (generic → 의미 기반)
- `scripts/generate_scene.py` — allow_female 옵션 추가, 해상도 480p 변경
- `scripts/gen_cor11_v3.py` — 해석 기반 11장 이미지 생성 스크립트
- `scripts/batch_generate.py` — 배치 이미지 생성 스크립트
- `scripts/measure_tts.py` — TTS 자동 측정 스크립트 (신규)
- `scripts/generate_tts.py` — TTS API 연결 틀 (신규)
- `scripts/split_tts.py` — TTS 묵음 분할 (임시)
- `src/compositions/Corinthians11.tsx` — TTS 연동 → 제거, 자막 개선, 10초 duration
- `src/compositions/Corinthians13.tsx` — 사랑장 신규 컴포지션
- `src/compositions/Corinthians2.tsx` — 자막 whiteSpace 수정
- `src/compositions/Philippians4_4.tsx` — 자막 whiteSpace 수정
- `src/Root.tsx` — 해상도 480p 변경, Corinthians13 등록
- `public/scenes/cor11r_*.png` — 해석 기반 이미지 15장
- `public/scenes/cor13_*.png` — 사랑장 이미지 13장
- `public/tts/cor11_v*.wav` — TTS 분할 파일 15개

## 검증 계획
- `npx remotion render` 성공 여부 확인
- 영상 재생하여 자막 위치/크기 확인 (480p 최적화)
- 이미지 육안 검수 (해석 반영 여부, 손/얼굴 퀄리티)
- 금지 패턴 확인 (여성 캐릭터는 본문 필요 시만 허용)

## 막힌 점
- TTS API 미연결: Voice Studio ID 확보했으나 팀원 로컬 환경 제한으로 API 직접 연결 불가. TTS 기능은 일단 제거 결정
- 고린도전서 전체 이미지 422장 생성 미완: 프롬프트 업그레이드 완료, 생성 대기 중
