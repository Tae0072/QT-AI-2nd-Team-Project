# CLAUDE.md — QT-AI Claude Code 가이드

Claude Code도 이 저장소에서는 `AGENTS.md`와 `DECISIONS.md`를 기준으로 작업한다.

## 필수 순서

1. `DECISIONS.md`를 먼저 읽는다.
2. `AGENTS.md`의 금지 패턴과 서비스 경계를 따른다.
3. 문서 레포 `https://github.com/Tae0072/2nd-Team-Project/blob/master/`의 OpenAPI/ERD/Kafka schema를 작업 서비스에 맞게 확인한다.

## 현재 기준

- 독립 `services/auth-service/`는 사용하지 않는다. 인증은 `services/gateway/`의 Gateway Auth 모듈에서 처리한다.
- 독립 `services/journal-service/`는 사용하지 않는다. 묵상일지(Journal)는 `services/bible-service/` 안에서 구현한다.
- AI LLM은 DeepSeek API다. Anthropic SDK, Claude 고정 코드, `ANTHROPIC_API_KEY`를 만들지 않는다.
- AI SSE 경로는 `/ai/sessions/{id}/turns`다. `/messages` 경로를 만들지 않는다.

세부 규칙은 중복 관리하지 않고 `AGENTS.md`에 둔다. 충돌 시 `DECISIONS.md`가 우선이다.
