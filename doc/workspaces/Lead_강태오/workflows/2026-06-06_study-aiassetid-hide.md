# 2026-06-06 study aiAssetId 비노출 워크플로우

리뷰 §3 study 단독 처리. 결과: `reports/2026-06-06_study-aiassetid-hide_report.md`

## 0. 전제
- 구현·커밋 완료(`fix/study-hide-internal-aiassetid`, 315d6bf, `origin/dev` 분기). push·PR은 T가 직접.

## 1. 변경 파일(5)
- main: `study/api/dto/QtStudyContentResponse.java`(필드 제거), `study/internal/QtStudyContentService.java`(전달 제거)
- 계약: `apis/api-v1/openapi.yaml`(QtStudyExplanationItem.aiAssetId 제거)
- test: `study/web/QtStudyContentControllerTest.java`(부재 가드), `ai/internal/AiBibleVerseAssetCustomerExposureFlowManualTest.java`(내부 read 모델로 검증 전환)

## 2. 의존/충돌
- 다른 P2 브랜치와 파일 교집합 없음 → 머지 순서 자유.
- 사용자 응답 계약 변경 — PR 본문에 "내부 PK 비노출, 관리자 스키마는 유지" 명시 권장.

## 3. 검증
```bash
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-2nd-Team-Project\qtai-server
Remove-Item build\test-results -Recurse -Force -ErrorAction SilentlyContinue
.\gradlew.bat --no-daemon test
```
- 전체 스위트 0 failures(매뉴얼 통합테스트 포함). OpenAPI는 Windows git diff로 5줄 삭제만 반영 확인.

## 4. 후속
- 남은 P2: study FAILED 의미·scene_script 검증, member 잠금·purge 인덱스, ai 검증 흐름(정밀, 정책), audit 화이트리스트·PII, 문서 drift(ERD/OpenAPI 추가 동기화).
