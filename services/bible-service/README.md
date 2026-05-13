# Bible Service — 성경/QT/Journal

Owner: 이지윤·이승욱

## Scope

- 성경 본문, 주석, 쉬운 본문 설명 조회
- 오늘 QT 본문 1구절 제공 (`verseStart == verseEnd`)
- 오늘 QT Journal DRAFT 생성/조회: `POST /api/v1/journals/today`
- Journal 4필드 자동 저장, 발행, 공유, 좋아요, 댓글, 신고
- `ai.session.completed` consume 후 오늘 Journal에 AI 요약과 `aiSessionId` 첨부

## Guardrails

- 자유 본문 `POST /api/v1/journals` 생성 금지
- 오늘 QT가 아닌 본문에서 Journal 생성 금지
- `JOURNAL_EVENTS`는 append-only
- 허용 데이터: KJV, 개역한글, Matthew Henry
- 금지 데이터: 개역개정, ESV, NIV
