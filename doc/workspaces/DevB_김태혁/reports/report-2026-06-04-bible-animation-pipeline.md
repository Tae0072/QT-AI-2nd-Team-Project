# Report — 2026-06-04 bible-animation-pipeline

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/bible-animation-pipeline |
| 작업 패널 | W23-106 성경 애니메이션 파이프라인 구축 |

## 변경 내용

### 1. 고린도전서 2장 캐릭터 레이어 제거 (v11)
- `src/compositions/Corinthians2.tsx`에서 캐릭터 분리 레이어(charPulse, _char.png) 코드 전체 삭제
- 잔상(ghosting) 문제 해결 불가로 사용자 판단에 따라 기능 제거
- 배경 패럴랙스 + 파티클 + 조명 + 타이핑 텍스트만 유지
- `corinthians2_v11.mp4` 렌더링 완료 (21.1MB)

### 2. 빌립보서 4장 4-9절 영상 신규 제작
- `src/compositions/Philippians4_4.tsx` 신규 생성 (4장면)
- ComfyUI로 장면 이미지 4장 생성 (`phil_01~04.png`)
- `src/Root.tsx`에 Philippians4-4 컴포지션 등록
- `philippians4_4_v2.mp4` 렌더링 완료 (25.4MB)

### 3. 고린도전서 11장 2-16절 영상 제작 (1절=1장면)
- `src/compositions/Corinthians11.tsx` 신규 생성
- 기존 4장면 → 15장면(1절=1장면)으로 구조 변경 (사용자 요청)
- SCENE_DURATION: 180→150프레임, CROSSFADE: 25→20프레임
- 타이핑 속도 0.8→1.0으로 상향 (짧은 장면에 맞춤)
- `src/Root.tsx`에 Corinthians11 컴포지션 등록

### 4. 이미지 생성 품질 개선
- `scripts/generate_scene.py` 프롬프트 개선:
  - Positive에 `well-defined facial features, consistent character design, beautiful detailed face` 추가
  - Negative에 `deformed face, ugly face, disfigured, bad anatomy, asymmetric face, extra fingers, mutated hands, poorly drawn face, distorted features` 추가
- 캐릭터 묘사 통일: "a middle-aged bearded man with short brown hair wearing simple brown ancient robes"
- 고린도전서 11장 이미지 15장 재생성 완료 (`cor11_01~15.png`)

### 5. 파이프라인 정리 다이어그램
- 전체 파이프라인 6단계 시각화 (Input → Scene Planning → Image Gen → Composition → Render → Output)

## 검증 결과
- `corinthians2_v11.mp4`: 렌더링 성공, 645프레임, 잔상 없음 확인
- `philippians4_4_v2.mp4`: 렌더링 성공, 645프레임, 이미지+텍스트 정상
- `corinthians11_v2.mp4`: 렌더링 성공, 1970프레임 (v2 = 구 이미지 버전, 57.7MB)
- `corinthians11_v3.mp4`: **미완** — 1970프레임 렌더링이 PowerShell 타임아웃 초과로 완료 불가
- 이미지 15장 육안 검수: 얼굴 일관성 양호, 기괴한 얼굴 없음
- 금지 패턴 검색: generate_scene.py STYLE_NEGATIVE에 `female, woman, girl, child, kid, boy` 포함 확인

## 확인한 금지선
- female/woman/girl 캐릭터 이미지 생성 없음
- child/kid/boy 관련 이미지 생성 없음
- Fooocus 사용 없음 (ComfyUI만 사용)
- 담당 범위 밖 변경 없음

## 남은 리스크
- **고린도전서 11장 v3 렌더링 미완**: 새 이미지 15장은 생성 완료됐으나 영상 렌더링이 타임아웃으로 실패. 내일 터미널에서 직접 `npx remotion render Corinthians11 public/videos/corinthians11_v3.mp4 --codec h264 --concurrency=4` 실행 필요
- **public 폴더 비대화**: 영상 파일 누적으로 533MB까지 증가. 기존 영상들을 `tmp_videos/`로 이동 조치했으나, 영구적 관리 방안 필요 (videos를 public 밖으로 이동 또는 .gitignore)
- **TTS 미적용**: 음성 나레이션은 아직 미정 상태
- **빌립보서/고린도전서 2장도 1절=1장면으로 미전환**: 고린도전서 11장만 적용, 나머지는 기존 4장면 유지 중
