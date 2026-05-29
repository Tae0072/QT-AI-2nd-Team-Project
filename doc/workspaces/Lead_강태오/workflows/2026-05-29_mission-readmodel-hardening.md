# 워크플로우 — mission 읽기 모델 하드닝 (#141 WARN 후속)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 대상 저장소: QT-AI-2nd-Team-Project (`qtai-server`)
- 선행: PR #141 (mission 진행률 읽기 모델) 머지 후속

## 1. 배경

PR #141 자동 리뷰에서 남은 WARN 2건을 후속 처리한다(BLOCK은 근거 부실로 미수용, 댓글로 반박 후 머지 완료).

1. HIDDEN 미션 정의의 진행률이 대시보드에 노출될 수 있음
2. mission 리포지토리/DDL `@DataJpaTest` 슬라이스 테스트 부재

진행률 배치 계산·대시보드 wiring(3번)은 범위가 커 본 작업에서 제외(별도 후속).

## 2. 작업 절차

1. `dev` 최신화 후 `feature/mission-readmodel-hardening` 브랜치
2. HIDDEN 정책 결정: 명세에 명시 없음 → ERD 전반의 HIDDEN(숨김) 의미와 코드베이스 패턴(`PraiseService.listActive`가 ACTIVE만 노출)에 맞춰 **HIDDEN 정의 진행률은 대시보드에서 제외**. 정의 누락(삭제 등)은 진행 기록 보존을 위해 방어적으로 유지.
3. `MissionService.getMissionProgress`에 HIDDEN 필터 추가 + 단위 테스트 1건
4. `MissionRepositoryTest`(@DataJpaTest) 추가 — 두 테이블 UNIQUE/파생쿼리/precision 검증
5. 전체 회귀 통과 후 PR

## 3. 검증

```powershell
cd qtai-server
.\gradlew.bat --stop ; Remove-Item build\classes,build\test-results,build\tmp -Recurse -Force
.\gradlew.bat test --no-daemon --console=plain
```
