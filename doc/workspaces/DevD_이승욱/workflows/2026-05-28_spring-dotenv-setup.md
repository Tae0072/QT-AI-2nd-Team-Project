# 2026-05-28 spring-dotenv 로컬 개발 환경 설정

## 목표
팀원이 환경변수를 수동으로 설정하지 않고도 `.env` 파일 하나로 JWT 키, DB 접속 정보 등을 자동 로딩할 수 있도록 spring-dotenv 의존성을 추가한다.

## 작업 내용
1. **spring-dotenv 의존성 추가** — `build.gradle.kts`에 `me.paulschwarz:spring-dotenv:4.0.0`을 `developmentOnly`로 추가. 운영 런타임에는 포함되지 않음
2. **.gitignore .env 등록** — `.env` 파일이 커밋되지 않도록 `.gitignore`에 패턴 추가
3. **PR 리뷰 반영** — 1차 리뷰에서 `implementation` → `developmentOnly` 스코프 변경 요청 수용. 운영 환경에서 `.env` 자동 로딩이 활성화되는 보안 리스크 제거

## 범위
- 브랜치: `chore/spring-dotenv-setup`
- PR: (Merged)
- 커밋: `chore(server): spring-dotenv 의존성 추가 + .gitignore .env 등록`
- 변경: 2파일 (`build.gradle.kts`, `.gitignore`)

## 미해결
- `.env.example` 파일 추가는 후속 PR에서 처리 가능

## 담당
- DevD 이승욱
