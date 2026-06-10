# Workflow — 2026-06-05 video-batch-render

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | feature/video-batch-render |
| 작업 패널 | W23-110 고린도전서 전체 영상 배치 렌더링 |
| 기능 ID | bible-engine / compositions |
| 기준 문서 | Task #106, workflow-2026-06-05-prompt-upgrade-and-rendering |

## 작업 목표
- 고린도전서 30일분(435절) 컴포지션 자동 생성 + 영상 배치 렌더링
- 이미지 437장 전체 생성 완료 확인
- 30개 mp4 파일 출력

## 수정 예정 경로
- `src/compositions/Cor*.tsx` — 30개 컴포지션 자동 생성
- `src/Root.tsx` — 30개 컴포지션 등록
- `scripts/gen_compositions.py` — 컴포지션 자동 생성 스크립트 (신규)
- `scripts/render_all.py` — 배치 렌더링 스크립트 (신규)
- `public/videos/cor*.mp4` — 30개 영상 출력

## 검증 계획
- 30개 영상 전부 렌더링 성공 확인
- 각 영상 재생하여 이미지/자막/효과 정상 출력 확인
- 절당 10초 타이밍 확인

## 막힌 점
- 없음. 이미지 437장 생성 완료, 컴포지션 템플릿(Corinthians11.tsx) 확보, 파이프라인 검증 완료
