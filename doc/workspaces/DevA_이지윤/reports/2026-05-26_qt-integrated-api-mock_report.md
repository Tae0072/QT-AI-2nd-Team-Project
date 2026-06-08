# Report - 2026-05-26 qt-integrated-api-mock

## 작업 요약

Flutter 앱의 오늘 QT 화면 연동을 먼저 진행할 수 있도록 `GET /api/v1/qt/today` 중심의 QT 통합 목업 API 계약과 구현 범위를 정리했다.

초기 workflow는 QT 도메인 안에서 Today QT, 학습 콘텐츠, 시뮬레이터 목업을 모두 처리하는 방향이었지만, 수정 workflow에서 도메인 소유권을 다시 나누었다. `/today`, `/{qtPassageId}`는 `domain.qt`가 소유하고, `/study-content`, `/simulator-clips/{clipId}`는 `domain.study`가 소유하는 기준으로 정리했다.

## 관련 F-ID

- F-01 오늘 QT 화면
- F-03 묵상 노트 진입점
- F-08 쉬운 본문 요약/해설/용어 풀이
- F-12 시뮬레이터 보기
- F-15 AI 질문 진입 가능 여부

## 정리한 계약

| 항목 | 결정 내용 |
| --- | --- |
| Today QT | `GET /api/v1/qt/today`를 사용하고 `domain.qt`가 소유한다. |
| QT 상세 | `GET /api/v1/qt/{qtPassageId}`를 사용하고 `domain.qt`가 소유한다. |
| 학습 콘텐츠 | `GET /api/v1/qt/{qtPassageId}/study-content`를 사용하고 `domain.study`가 소유한다. |
| 시뮬레이터 | `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`를 기준 경로로 둔다. |
| 시뮬레이터 상태 | `READY`, `MISSING`, `FAILED`, `DISABLED` 중 하나로 반환한다. |
| AI 질문 | 실제 Q&A 구현이 아니라 Today QT 화면의 버튼 진입 가능 여부만 표시한다. |
| 인증 | `/api/v1/qt/**`를 공개 예외로 두지 않고 인증된 USER 접근을 전제로 한다. |

## 주요 반영 내용

- `qt-integrated-api-mock` 원본 workflow를 작성해 Today QT 통합 목업 API의 목표, 범위, 제외 범위, 테스트 계획을 정리했다.
- 수정 workflow에서 `domain.qt`와 `domain.study`의 소유 경계를 분리했다.
- `/simulator` 단축 경로는 새 공개 API로 확정하지 않고, 기준 API 명세의 `/simulator-clips/{clipId}`를 우선하도록 정리했다.
- 목업 데이터에도 실제 성경 본문, 금지 번역본, 성서유니온/두란노 본문 텍스트를 넣지 않는 기준을 명시했다.
- 실제 Bible, Study, Note, AI 도메인 연동은 후속 작업으로 분리했다.

## 변경 파일

- `doc/workspaces/DevA_이지윤/workflows/2026-05-26_qt-integrated-api-mock.md`
- `doc/workspaces/DevA_이지윤/workflows/2026-05-26_qt-integrated-api-mock(수정).md`
- `doc/workspaces/DevA_이지윤/reports/2026-05-26_qt-integrated-api-mock_report.md`

## 범위 조정 기록

이 작업은 실제 서버 구현 완료 리포트가 아니라, Flutter 연동을 위한 QT 통합 API의 계약과 작업 범위를 정리한 리포트다.

같은 날짜의 `bible-read-api-spec` PR에서는 Bible Read API 범위와 섞이지 않도록 `qt-integrated-api-mock` workflow 2개를 PR diff에서 제외한 기록이 있다. 따라서 이 리포트는 당일 작업 맥락을 보존하되, 실제 구현 완료 여부는 후속 문서와 구분한다.

후속 실제 구현은 아래 흐름으로 이어졌다.

| 후속 날짜 | 문서 | 내용 |
| --- | --- | --- |
| 2026-05-28 | `2026-05-28_qt-study-simulator_report.md` | `study-content`, `simulator-clips`를 Study 도메인 기준으로 구현 |
| 2026-06-02 | `2026-06-02_flutter-bible-api-integration_report.md` | Flutter Today QT 화면과 `/qt/today`, `/bible/verses` 연동 |
| 2026-06-04 | `2026-06-04_qt-today-review-block-fix_report.md` | Today QT PR BLOCK 항목 수정 |

## 검증 결과

이번 리포트는 누락된 문서 보강 작업이므로 서버 빌드나 테스트를 새로 실행하지 않았다.

문서 기준 확인:

- `04_API_명세서.md`의 QT 관련 경로 확인
- `25_기능_명세서.md`의 F-01, F-08, F-12, F-15 흐름 확인
- 기존 DevA workflow/report 파일명과 후속 문서 흐름 확인

## 남은 리스크 / 후속 작업

- 목업 API 자체는 당일 최종 구현으로 확정하지 않는다.
- 실제 Today QT 본문 조립, Bible 절 범위 조회, Study 승인 해설 조회, Note 묵상 진입점, AI 질문 결과 조회는 각각 후속 PR에서 처리한다.
- PR 본문에 사용할 때는 이 리포트를 "구현 완료"가 아니라 "계약 정리 및 범위 조정" 산출물로 표현해야 한다.
