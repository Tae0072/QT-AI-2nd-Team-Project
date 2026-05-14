# CLAUDE.md — QT-AI Claude Code 가이드

Claude Code도 이 저장소에서는 `AGENTS.md`와 `DECISIONS.md`를 기준으로 작업한다.

## 필수 순서

1. `DECISIONS.md`를 먼저 읽는다.
2. `AGENTS.md`의 금지 패턴과 서비스 경계를 따른다.
3. 문서 레포 `https://github.com/Tae0072/2nd-Team-Project/blob/master/`의 OpenAPI/ERD/Kafka schema를 작업 서비스에 맞게 확인한다.

## 현재 기준

- 독립 `services/auth-service/`는 사용하지 않는다. 인증은 `services/gateway/`의 Gateway Auth 모듈에서 처리한다.
- 독립 `services/journal-service/`는 사용하지 않는다. 묵상일지(Journal)는 `services/bible-service/` 안에서 구현한다.
- 오늘 QT는 **하루 1개 본문(범위 허용)** 이며 — 한 절·한 단락·다중 장 모두 가능 (DECISIONS.md §3.1, 02_ERD v2.3, ADR-0021). DB/API는 `chapter_start`·`verse_start`·`chapter_end`·`verse_end` + `start_ordinal`/`end_ordinal`을 사용한다. AI 질문과 Journal 생성은 오늘 QT 본문에서만 가능하다.
- Journal DRAFT는 `POST /api/v1/journals/today`로 생성/조회한다. 자유 본문 `POST /api/v1/journals`는 만들지 않는다.
- `ai.session.completed`는 새 Journal 생성이 아니라 오늘 Journal에 AI 요약과 `aiSessionId`를 첨부한다.
- 앱 첫 화면은 별도 홈이 아니라 오늘 QT 화면이다. 교회 인증은 MVP gate로 만들지 않는다.
- AI LLM은 DeepSeek API다. Anthropic SDK, Claude 고정 코드, `ANTHROPIC_API_KEY`를 만들지 않는다.
- AI SSE 경로는 `/ai/sessions/{id}/turns`다. `/messages` 경로를 만들지 않는다.

세부 규칙은 중복 관리하지 않고 `AGENTS.md`에 둔다. 충돌 시 `DECISIONS.md`가 우선이다.
