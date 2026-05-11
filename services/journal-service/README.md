# Journal Service — 이승욱
#
# 역할: 묵상 노트 서비스 (Spring Boot 3.3 / Java 21)
# 담당: 이벤트 소싱, Kafka 컨슈머, @Lock PESSIMISTIC_WRITE
# 포트(로컬): 8084
#
# 참조 명세: https://github.com/Tae0072/2nd-Team-Project/blob/main/apis/journal/openapi.yaml
#
# ⚠️ JOURNAL_EVENTS 테이블은 append-only — 수정/삭제 코드 생성 금지
# ⚠️ Kafka 컨슈머에 idempotencyKey 검증 필수
# ⚠️ POST /api/v1/journals 없음 — ai.session.completed 컨슈머로 자동 생성
