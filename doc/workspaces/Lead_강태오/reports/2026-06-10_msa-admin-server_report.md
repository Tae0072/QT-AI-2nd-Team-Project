# 작업 리포트 — MSA Day3 admin-server 분리

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-ai` (브랜치 feature/msa-admin-server, dev-msa 기준)
- **작업자**: 강태오(Lead, AI 보조)
- **근거**: 회의록 2026-06-09 §6, 2026-06-10 Lead 결정("통째 복사, 강사님 방식 그대로")

## 1. 배경
Day3 트랙 중 admin-server. 사용자용 4서비스(user/bible/note/ai)는 dev-msa 머지 완료. 관리자는 트래픽이 적고 1인 사용 → MSA 불필요 → 모놀리식 어드민 서버로 분리(회의록 §6).

## 2. 방식 (Lead 결정)
모놀리식 원본을 **통째 복사**한 뒤 admin 컨트롤러만 남기는 강사님 방식. lib-common 슬라이스 방식은 채택하지 않음.

## 3. 수행 내용
- `qtai-server/src/main`(504 java + db/seed/profiles 리소스) 전체를 `admin-server` 모듈로 robocopy. lib-common 비의존 standalone(자체 com.qtai.common). 단일 DB 공유, port 8090.
- build.gradle.kts: 모놀리식 deps 미러(web/jpa/security/validation/redis/cache/caffeine/pdfbox/flyway/jjwt/lombok/h2/mysql). settings.gradle `include("admin-server")`.
- **컨트롤러 정리**: 사용자용 18 + system 4 = 22개 `*Controller.java` 삭제. `/api/v1/admin/**` 7개만 잔류(AdminController, AdminAiAsset/BatchRunLog/Monitoring/ValidationChecklist, AdminAuditLog, AdminReport). 서비스·엔티티·리포지토리·로직은 그대로(§6 "폴더·로직 그대로, 컨트롤러만 정리").
- 테스트 하니스: 모놀리식 `application-test.yml`(H2·MySQL모드·flyway off·ddl create-drop) + `JwtTestKeysContextCustomizerFactory` + `META-INF/spring.factories` 차용(테스트 스위트 본체는 미복사).

## 4. 검증 결과
- `:admin-server:build` BUILD SUCCESSFUL. 부팅 스모크에서 전 도메인 엔티티가 H2에 생성·컨텍스트 로드 후 정상 종료(단일 DB 공유 구조 동작 확인).
- 테스트 3종: `AdminServerApplicationTest`(부팅), `AdminControllerSurfaceTest`(노출 컨트롤러 전부 /api/v1/admin — 트림 회귀 방지), `AdminServerSecurityTest`(미인증 admin 경로 401/403).

## 5. 설계 판단·이슈
- **테스트 스위트 미복사**: 모놀리식 테스트를 통째 복사하면 삭제된 컨트롤러의 테스트가 깨지므로, 부팅·노출면·보안 최소 테스트만 신규 작성.
- **컨트롤러만 삭제**(서비스/리포 잔류) → 컴파일·부팅 무손상. 삭제로 인한 참조 단절 없음 확인.
- **대형 PR**(565파일): 모놀리식 복제 특성. 강사님 방식(통째 복사)에 따른 의도된 중복이며, 추출 완료 후 모놀리식 root 제거 시 정리 예정(Strangler).

## 6. 다음 단계
- 컨트롤러 없는 잔류 사용자/system 서비스 코드 정리·운영 application.yml(8090, 프로파일) 정비 = 통합 단계.
- RestClient 통합: 각 서비스의 audit/admin Mock → admin-server 호출(또는 공통 audit). 로컬 k8s 배포(가드 완화 PR #432 머지 후) 시 admin-server 포함 5서비스 컨테이너화.
