# 작업 리포트 — MSA Day 2 (사용자/노트/AI 서비스 추출)

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-day2`
- **작업자**: 강태오(Lead, AI 보조)
- **관련**: `2026-06-10_msa-day2-services.md`(워크플로우), `2026-06-09_msa-restart-plan.md`(계획)

## 1. 배경
PR#1으로 멀티모듈 골격 + lib-common + service-bible(bible·music·praise)이 dev-msa에 머지됐다. Day 2는 사용자 4서비스 중 나머지 3개(service-user / service-note / service-ai)를 추출한다. PR#2(qt·study)는 별도 세션에서 병행.

## 2. 수행 내용
(각 단계 완료 시 갱신)

- **Day2-1 service-user 스켈레톤**: ✅ 완료 (`a82724b`). settings include + boot app(web+jpa) + @SpringBootTest contextLoads 통과(14s). 운영 ddl-auto=validate, 테스트 create-drop override.
- **Day2-2 member 이전**: _대기_
- **Day2-3 notification·mission + PR**: _대기_
- **Day2-4 service-note**: _대기_
- **Day2-5 service-ai (+Kafka)**: _대기_

## 3. 검증 결과
(빌드/테스트 결과 누적 기록)

## 4. 이슈 및 대응
(발생 시 기록 — 예: 빌드 폴더 잠금, cross-domain Mock 누락 등)

## 5. 다음 단계
service-user 추출 후 service-note → service-ai 순. 각 서비스는 첫 푸시 APPROVE 품질로 별도 PR(base dev-msa).
