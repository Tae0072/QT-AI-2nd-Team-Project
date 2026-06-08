# 2026-06-08 MSA Phase 0 — 멀티모듈/lib-common — 결과 보고

## 요약
`qtai-server`를 Gradle 멀티모듈로 전환하고 서비스 공통 코드를 `lib-common` 모듈로 분리했다. 동작 무변경 구조 리팩토링이며 MSA 분리의 0단계 토대다. 본 PR은 코드 8파일 + 문서(설계서·workflow·report)를 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/settings.gradle.kts` | `:lib-common` 모듈 등록 |
| `qtai-server/build.gradle.kts` | `implementation(project(":lib-common"))` 의존 추가 |
| `qtai-server/lib-common/build.gradle.kts` | (신규) 공통 모듈 — Spring(web/data-jpa/validation/security) + lombok, Boot BOM 관리 |
| `lib-common/.../common/dto/ApiResponse.java` | 이전(rename) |
| `lib-common/.../common/entity/BaseEntity.java` | 이전(rename) |
| `lib-common/.../common/exception/{ErrorCode,BusinessException,GlobalExceptionHandler}.java` | 이전(rename) |
| `doc/.../MSA분리계획_2026-06-08.md` | 설계 문서(v2) |

## 변경 성격
- **순수 구조 리팩토링**: 패키지명 `com.qtai.common.*` 유지 → 이를 import하는 **102개 파일 무수정**, git이 5개 전부 **100% rename**으로 인식.
- 최신 `dev` 머지(거버넌스 #341 MSA 허용 포함, 충돌 없음).

## 검증
- `gradlew assemble` — **BUILD SUCCESSFUL** (머지 전/후 모두). lib-common jar + app bootJar 정상.
- 단위/통합 테스트는 후속 증분에서 모듈 경계 ArchUnit 규칙과 함께 보강.

## 미해결
- PR 머지 대기
- 후속: JWT 유틸 이전 · service-gateway · ArchUnit 모듈 경계 · bible 추출
