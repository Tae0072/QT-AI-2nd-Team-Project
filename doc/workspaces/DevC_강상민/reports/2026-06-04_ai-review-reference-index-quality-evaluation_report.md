# Report - 2026-06-04 ai-review-reference-index-quality-evaluation

| 항목 | 내용 |
| --- | --- |
| 브랜치 | `docs/ai-reference-index-quality-evaluation` |
| PR 대상 | `dev` |
| workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-04_ai-review-reference-index-quality-evaluation.md` |
| 실행 방식 | `workflow-spec-runner` 기준 직접 실행 |
| 평가 대상 | `qtai-server/build/ai-review-reference/reference-index.json` |
| 관련 F-ID | 해당 없음 |

## 평가 요약

현재 로컬 index는 품질 평가 및 layer 2 연결 테스트용으로 사용할 수 있다. 운영 최종 확정 전에는 권별 앞/중간/뒤 샘플 육안 검수를 추가로 수행하는 것을 권장한다.

평가 report에는 장문 원문 값과 긴 인용문을 수록하지 않고, 정량 지표와 분포만 기록한다. 평가 대상 JSON과 PDF 원본은 Git에 포함하지 않는다.

## 핵심 지표

| 지표 | 값 |
| --- | ---: |
| schemaVersion | `ai-review-reference-index.v1` |
| entry count | 3021 |
| book count | 66 |
| min text length | 40 |
| max text length | 23310 |
| average text length | 1217.95 |
| blank long-text count | 0 |
| duplicate hash count | 0 |

## Promotion Summary

| 지표 | 값 |
| --- | ---: |
| candidate | 3184 |
| promoted | 3021 |
| unusable | 153 |
| unmapped | 10 |

## 회복 대상 9권

| bookCode | promoted count |
| --- | ---: |
| `PSA` | 57 |
| `ECC` | 8 |
| `SNG` | 8 |
| `OBA` | 2 |
| `NAM` | 3 |
| `MAL` | 3 |
| `PHM` | 4 |
| `2JN` | 1 |
| `3JN` | 1 |

9권 모두 promoted count가 1 이상이다.

## 66권 분포

| bookCode | count |
| --- | ---: |
| `GEN` | 118 |
| `EXO` | 69 |
| `LEV` | 58 |
| `NUM` | 75 |
| `DEU` | 94 |
| `JOS` | 57 |
| `JDG` | 50 |
| `RUT` | 9 |
| `1SA` | 52 |
| `2SA` | 42 |
| `1KI` | 71 |
| `2KI` | 78 |
| `1CH` | 30 |
| `2CH` | 74 |
| `EZR` | 27 |
| `NEH` | 25 |
| `EST` | 23 |
| `JOB` | 19 |
| `PSA` | 57 |
| `PRO` | 12 |
| `ECC` | 8 |
| `SNG` | 8 |
| `ISA` | 142 |
| `JER` | 110 |
| `LAM` | 7 |
| `EZK` | 71 |
| `DAN` | 21 |
| `HOS` | 26 |
| `JOL` | 6 |
| `AMO` | 26 |
| `OBA` | 2 |
| `JON` | 9 |
| `MIC` | 16 |
| `NAM` | 3 |
| `HAB` | 6 |
| `ZEP` | 6 |
| `HAG` | 3 |
| `ZEC` | 32 |
| `MAL` | 3 |
| `MAT` | 216 |
| `MRK` | 113 |
| `LUK` | 182 |
| `JHN` | 125 |
| `ACT` | 180 |
| `ROM` | 77 |
| `1CO` | 79 |
| `2CO` | 46 |
| `GAL` | 37 |
| `EPH` | 26 |
| `PHP` | 21 |
| `COL` | 22 |
| `1TH` | 23 |
| `2TH` | 9 |
| `1TI` | 30 |
| `2TI` | 19 |
| `TIT` | 12 |
| `PHM` | 4 |
| `HEB` | 50 |
| `JAS` | 25 |
| `1PE` | 27 |
| `2PE` | 14 |
| `1JN` | 21 |
| `2JN` | 1 |
| `3JN` | 1 |
| `JUD` | 6 |
| `REV` | 110 |

## 사용 판단

- 66권 모두 promoted entry가 존재한다.
- 빈 장문 텍스트가 없고, 중복 hash도 없다.
- 회복 대상 9권이 모두 1건 이상으로 확인됐다.
- `unmapped=10`은 현 시점에서 평가/연결 테스트를 막는 직접 사유로 보지 않는다.
- 최종 운영 확정 전에는 짧은 책과 count가 낮은 책을 우선으로 샘플 육안 검수를 수행한다.

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| index 파일 존재 | 확인 |
| promotion summary 존재 | 확인 |
| JSON 집계 | 확인 |
| 9권 count `> 0` | 확인 |
| PDF와 `build` 산출물 ignored 상태 | 확인 |
| report 원문성 키워드 검색 | 매치 없음 |
| 코드 변경 없음 | 확인 |

## 후속 작업

- restricted storage 배포
- `validation_reference_jobs.indexStorageUri` 연결 검증
- 운영 최종 확정 전 샘플 육안 검수
