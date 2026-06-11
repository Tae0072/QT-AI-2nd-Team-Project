# Workflow — 2026-06-04 bible-animation-pipeline

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/bible-animation-pipeline |
| 작업 패널 | W23-106 성경 애니메이션 파이프라인 구축 |
| 기능 ID | bible-engine / compositions |
| 기준 문서 | Task #106, #107, #108 |

## 작업 목표
- 성경 본문 → AI 이미지 생성 → Remotion 영상 렌더링 파이프라인 완성
- 1절 = 1장면 방식으로 본문 누락 없는 영상 제작
- ComfyUI + SDXL + LoRA로 일관된 캐릭터 이미지 생성
- Ken Burns, 파티클, 동적 조명, 타이핑 텍스트 효과 적용

## 수정 예정 경로
- `scripts/generate_scene.py` — 이미지 생성 스크립트 (프롬프트 개선 완료)
- `src/compositions/Corinthians2.tsx` — 고린도전서 2장 (캐릭터 레이어 제거 완료)
- `src/compositions/Corinthians11.tsx` — 고린도전서 11장 (15장면 버전 완료, 렌더링 미완)
- `src/compositions/Philippians4_4.tsx` — 빌립보서 4장 4-9절 (완료)
- `src/Root.tsx` — 컴포지션 등록
- `public/scenes/cor11_*.png` — 고린도전서 11장 이미지 15장 (재생성 완료)
- `public/scenes/phil_*.png` — 빌립보서 4장 이미지 4장 (완료)
- `public/videos/*.mp4` — 렌더링 결과물

## 검증 계획
- `npx remotion render` 성공 여부 확인
- 영상 재생하여 텍스트/이미지/효과 정상 출력 확인
- 캐릭터 얼굴 일관성 육안 검수
- 금지 패턴 확인 (female/woman/girl/child 프롬프트 제외)

## 막힌 점
- 고린도전서 11장 v3 렌더링 미완: 1970프레임(66초) 영상이 PowerShell 타임아웃에 걸림. 내일 직접 터미널에서 렌더링하거나 concurrency 조정 필요
- public 폴더 533MB로 비대해짐 → 기존 영상들 tmp_videos로 이동 조치함, 영구적으로 videos 폴더를 .gitignore 또는 별도 관리 필요
