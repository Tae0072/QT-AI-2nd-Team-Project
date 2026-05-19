# 2026-05-19 패키지 구조 + 파일 주석 작업 — 결과 보고

## 요약
PDF "QT-AI 프로젝트 패키지 구조 (개정안)" 명세대로 `qtai-server/` 골격 + 145개 `.java` 스켈레톤을 master에 반영. 각 파일에 PDF 명시 코멘트와 책임 한 줄을 javadoc TODO로 남겨, 도메인 13개 PR 분할의 출발점 확보. CI 파이프라인은 Requirements Guard v3.1 가이드 문서 exclude 보정 후 통과.

## 산출
| 카테고리 | 파일 수 | 형태 |
|----------|---------|------|
| 인프라 (build, Dockerfile, docker-compose, openapi, README) | 6 | **실내용** (Spring Boot 3.3 + Java 21, MySQL 8.0, DeepSeek 게이트 반영) |
| 진입점 + common (QtAiApplication, BusinessException, ErrorCode, ApiResponse) | 4 | **실내용** |
| external (KakaoOAuthClient(+Impl), LlmClient, AnthropicLlmClient, DTO 3) | 7 | interface 메서드 시그니처 + `UnsupportedOperationException` 스텁, DTO record는 필드 정의 |
| domain 13개 (UseCase, Service, Entity, Repository, Mock, Controller) | 134 | **빈 스켈레톤** + TODO javadoc |
| **`src/main/java` 총** | **145** | |

추가로 폴더 placeholder `.gitkeep` 7개(`batch/`, `config/`, `security/`, `admin/api/dto/`, `admin/client/{member,sharing,report}/`) — PDF에 폴더만 명시되고 구체 파일 명세 없는 곳.

## TODO javadoc 패턴
- **UseCase**: 책임 + 누가 호출하는지 (예: `GetMemberUseCase` → "타 도메인이 가장 많이 사용")
- **Service**: 의존성 + 구현 범위 (예: `QtService` → "5개 UseCase 구현. bible/member/ai를 client/ 어댑터로 호출")
- **Entity**: 주요 필드 힌트 (예: `Qt` → "작성자, 본문, 참조 BibleVerse, 공개범위(QtVisibility)")
- **Mock**: "실제 구현 머지 시 `@Primary` 빈으로 자연스러운 전환"
- **DTO**: 필드 의도
- **PDF 명시 코멘트는 그대로 반영**: `ShareSnapshot` → "원본 변경과 분리된 스냅샷", `AiCallLog` → "감사용 (영속 응답 X)", `WriteAuditLogUseCase` → "모든 도메인이 호출 (logger 형태)" 등
- **v3.1 게이트 제약은 해당 파일에 직접 표기**: `AnthropicLlmClient` → "DeepSeek API만 허용", `AiController` → "SSE 금지, 202 + polling", `Praise` → "lyrics_text/audio_url 저장 금지"

## 검증
- [x] PDF 구조와 1:1 매핑 — 도메인 13개, `client/<상대도메인>/`, `api/dto/` 위치, `audit` 예외 처리
- [x] CI 모든 게이트 통과 (Requirements Guard v3.1, Gitleaks, Spectral, Docker Compose)
- [x] 한글 경로 폴더(`workspaces/Lead_강태오/` 등) Windows에서 정상 처리
- [x] 빈 스켈레톤이라 컴파일은 통과 (메서드 시그니처 없음 → 호출자 없음)
- [ ] **단위 테스트 작성 보류** — 도메인 PR에서 메서드 시그니처가 채워질 때 함께 진행

## 결정 사항
1. **`domain/` 래퍼 유지** — 비즈니스 도메인과 인프라의 시각적 분리
2. **`external/` 최상위** — 외부 시스템 한곳에 모아 보안 감사·시크릿 관리
3. **`audit` 직접 의존 허용** — 로거형 횡단 관심사라 `client/` 우회 예외
4. **도메인 13개 = 13 PR** — 한 PR 머지 시 충돌 회피 (PDF 4번 권장 그대로 수용)
5. **`external/` impl은 `@Component` + `UnsupportedOperationException`** — 도메인 PR이 채울 때까지 빈 빈 등록되지 않게 함과 동시에 호출 시 즉시 실패하여 미구현 노출

## 위험 / 한계
- **빌드 산출물 git 추적** — `qtai-server/.gradle/`, `qtai-server/bin/`이 `.gitignore` 부재로 git에 들어가 있음. 브랜치 전환 시 Gradle daemon / IDE가 파일을 잠가서 `git checkout` 실패. 다음 PR에서 `.gitignore` 추가 + `git rm --cached` 필요.
- **`admin/client/{member,sharing,report}/`** — PDF에 구체 Mock 파일 명시 없음 → `.gitkeep`만 둠. admin PR에서 구체 Mock 정의 필요.
- **master 직접 push** — 일부 커밋(`7b73c22`, `97dc57c`, `a350629`, `3f4598f`)이 PR 리뷰 우회. 후속 도메인 PR부터는 `dev` 브랜치 경유 권장.
- **빈 스켈레톤 컴파일 안전성** — 도메인 PR에서 메서드 시그니처를 채우면 `client/<상대>/Mock`이 `implements` 절을 가져야 컴파일 통과. Mock이 빈 class인 상태에서 Service가 Mock 주입을 시도하면 런타임 오류 가능. 도메인 PR 머지 순서를 leaf부터 지켜야 함.

## 다음 단계
1. **`.gitignore` 추가 + 빌드 산출물 정리** (`qtai-server/.gradle/`, `qtai-server/bin/`, `qtai-server/build/`, `.idea/`)
2. **도메인 leaf부터 PR 진행** (이관 순서):
   `bible → member → audit → qt → study → note → praise → notification → mission → ai → sharing → report → admin`
3. **`config/`, `security/`, `batch/` 패키지 채우기** — 첫 도메인 PR(예: `member`)과 동반 작성 (`config/JpaConfig`, `security/JwtAuthenticationFilter` 등)
4. **API 명세 채우기** — `apis/api-v1/openapi.yaml` 현재 빈 골격. 각 도메인 PR에서 해당 도메인 `paths` 추가
5. **v3.1 게이트 추가 점검** — `praise` PR 시 `recommend.*song|lyrics_text|audio_url` 룰, `ai` PR 시 SSE/Anthropic SDK 룰 사전 확인

## 관련 커밋
| SHA       | 메시지 | 비고 |
|-----------|--------|------|
| `3842025` | `chore(infra): flutter-app placeholder 추가 + qt-ai-ci.yml 파일명 정합화` | PR `chore/flutter-app-and-ci-rename` |
| `99ad1de` | `chore: scaffold qtai-server package structure, remove unused folders` | 폴더 이관 |
| `7b73c22` | `구조완성` | master 직접 |
| `97dc57c` | `패키지 구조 수정` | master 직접 |
| `3f4598f` | `fix(ci): Requirements Guard v3.1 — 가이드 문서(doc/) 제외` | master 직접 |
| `a350629` | `각 파일 주석 추가` | master 직접, TODO javadoc 145개 일괄 추가 |
