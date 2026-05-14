# 스캐폴딩 안내 — 2026-05-14

문서 레포 `2nd-Team-Project` 의 v2.0(2026-05-14) 결정과 ERD 피드백을 반영한 골격이다.
각 팀원은 자기 워크스페이스의 `workflows/W1_kickoff.md`부터 읽고 시작.

## 현재 구조 (v1.0 MSA 골격 + v2.0 명세 정합)

```
services/
  gateway/          ← 강태오 (JWT, 라우팅, OAuth, JWKS)
  bff-aggregator/   ← 강태오 (UseCase 병렬 호출, STOMP)
  bible-service/    ← 이지윤·이승욱·김지민 (성경, 해설, Journal, Shares)
  ai-service/       ← 강상민·강태오·김태혁 (DeepSeek SSE, Kafka producer)
apps/mobile/        ← 김지민 (Flutter 5 화면)
docs/mockups/       ← UI 정적 목업 (today_qt / journal / ai_chat)
workspaces/         ← 팀원별 작업 폴더 (workflows/ + reports/)
```

> v2.0 Modular Monolith(`qtai-server` 단일 모듈) 전환은 W4(6/8) 이후 ADR-0016 트리거 충족 시 시작.
> 그때까지 도메인 패키지명(`com.qtai.bible`, `com.qtai.ai` 등)은 v2.0 그대로 유지하므로 마이그레이션 부담이 적다.

## v2.0 명세 반영 완료

| 항목 | 반영 위치 |
| --- | --- |
| COMMENTARIES → `bible_explanations` | 엔티티 / Repository / Controller / Flyway V1 |
| `bible_explanations` 범위 컬럼 (`chapter_start ~ verse_end`) | Genesis 41:37-57 등 Tyndale/MHC 원천 대응 |
| `source_type` 2분류 (REFERENCE_SOURCE / GENERATED_EXPLANATION) | REFERENCE는 절대 노출 X, AI 컨텍스트 적재 전용 |
| `bible_today_qt_schedule` 테이블 | 성서유니온 19:00 스크래퍼 적재 자리. **MVP는 하루 1구절** (slot_no 없음) |
| `rag_sources` → `sources` | AI 도메인 / SSE / `ai_turns.sources` JSON 컬럼 |
| AI SSE 경로 `/ai/sessions/{id}/turns` | `/messages` 아님 |
| Journal `(user_id, qt_date)` UNIQUE | 하루 1 QT 정책 유지 |
| RFC 7807 application/problem+json | `ProblemDetailAdvice` |
| Kafka envelope `data` 키 | publisher / consumer 양쪽 강제 |

## 빌드 검증

```bash
# 각 서비스 부팅 (별도 터미널)
./gradlew :gateway:bootRun
./gradlew :bible-service:bootRun
./gradlew :ai-service:bootRun
./gradlew :bff-aggregator:bootRun

# Flutter
cd apps/mobile && flutter pub get && flutter run -d emulator-5554
```

## 다음 회의 안건 (제안)
1. `bible_passage_explanations` 별도 테이블 분리 여부 — 본문 요약/배경/어려운 단어가 commentary와 형식이 달라 한 테이블에 섞으면 컬럼이 비대해진다.
2. 오늘 QT 스크래퍼 owner — 강태오 인프라 안에 둘지 별도 Job 모듈로 뺄지.
3. v2.0 Modular Monolith 분리 시점 — ADR-0016 트리거를 W4에 검증.
