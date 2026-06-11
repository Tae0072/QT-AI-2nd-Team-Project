# Report — 2026-06-11 코드리뷰 P5c admin-web code-split

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `chore/admin-web-code-split` (base `origin/dev`) |
| PR 링크 | (PR 생성 후 작성) |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 5 (P5c) |
| 워크플로우 | [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md) |

## 변경 내용

빌드 산출물이 1.15MB 단일 청크여서, `vite.config.ts`에 `build.rollupOptions.output.manualChunks`를 추가해 라이브러리 단위로 분리.

- `antd` = `['antd', '@ant-design/icons']`
- `vendor` = `['react', 'react-dom', 'react-router-dom', 'axios']`

## 검증 결과 (build 청크 비교)

| 청크 | 전 | 후 |
| --- | --- | --- |
| index(앱 코드) | 1,162 kB | **61 kB** |
| vendor | — | 204 kB |
| antd | — | 929 kB |

- `npm run typecheck` + `npm run build` 통과.
- **핵심 이득**: antd/vendor는 거의 변하지 않아 브라우저가 캐싱 → 앱 코드만 바뀌는 일반 배포 시 사용자는 61KB(앱 청크)만 새로 받음(이전엔 1.15MB 전체).

## 남은 리스크 / 후속

- antd 청크(929KB)는 여전히 >500KB라 빌드 경고는 남음 — antd 자체가 큰 것이며, 추가 축소는 **라우트별 동적 import(lazy)** 가 필요(별도 작업, 본 범위 밖). 경고는 정보성이며 빌드 실패 아님.
- (선택) `build.chunkSizeWarningLimit` 상향으로 경고 노이즈만 줄일 수 있으나, 실질 개선이 아니라 본 PR에선 미적용.
- 로드맵 다음: P5b(SDK 안내) → P4(토큰 보관). (P5a 찬양 숨김 보류)
