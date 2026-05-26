# 2026-05-19 ~ 05-20 qtai-server 주석 작성 + dev 머지 정리 — 결과 보고

## 한 줄 요약
전 도메인 `.java` 216개에 JavaDoc + TODO 시그니처 주석을 추가하고, dev 머지 시 충돌난 빌드 산출물 72개를 정리. 다음 도메인 PR이 주석만 보고 책임·권한·의존성을 파악할 수 있는 상태.

## 산출
| 카테고리 | 파일 수 | 변경 |
|---|---|---|
| common (ApiResponse, ErrorCode 등) | 3 | 주석 |
| domain 13개 (admin·ai·audit·bible·member·mission·note·notification·praise·qt·report·sharing·study) | ~195 | 주석 |
| external (kakao, llm) | 7 | 주석 |
| **총 `.java`** | **216** | **+1,360 / -269** |
| 빌드 산출물 정리 | 72 | 삭제 (`bin/*`, `.gradle/*`) |

## 주석 패턴 (다음 PR 작성자용 가이드)
| 대상 | 패턴 |
|---|---|
| UseCase 포트 | 역할 1줄 + 권한 정책 + TODO 메서드 시그니처 |
| Service | 트랜잭션 경계 + 의존 도메인 + TODO 어노테이션 |
| Entity | FK 관계 + cascade 정책 + TODO 필드 힌트 |
| Controller | HTTP 메서드/경로 매트릭스 + base path |
| Mock | `@Primary` 교체 트리거 명시 |
| DTO | 필드 의도 + Validation 힌트 |

## 검증
- [x] 컴파일 통과 (시그니처 무변경)
- [x] dev 머지 충돌 해소 (72/72)
- [x] origin/anno 푸시 완료
- [x] dev PR 머지

## 결정 사항
1. **JavaDoc 한국어** — 팀 가이드, 영어 혼용 시 약어만
2. **TODO 표기 표준화** — `// TODO: <시그니처> — <설명>`, 도메인 PR이 그대로 치환
3. **빌드 산출물 git 제외** — dev에서 들어온 `.gitignore`(.gradle/, bin/, *.class) 룰 수용

## 위험 / 학습
| 문제 | 학습 |
|---|---|
| `git add .` 로 IDE 자동 컴파일 산출물(`.class`)이 같이 묻어 들어감 | 다음부터 `git status` 확인 후 구체 경로로 add |
| 충돌이 "코드 충돌"이 아니라 "수정 vs 삭제" 형태라 IDE 머지 뷰가 비어 보였음 | `deleted by them` = 저쪽이 삭제. 텍스트 마커 없음 |
| `gh` CLI 미설치로 PR 자동 생성 불가 | 브라우저에서 직접 생성, 또는 추후 `winget install GitHub.cli` |

## 다음 단계
1. 도메인 leaf PR 시작 (bible · member)
2. 첫 구현 PR에서 TODO → 실제 코드 치환 패턴 검증

## 관련 커밋
| SHA | 메시지 |
|---|---|
| `e55684e` | 주석 작성 |
| `11af6ff` | anno에 dev 머지 — 빌드 산출물(bin, .gradle) 정리 |
