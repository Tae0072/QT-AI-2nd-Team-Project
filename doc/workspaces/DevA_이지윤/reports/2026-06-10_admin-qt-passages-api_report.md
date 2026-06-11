# 2026-06-10 admin-server QT 본문 관리 API 리포트

## 요약

2026-06-10 팀원별 TODO PDF의 이지윤 담당 항목에 따라 `admin-server`에 AD-02 `qt-passages` 관리자 API를 추가했다.

구현 범위는 목록, 등록, 수정, 게시, 숨김이며, 모든 엔드포인트는 `ROLE_ADMIN` 1차 검증 뒤 `admin_users.admin_role=OPERATOR` 또는 `SUPER_ADMIN` 2차 검증을 요구한다. 쓰기 작업은 `audit_logs`에 변경 전후 스냅샷을 남긴다.

## 기준

| 항목 | 내용 |
| --- | --- |
| 기준 브랜치 | `origin/dev-msa` `140427b` |
| 작업 브랜치 | `feature/admin-qt-passages-api` |
| workflow | `doc/workspaces/DevA_이지윤/workflows/2026-06-10_admin-qt-passages-api.md` |
| 관련 F-ID | F-06 / AD-02 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md` |

## 구현 내용

| 구분 | 내용 |
| --- | --- |
| API | `GET /api/v1/admin/qt-passages`, `POST /api/v1/admin/qt-passages`, `PATCH /api/v1/admin/qt-passages/{id}`, `POST /api/v1/admin/qt-passages/{id}/publish`, `POST /api/v1/admin/qt-passages/{id}/hide` |
| 상태 | `active`, `hidden`, `pending_review`, `deletion_notified`, `removed` |
| 스키마 | `qt_passages.status`, `published_at`, `hidden_at` 컬럼 추가 |
| 권한 | `ROLE_ADMIN` + `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR"])` |
| 감사 로그 | `QT_PASSAGE_CREATE`, `QT_PASSAGE_UPDATE`, `QT_PASSAGE_PUBLISH`, `QT_PASSAGE_HIDE` |
| 목록 필터 | `status`, `from`, `to`, `q`, `page`, `size` |
| 방어 로직 | 중복 `qtDate` 차단, 잘못된 ID/범위/필수값 차단, page/size 보정 |

## 김지민님 회신 반영

| 항목 | 반영 |
| --- | --- |
| 엔드포인트 5종 | 그대로 유지 |
| 목록/단건 응답 envelope | 그대로 유지 |
| 에러 envelope와 코드 | 그대로 유지 |
| 페이징 필드 | `content/page/size/totalElements/totalPages/first/last` 유지 |
| 요청 필드 | `bookId/chapter/startVerse/endVerse` 유지, `04_API_명세서.md` AD-02 예시 갱신 |
| 상태값 | 6/9 모더레이션 5종 `active/hidden/pending_review/deletion_notified/removed`로 변경 |

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `doc/workspaces/DevA_이지윤/workflows/2026-06-10_admin-qt-passages-api.md` | PDF 기준 작업 workflow 작성 |
| `qtai-server/admin-server/src/main/resources/db/migration/V31__add_qt_passage_admin_status.sql` | QT 본문 관리자 상태 컬럼 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassage.java` | 상태, 발행/숨김 시각, admin 수정/게시/숨김 메서드 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageStatus.java` | 모더레이션 5종 상태 enum 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java` | 관리자 목록 Specification, 날짜 중복 검증 지원 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java` | AD-02 UseCase 구현과 audit 기록 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/**` | 관리자 QT UseCase 계약 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/dto/**` | 관리자 QT 요청/응답 DTO 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/web/AdminQtPassageController.java` | 관리자 HTTP API 추가 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/internal/AdminQtPassageServiceTest.java` | 등록, 중복 날짜, 게시, 감사 로그 테스트 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/web/AdminQtPassageControllerTest.java` | 목록/등록/수정/게시/숨김 mapping, 권한, validation 테스트 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `./gradlew.bat :admin-server:compileJava` | 통과 |
| `./gradlew.bat :admin-server:test --tests "*AdminQtPassage*"` | 통과 |
| `./gradlew.bat :admin-server:test --tests "*AdminControllerSurfaceTest"` | 통과 |
| `./gradlew.bat :admin-server:build` | 통과 |
| `git diff --check` | 통과. Windows 줄끝 변환 경고만 출력 |

## 추가 점검

| 점검 | 결과 |
| --- | --- |
| 금지 번역본/본문/secret 키워드 스캔 | 기존 성서유니온 수집 코드와 과거 문서의 guardrail 문구가 매칭됨. 이번 구현 파일에는 성경 본문 seed/test/fixture/response를 추가하지 않음 |
| 도메인 import 스캔 | 기존 `domain.qt.internal` -> 같은 도메인 `client.sum` import만 매칭됨. 이번 AD-02 구현에서 다른 도메인 `internal/client/web` 직접 import를 추가하지 않음 |
| 컨트롤러 노출면 | `AdminControllerSurfaceTest`로 `/api/v1/admin/**` 범위 유지 확인 |

## 미실행 검증과 사유

| 명령 | 사유 |
| --- | --- |
| `./gradlew -p qtai-server build` | 이 작업트리의 Gradle wrapper는 `qtai-server/gradlew.bat` 구조라 동일 범위 검증을 `qtai-server` 디렉터리에서 `./gradlew.bat :admin-server:build`로 수행 |
| `./gradlew -p qtai-server test jacocoTestReport` | 변경 범위가 `admin-server` AD-02에 한정되어 admin-server build와 집중 테스트를 우선 수행. 전체 멀티모듈 coverage는 CI 또는 통합 게이트에서 확인 필요 |
| `./gradlew -p qtai-server jacocoTestCoverageVerification` | admin-server 집중 변경이라 로컬에서는 미실행 |
| `npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml` | 이번 변경은 admin-server 구현 중심이며 별도 admin-server OpenAPI 산출물은 아직 없음. 김지민과 응답 계약 확정 후 문서 반영 필요 |
| `gitleaks detect --source . --redact --exit-code 1` | 로컬 실행 여부는 이번 범위에서 확인하지 못함. PR 전 실행 필요 |

## 남은 리스크와 후속 작업

- 김지민과 admin-web 연동용 JSON 필드명, 목록 필터, 등록/수정 request 필드명을 최종 합의해야 한다.
- 김지민님과 협의 결과 `bookId/chapter/startVerse/endVerse` 요청 필드와 공통 페이징 응답은 그대로 연동 가능하다는 답변을 받았다.
- 기존 `qt_passages` seed 데이터는 마이그레이션 기본값 때문에 `pending_review`가 된다. 운영 전 기존 본문 중 사용자 노출 대상은 별도 데이터 보정으로 `active` 전환이 필요하다.
- 최종 MSA 게이트에서 admin-web 8090 base URL과 gateway routing 정합을 함께 검증해야 한다.
