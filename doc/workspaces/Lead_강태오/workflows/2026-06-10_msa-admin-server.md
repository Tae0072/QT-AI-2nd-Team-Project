# 2026-06-10 MSA Day3 — admin-server 분리 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-ai` (브랜치 `feature/msa-admin-server`, dev-msa 기준)
> 근거: 회의록 2026-06-09 §6 "관리자는 모놀리식 원본 복사 → admin 컨트롤러만 남기고 나머지 삭제". 2026-06-10 Lead 결정으로 "강사님 방식 그대로(통째 복사)" 확정.

## 방식 결정 (Lead)
- **모놀리식 통째 복사**: `qtai-server/src/main`(504 java + resources/db/seed) 전체를 `admin-server` 모듈로 복사. lib-common 슬라이스 방식이 아니라 강사님 방식(통째 복사 후 컨트롤러 정리)을 따른다.
- lib-common **비의존 standalone**(자체 `com.qtai.common` 보유) — 단일 DB 공유, 사용자용 4서비스와 분리. port 8090.

## TODO
- [x] 모놀리식 main 통째 복사 + 모듈 등록(build.gradle 미러, settings include "admin-server").
- [x] admin 외 컨트롤러 정리 — 사용자용 18 + system 4 = 22개 `*Controller.java` 삭제, `/api/v1/admin/**` 7개만 잔류(AdminController, AdminAiAsset/BatchRunLog/Monitoring/ValidationChecklist, AdminAuditLog, AdminReport). 폴더·서비스·로직은 그대로(회의록 §6 "컨트롤러만 정리").
- [x] 부팅·검증 — monolith 테스트 하니스(application-test.yml: H2·flyway off·create-drop, JwtTestKeysContextCustomizerFactory, spring.factories) 차용. 테스트 3종: 부팅 스모크 / 컨트롤러 노출면(전부 /api/v1/admin) / 미인증 거부. `:admin-server:build` 통과.
- [ ] 문서 + PR — feature/msa-admin-server → dev-msa. 첫푸시 APPROVE 체크리스트 적용, 자동머지.

## 핵심 원칙
- 비-admin 컨트롤러만 삭제(서비스/엔티티/리포지토리는 잔류) → 컴파일·부팅 안정. 삭제로 깨지는 참조 없음 확인.
- 테스트 스위트는 통째 복사하지 않음(삭제된 컨트롤러 테스트가 깨지므로). 부팅·노출면·보안 최소 테스트만 신규 작성.
- 첫푸시 APPROVE: 푸시 전 dev-msa 병합, 금지토큰 무관(복사본은 기존 통과 코드), 대형 PR이나 "강사님 방식 통째 복사"로 문서화.

## 진행 메모
- 복사 504파일 - 삭제 22컨트롤러 + 모듈/테스트 = 565파일 PR(대형, 모놀리식 복제 특성).
- 후속: admin 외 사용자/system 서비스 코드(컨트롤러 없는 잔류분) 정리·운영 application.yml(8090) 정비는 통합 단계(Day3 후속)에서. RestClient 통합 시 admin-server가 audit/admin 호스트 역할.
