# 2026-06-08 feature/20260604 dev 최신화 + 리뷰 차단 대응 리포트

## 요약

`feature/20260604` 브랜치를 최신 `origin/dev` `bf69da4` 기준으로 병합하고, PR 리뷰에서 차단 가능성이 있는 항목을 보수적으로 정리했다.

별도 담당자 협의가 필요한 새 정책 결정은 없었다. 적용 내용은 모두 기존 컨벤션의 보수적 적용이다.

## dev 최신화

| 항목 | 내용 |
| --- | --- |
| 기준 dev | `origin/dev` `bf69da4` |
| 병합 상태 | Flutter 핵심 파일 5개 conflict 해소 후 Lead 문서 정리 커밋 추가 병합 |
| 주요 충돌 파일 | `api_client.dart`, `bible_models.dart`, `bible_browser_screen.dart`, `today_qt_screen.dart`, `bible_repository.dart` |

## 리뷰 대응

| 리뷰 항목 | 처리 |
| --- | --- |
| `X-Dev-User-Id` 강제 부착 위험 | `AppConfig.instance.isDev` 조건 제거. `webDevNoLogin`이 참일 때만 dev header 부착 |
| 실제 성경 본문 fixture 금지 | `형제들아...`, `태초에...`, KJV 문장을 더미 문장으로 교체 |
| Today QT 버튼 정책 | 해설은 `hasExplanation`, 시뮬레이터는 `simulatorStatus == READY`, 노트는 `qtPassageId` 존재 조건으로 활성화 |
| 성경 브라우저 fallback 50 위험 | 장별 절 수 로딩 실패 시 임의 50절 조회 대신 조회 비활성화 및 오류 안내 |
| QT study-content 모델 충돌 | dev의 `simulatorStatus`, `draftNoteId`를 유지하면서 PR의 `glossaryTerms` DTO 보존 |
| 오늘 QT fallback 본문 위험 | range 없음 상태에서 하드코딩 본문을 보여주지 않고 오류 처리 유지 |

## 협의 판단

협의 필요 없음.

- dev header는 이미 dev 브랜치에 들어온 `webDevNoLogin` 정책을 따른 것이므로 보안 정책 변경이 아니다.
- 테스트 fixture 더미화는 금지 데이터 제거라 담당자 의사결정이 필요하지 않다.
- 버튼 활성 조건은 요구사항의 `READY`/승인 콘텐츠 노출 정책을 그대로 적용한 것이다.
- 성경 브라우저 fallback 제거는 없는 절 조회를 막는 방어 로직이다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `flutter-app/lib/core/network/api_client.dart` | dev header 부착 조건을 `webDevNoLogin`으로 제한 |
| `flutter-app/lib/features/bible/models/bible_models.dart` | Today QT dev 필드와 study-content glossary DTO 병합 |
| `flutter-app/lib/features/bible/screens/bible_browser_screen.dart` | 장별 절 수 로딩 실패 시 조회 차단 |
| `flutter-app/lib/features/bible/screens/today_qt_screen.dart` | 해설/시뮬레이터/노트 버튼 조건 및 route 연결 병합 |
| `flutter-app/lib/features/bible/services/bible_repository.dart` | Today QT fallback 제거 유지, study-content 조회 주석 정리 |
| `flutter-app/test/core/network/api_client_test.dart` | dev 기본 실행에서 header 미부착 기대값 반영 |
| `flutter-app/test/features/bible/screens/today_qt_screen_test.dart` | 실제 본문 fixture 제거, 해설 활성 조건 반영 |
| `flutter-app/test/features/note/screens/qt_note_editor_screen_test.dart` | 실제 본문 fixture 제거 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `flutter analyze` | 통과 |
| `flutter test test/core/network/api_client_test.dart test/features/bible test/features/note/screens/qt_note_editor_screen_test.dart test/routes/app_router_test.dart` | 통과 |
| `./gradlew.bat test --tests "*GlossaryTermServiceTest" --tests "*GlossaryTermRepositoryTest" --tests "*QtStudyContentServiceTest" --tests "*DevMemberSeedRunnerTest"` | 통과 |
| `./gradlew.bat build` (`qtai-server`) | 통과 |
| `git diff --check` / `git diff --cached --check` | 통과 |

## 남은 리스크

- `origin/dev`가 admin-web/music/security 등 넓은 범위를 포함하므로 CI에서 전체 프로젝트 조합 검증이 필요하다.
- `gitleaks` 로컬 명령이 없어 직접 실행하지 못했다.
- `.spectral.yaml`이 현재 워크스페이스에 없어 OpenAPI spectral lint는 직접 실행하지 못했다.
- coverage KPI는 GitHub Actions에서 최종 확인한다.
