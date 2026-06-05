# 2026-06-04 성경 전체 본문 seed 적재 — 결과 보고

## 요약
성경 66권 31,104절 전체 본문을 `bible_verses` 테이블에 Flyway seed(V23)로 적재. KorRV(개역한글) + KJV(킹제임스), 모두 퍼블릭 도메인.

## 산출물

| 파일 | 설명 |
|------|------|
| `V23__seed_bible_verses.sql` | 66권 31,104절 INSERT (korean_text + english_text) |
| `generate_seed_sql.py` | KorRV.json + KJV.json → SQL 변환 스크립트 |

## 저작권 근거

| 번역본 | 정식 명칭 | 저작권 상태 | 출처 |
|--------|-----------|------------|------|
| KorRV | 개역한글 (Korean Revised Version) | 퍼블릭 도메인 — 1961년 발행, 대한성서공회 저작권 만료 | GitHub `Tae0072/2nd-Team-Project/성경/KorRV.json` |
| KJV | King James Version | 퍼블릭 도메인 — 1611년 발행, 저작권 없음 | GitHub `Tae0072/2nd-Team-Project/성경/KJV.json` |

- **개역개정(개역개정판, NKRV)과 다름**: KorRV는 1961년 개역한글이며, 1998년 개역개정과는 별개 번역본
- CLAUDE.md §8 금지 데이터: 개역개정/ESV/NIV/성서유니온/두란노 본문 미포함 확인

## 정량 검증

| 항목 | 값 |
|------|-----|
| 총 절 수 | 31,104절 |
| 구약 권 수 | 39권 (book_id 1~39) |
| 신약 권 수 | 27권 (book_id 40~66) |
| korean_text 비어있는 절 | 0건 (전체 NOT NULL) |
| english_text 비어있는 절 | 0건 |
| SQL INSERT 문 수 | 1,189건 (장 단위) |
| SQL 파일 크기 | 약 4.5MB |
| `gradlew bootRun` | 정상 기동 (Flyway V23 적용) |

## F-01 노출 경로 · §6 본문 저장 정책 확인

- **노출 경로**: `GET /api/v1/bible/verses?bookCode=xxx&chapter=x&verseFrom=x&verseTo=x` → `bible_verses` 테이블에서 조회 → Flutter 앱 QT 화면 표시
- **§6 본문 저장 정책**: CLAUDE.md §8 "성서유니온 또는 두란노 본문 텍스트 저장 금지" — 본 seed는 KorRV/KJV 퍼블릭 도메인만 사용하여 정책 충돌 없음
- **저작권 표현**: "저작권 문제 없음" 대신 "저작권 리스크를 낮춘다" 사용 (CLAUDE.md §8 표현 규칙)

## 검증
- 서버 `bootRun` 정상 기동 (Flyway V23 적용)
- 금지 데이터 미포함 확인 — 개역개정/ESV/NIV/성서유니온/두란노 없음

## 미해결
- PR 머지 대기
