# 2026-06-08 feature/20260604 dev 최신화 + 리뷰 차단 대응 workflow

## 작업 목적

`feature/20260604` PR을 최신 `origin/dev` 기준으로 다시 맞추고, Claude 리뷰에서 `REQUEST_CHANGES`로 지적된 차단 가능 항목을 PR 전에 제거한다.

## 기준

- 기준 브랜치: `origin/dev` `bf69da4`
- 작업 브랜치: `feature/20260604`
- 주요 기준 문서: `07_요구사항_정의서.md` v3.1, `18_코드_품질_게이트.md`, `09_Git_규칙.md`

## 수행 범위

- `origin/dev` 병합 및 충돌 해소
- Flutter dev header 정책 보정
- Today QT 해설/시뮬레이터/노트 버튼 활성 조건 보정
- 성경 브라우저 장별 절 수 로딩 실패 시 임의 절 조회 차단
- Flutter 테스트 fixture의 실제 성경 본문 문장 제거
- PR 본문과 report 갱신

## 제외 범위

- admin-web, music, sharing, AI 등 `origin/dev` 신규 기능 자체 수정
- OpenAPI 전체 계약 재작성
- 운영 배포 설정 변경

## 협의 필요 여부

현재 적용한 결정은 기존 팀 컨벤션을 더 보수적으로 적용한 범위다. 별도 담당자 협의가 필요한 새 정책 결정은 없음.

## 검증 계획

- `flutter analyze`
- `flutter test test/core/network/api_client_test.dart test/features/bible test/features/note/screens/qt_note_editor_screen_test.dart test/routes/app_router_test.dart`
- `./gradlew.bat build` (`qtai-server`)
- `./gradlew.bat test --tests "*GlossaryTermServiceTest" --tests "*GlossaryTermRepositoryTest" --tests "*QtStudyContentServiceTest" --tests "*DevMemberSeedRunnerTest"`
