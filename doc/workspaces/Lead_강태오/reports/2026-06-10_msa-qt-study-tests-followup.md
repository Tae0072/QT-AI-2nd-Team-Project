# 리포트 — qt·study 쓰기/파싱 서비스 단위테스트 보강 (PR #424 후속)

작성일: 2026-06-10 / 브랜치: `feature/msa-qt-study-tests` (origin/dev-msa `7885bb7` 기준)

## 1. 배경

PR #424(qt·study 이전)는 APPROVE로 머지됐으나, 자동리뷰 종합 의견에서 **핵심 쓰기/파싱 경로
서비스 단위테스트 누락**을 비차단 WARN("다음 PR에서 반드시 보강 필요")으로 남겼다. 게시·숨김
멱등성, 동시 게시 leak, JSON 검증, 파서 정상/예외 분기가 대상이다. 이 후속 PR이 그 공백을 메운다.

## 2. 변경 내역 (테스트 전용, 운영 코드 변경 없음)

모놀리식에 이미 있던 동일 서비스 테스트를 service-bible로 이전했다. 대상 서비스가 같은 패키지
(`com.qtai.domain.*.internal`)로 이전돼 있어 그대로 적응 가능했다.

- `support/TestEntityFactory.java` — 리플렉션 기반 엔티티 빌더(bible/study internal 엔티티만 참조, 자기완결)
- `domain/qt/client/sum/SuTodayPassageParserTest.java` (2) — 성서유니온 응답 파싱 정상/거부
- `domain/study/internal/SimulatorClipPublishServiceTest.java` (9) — scene JSON 검증·게시 게이팅·
  과대 페이로드(200,000자 상한)·숨김 멱등(hiddenCount)
- `domain/study/internal/VerseExplanationServiceTest.java` (6) — 해설 게시/숨김 멱등·비관락 leak 방지
- `domain/study/internal/GlossaryTermServiceTest.java` (14) — 용어 게시/숨김·중복 검증·멱등 분기

신규 단위테스트 **31개** 추가.

## 3. 검증

호스트 gradlew `:service-bible:build` GREEN. 추가 테스트 31개 전부 통과(0 실패/에러):
SuTodayPassageParserTest 2 · SimulatorClipPublishServiceTest 9 · VerseExplanationServiceTest 6 ·
GlossaryTermServiceTest 14.

## 4. 비고

- 파일 복사는 PR #424에서 겪은 샌드박스 마운트 truncation을 피해 **호스트 Copy-Item**으로 수행.
- 빈 stub 일괄 정리·BibleBookLookup 캐싱·SU 재시도/서킷브레이커는 운영 코드 변경이라 별도 후속
  과제로 남긴다(리뷰어도 후속 권장).
