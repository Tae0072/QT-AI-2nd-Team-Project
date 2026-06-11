# Workflow — 2026-05-27 jonah-three-scenes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/jonah-scenes` |
| 작업 패널 | 요나서 1~2장 3편 시나리오 작성 + 영상 렌더 |
| 기능 ID | F-09 |
| 기준 문서 | 요나서 1:1~2:10, `STYLE_GUIDE.md` |

## 작업 목표

- 요나 시리즈 첫 3편을 JSON으로 작성하고 MP4로 렌더한다.
  - jonah_01: 다시스로 도망 — 부두에서 배에 오름
  - jonah_02: 큰 폭풍 — 배 위에서 사람들 두려워함
  - jonah_03: 물고기 뱃속 — 어두운 화면 + 기도 음성
- 각 편 8~10초, 자막 + 배경 음악 placeholder.
- 카메라 워크 통합: 폭풍 씬에 shake, 회개 씬에 zoom-in.
- 파티클 효과: storm 씬에 rain.

## 수정 예정 경로

- `src/scenes/jonah_01.json`
- `src/scenes/jonah_02.json`
- `src/scenes/jonah_03.json`
- `package.json`에 render 스크립트 추가 (render:jonah01/02/03, render:all)

## 검증 계획

- 3편 모두 npm run render:jonahXX 명령으로 MP4 출력.
- 영상 시청해서 자연스러운지 확인.
- 캐릭터 일관성 (요나가 3편 내내 같은 사람으로 보이는지).

## 막힌 점

- jonah_03 (물고기 뱃속) — 캐릭터 외 어두운 공간만 그리기 까다로움. 일단 짙은 청록색 배경 + 캐릭터 무릎 꿇기 자세.
- 자막 시간 timing은 자막 길이에 비례하게 수동 조정.
