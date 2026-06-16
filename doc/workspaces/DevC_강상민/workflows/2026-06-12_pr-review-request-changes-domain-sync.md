# Workflow - 2026-06-12 PR 리뷰 REQUEST_CHANGES 도메인 동기화 보강

## 배경

Claude 자동 코드 리뷰(v3.1)에서 QT 자동 수입 기본 상태, Praise create 상태 파싱 테스트, Today QT 캐시 무효화 테스트, AI 산출물 상세 active job 매핑 테스트, 마이그레이션 포함 여부가 지적되었다.

## 작업 범위

1. `service-bible` QT 자동 수입 기본 상태가 00:00/04:00 자동 노출 정책을 깨지 않는지 확인하고 필요 시 보정한다.
2. `service-bible` Praise create 상태 파싱 테스트를 추가한다.
3. `admin-server` Today QT 변경 API의 `todayQt` 캐시 무효화 계약을 테스트로 고정한다.
4. `admin-server` AI 산출물 상세 응답의 active generation job 매핑을 테스트로 고정한다.
5. 마이그레이션 누락 지적은 실제 `origin/dev`와 PR diff 기준으로 확인해 보고서에 기록한다.

## 제외 범위

- PR 전체 분할은 현재 사용자의 한 브랜치 QA 흐름을 유지해야 하므로 코드 변경 범위에서 제외한다.
- `service-bible` Praise 관리자 전용 update/delete API 복사는 제외한다. 기존 코드 주석상 service-bible 동기화 대상은 사용자 create/list/save/listMy이며, 관리자 전용 기능은 admin-server 소유로 유지한다.

## 검증 계획

- `:service-bible:test`의 Praise/QT 관련 테스트 실행
- `:admin-server:test`의 QT/AI 산출물 관련 테스트 실행
- `:service-bible:compileJava`, `:admin-server:compileJava` 실행
