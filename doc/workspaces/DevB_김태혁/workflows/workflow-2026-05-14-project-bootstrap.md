# Workflow — 2026-05-14 project-bootstrap

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `main` |
| 작업 패널 | bible-engine 레포 초기화 및 1달 MVP 비전 정립 |
| 기능 ID | F-00 |
| 기준 문서 | `STYLE_GUIDE.md`, `.cursorrules`, 큐티 영상 기획서 초안 |

## 작업 목표

- 한국어 성경 본문을 자동 재생 캐릭터 애니메이션 MP4로 변환하는 콘텐츠 생성 엔진 레포를 초기화한다.
- Remotion + TypeScript strict + React 18 기반의 1인 개발 1달 MVP 스택을 확정한다.
- Rive/Lottie/Spine 등 외부 도구 의존 없이 React + Canvas로 직접 구현하는 방침을 못박는다.
- Cursor AI가 매 코드 생성 시 참조할 컨텍스트 문서(`.cursorrules`)를 작성한다.

## 수정 예정 경로

- `bible-engine/README.md`
- `bible-engine/STYLE_GUIDE.md`
- `bible-engine/.cursorrules`
- `bible-engine/.gitignore`
- `bible-engine/package.json`
- `bible-engine/tsconfig.json`
- `bible-engine/src/index.ts`
- `bible-engine/src/Root.tsx`

## 검증 계획

- `npm install` 후 의존성 충돌 없음 확인
- `npm run preview` 실행 시 Remotion Studio 정상 기동
- `npx tsc --noEmit` 클린 빌드
- README와 STYLE_GUIDE의 비주얼 톤 일관성 검토

## 막힌 점

- 캐릭터 디자인 톤은 1주차 첫 캐릭터(요나) 작업하면서 정확한 기준을 정한다.
- 모바일 앱은 별도 트랙으로 진행 — 이 레포 범위 밖.
