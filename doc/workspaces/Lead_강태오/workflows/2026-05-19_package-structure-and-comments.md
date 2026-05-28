# 2026-05-19 패키지 구조 + 파일 주석 작업

## 목표
QT-AI 프로젝트 패키지 구조 개정안(`QT-AI_패키지_구조.pdf`) 명세대로 `qtai-server/` 골격을 세우고, 각 `.java` 파일에 책임을 TODO javadoc으로 남겨 도메인 PR 분할의 출발점을 만든다.

## 범위
- **인프라 파일**: `build.gradle.kts`, `settings.gradle.kts`, `Dockerfile`, `docker-compose.yml`, `apis/api-v1/openapi.yaml`, `data/bible-sources/README.md`
- **진입점**: `QtAiApplication.java`
- **공통**: `common/exception/{BusinessException, ErrorCode}`, `common/dto/ApiResponse`
- **외부 시스템**: `external/{kakao, llm}` 인터페이스/구현 스텁 + DTO record
- **도메인 13개**: `member, bible, qt, study, note, sharing, report, notification, praise, mission, ai, admin, audit`
  - 각 도메인마다 `api/UseCase` + `api/dto` + `internal` + `client/<상대도메인>` + `web/Controller`

## 적용 결정 (PDF 상단 메모 그대로)
1. **`domain/` 래퍼 유지** — 비즈니스 도메인과 인프라(`common/config/security/external/batch`)의 시각적 분리
2. **`external/` 최상위 유지** — Kakao OAuth, LLM 등 외부 시스템을 한곳에 모아 보안 감사·시크릿 관리
3. **`audit` 직접 의존 허용** — 로거형 횡단 관심사로 `client/` 우회 예외 처리

## 단계
1. **PDF 구조 분석** — 도메인 13개, `external/`, `common/`, 인프라 파일 식별
2. **CI 워크플로 정합화** — `ci.yml` → `qt-ai-ci.yml` (파일명만 변경, 위치는 루트 유지)
3. **placeholder 정리** — `flutter-app/.gitkeep`, 팀원 워크스페이스 `workflows/.gitkeep` + `reports/.gitkeep` 5개 폴더
4. **인프라 + 진입점 + common 실내용 작성** — Spring Boot 3.3 + Java 21, `@SpringBootApplication`, `ApiResponse<T>` record 등
5. **`external/` 스텁** — interface 메서드 시그니처 + `UnsupportedOperationException` impl (`@Component` 부여, 도메인 PR에서 채움)
6. **도메인 13개 빈 스켈레톤 일괄 생성** — Bash 스크립트로 145개 `.java` 작성
   - `*UseCase` → `interface`
   - `*Repository` → `interface`
   - `*Request`/`*Response` → `record`
   - `*Status`/`*Visibility` → `enum`
   - 그 외 (`*Service`, `*Mock`, `*Controller`, Entity) → `class`
7. **각 파일에 TODO javadoc 추가** — 동일 스크립트에 `note` 인자로 책임 한 줄 + PDF 명시 코멘트 (예: `GetMemberUseCase` → "타 도메인이 가장 많이 사용", `QtService` → "5개 UseCase 구현", `ShareSnapshot` → "원본 변경과 분리된 스냅샷")
8. **CI Requirements Guard v3.1 보정** — `--exclude-dir=doc` 추가. 가이드 문서가 "이렇게 쓰지 마라"의 컨텍스트로 금지 표현을 인용하면서 차단되던 이슈 해결

## 의존 흐름 (PDF 2번)
같은 패키지든 다른 패키지든 동일 순서: `Controller (web) → UseCase (api, interface) → Service (internal) → Repository (internal)`

타 도메인 호출 시 자기 `client/<상대도메인>/`에 어댑터(또는 Mock)를 두고 인터페이스에만 의존. 상대 도메인이 미구현이면 `XxxUseCaseMock.java`로 임시 처리, 실제 구현 머지 시 `@Primary` Bean이 자연 전환.

**예외 두 가지**
- `audit`은 모든 도메인이 호출하는 횡단 관심사 → 각 Service에서 `WriteAuditLogUseCase`를 직접 주입
- `external/*` (KakaoOAuthClient, LlmClient 등) → `client/` 우회 없이 도메인 Service에서 직접 주입

## 도메인별 의존 요약 (PDF 3번)
```
member          (leaf — 의존 없음, external/kakao 직접 사용)
bible           (leaf — 의존 없음)
qt              → bible, member, ai
study           → bible, member
note            → qt, member
sharing         → qt, note, study, member (스냅샷 생성 시)
report          → sharing, member
notification    → member
praise          → member
mission         → member, qt
ai              → qt (컨텍스트), external/llm 직접 사용
admin           → member, sharing, report 등 다수
audit           (모든 도메인이 호출 — 호출 경로 예외)
```

## 이관 순서 권장 (PDF 4번)
도메인 13개를 한 PR에 모두 옮기면 충돌이 심함. **도메인당 1 PR**, 의존이 적은 leaf부터:

`bible → member → audit → qt → study → note → praise → notification → mission → ai → sharing → report → admin`

## 관련 커밋
| SHA       | 메시지 |
|-----------|--------|
| `3842025` | `chore(infra): flutter-app placeholder 추가 + qt-ai-ci.yml 파일명 정합화` |
| `99ad1de` | `chore: scaffold qtai-server package structure, remove unused folders` |
| `7b73c22` | `구조완성` |
| `97dc57c` | `패키지 구조 수정` |
| `3f4598f` | `fix(ci): Requirements Guard v3.1 — 가이드 문서(doc/) 제외` |
| `a350629` | `각 파일 주석 추가` |

## 게이트 / 검증
- [x] **v3.1 Requirements Guard** 통과 (가이드 문서 `doc/` exclude 보정 후)
- [x] **Gitleaks Secret Scan** 통과
- [x] **OpenAPI Spectral Lint** 통과
- [x] **Docker Compose Config Validation** 통과
- [ ] **qtai-server Build & Test** — 빈 스켈레톤이라 단위 테스트 무의미. 도메인 PR에서 검증 시작


> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.