# 2026-06-04 성경 66권 전체 본문 seed 적재

## 목표
`bible_verses` 테이블에 성경 66권 전체 본문(31,104절)을 적재하여 오늘 QT 본문 표시를 가능하게 한다.

## 작업 내용
1. **KorRV.json + KJV.json 다운로드** — GitHub `2nd-Team-Project/성경/` 에서 가져옴
2. **generate_seed_sql.py** — JSON → SQL 변환 스크립트 작성
3. **V23__seed_bible_verses.sql** — 66권 31,104절 INSERT (korean_text=KorRV, english_text=KJV)
4. **저작권 확인** — KorRV(개역한글, 퍼블릭 도메인), KJV(퍼블릭 도메인). 금지 번역본(개역개정/ESV/NIV) 미포함

## 범위
- 브랜치: `feature/bible-verse-seed`
- 변경: 2파일 (V23 SQL + 변환 스크립트)
- 관련: F-01 성경·QT 본문 화면

## 미해결
- PR 머지 대기

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
