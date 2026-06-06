# 성경 JSON 출처 검토표

이 문서는 F-01 성경·QT 본문 화면 구현에서 사용할 성경 JSON 후보의 출처, 라이선스, 재배포 가능 여부를 적재 전에 검토하기 위한 기준 산출물이다. 이 표는 성경 본문 저장과 배포 과정의 저작권 리스크를 낮춘다. 본문 텍스트나 원본 JSON 파일은 저장하지 않는다.

## 기준 문서

- `doc/프로젝트 문서/07_요구사항_정의서.md` F-01 데이터 정책
- `doc/프로젝트 문서/18_코드_품질_게이트.md` §3.7 성경 JSON 소스 등록 검사
- `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md` P0 성경 JSON 소스 검토 표
- `doc/프로젝트 문서/23_도메인_용어사전.md` 금지 번역본 기준

## 성경 JSON 출처 검토표

현재 승인된 후보는 아래와 같다. 실제 본문 텍스트, 원본 JSON, 가공 JSON, SQLite DB, seed SQL, fixture 파일은 이 디렉터리에 커밋하지 않는다.

후보를 추가할 때는 아래 컬럼을 모두 채운다. 빈 미정 행은 두지 않으며, 라이선스나 재배포 조건이 불명확한 후보는 `NEEDS_LEAD_REVIEW`로 남기고 seed, fixture, 관리자 등록 데이터로 적재하지 않는다.

| reviewStatus | language | translationName | repoUrl | license | attribution | redistributionAllowed | appBundleAllowed | serverMasterAllowed | prohibitedReason | reviewedAt | reviewer | notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| APPROVED | ko | KorRV | https://github.com/scrollmapper/bible_databases/blob/master/formats/json/KorRV.json | MIT License | scrollmapper/bible_databases KorRV | true | true | true |  | 2026-06-01 | 이지윤 | `2nd-Team-Project` 기준 한글 본문 후보를 `88.json`에서 `KorRV`로 정리. 실제 본문 파일은 별도 보관 후 적재 workflow에서 처리한다. |
| APPROVED | en | KJV | https://github.com/scrollmapper/bible_databases/blob/master/formats/json/KJV.json | MIT License | scrollmapper/bible_databases KJV | true | false | true |  | 2026-06-01 | 이지윤 | 영어 본문 후보. F-01 정책상 앱 로컬 번들 대상이 아니라 서버 마스터/온라인 조회 기준으로 사용한다. |

### 컬럼 기준

| 컬럼 | 기준 |
| --- | --- |
| `reviewStatus` | `APPROVED`, `REJECTED`, `NEEDS_LEAD_REVIEW` 중 하나만 사용한다. |
| `language` | `ko`, `en`처럼 본문 언어를 식별한다. |
| `translationName` | 후보 번역본명을 기록한다. 프로젝트 금지 번역본은 후보 표에 추가하지 않는다. |
| `repoUrl` | 원본 공개 저장소 또는 공식 배포 위치를 기록한다. |
| `license` | 라이선스명, 라이선스 파일 위치, 또는 검토 필요 상태를 기록한다. |
| `attribution` | 앱, 관리자 화면, 문서에 표시할 출처 표기 방식을 기록한다. |
| `redistributionAllowed` | 재배포 가능 여부를 `true`, `false`, `unknown` 중 하나로 기록한다. |
| `appBundleAllowed` | 한글 본문의 클라이언트 로컬 SQLite 번들 또는 다운로드 허용 여부를 기록한다. |
| `serverMasterAllowed` | 서버 마스터 사본 보관 가능 여부를 기록한다. |
| `prohibitedReason` | `REJECTED` 또는 금지 데이터 차단 시 사유를 기록한다. 승인 후보는 비워둘 수 있다. |
| `reviewedAt` | 검토일을 KST 기준 `YYYY-MM-DD` 형식으로 기록한다. |
| `reviewer` | 검토자를 기록한다. |
| `notes` | 버전 식별자, 출처 표기 주의사항, Lead 확인 필요 사항을 기록한다. |

## 금지/보류 기준

- 프로젝트 금지 번역본은 후보 표에 행으로 추가하지 않는다.
- 성서 유니온 또는 두란노 본문 텍스트는 저장하지 않는다. QT 본문 큐레이션에는 날짜별 책, 장, 절 범위 정보만 저장한다.
- `repoUrl`, `license`, `translationName`, `attribution`, `redistributionAllowed` 중 하나라도 없으면 적재 PR을 머지하지 않는다.
- `redistributionAllowed=false` 후보는 seed, fixture, 관리자 등록 데이터로 적재하지 않는다.
- `redistributionAllowed=unknown`이거나 라이선스 파일 해석이 불명확한 후보는 `NEEDS_LEAD_REVIEW`로 분류하고 본문 적재 전 Lead 검토를 받는다.
- 출처 표기 방식이 불명확하거나 앱/관리자 화면 표기 요구를 충족하지 못하면 적재하지 않는다.

## 저장 위치 판단 기준

| 언어 | appBundleAllowed 판단 | serverMasterAllowed 판단 |
| --- | --- | --- |
| 한글 | F-01 정책에 따라 클라이언트 로컬 SQLite 번들 또는 다운로드 가능 여부를 별도로 검토한다. | 서버 마스터 사본 보관 가능 여부를 별도로 검토한다. |
| 영어 | 기본적으로 로컬 번들 대상이 아니므로 `false`를 우선 검토한다. | F-01 정책에 따라 서버 마스터 사본과 온라인 조회 가능 여부를 검토한다. |

## 운영 규칙

- 원본 성경 JSON 파일, 가공 JSON, SQLite DB, seed SQL, fixture 파일은 Git에 커밋하지 않는다.
- 승인 전 후보는 별도 스토리지 또는 로컬 검토 환경에만 보관하고, 저장 위치와 접근 권한을 PR에 기록한다.
- 승인 전에는 `bible_books`, `bible_verses` 적재 로직, 마이그레이션, 관리자 등록 데이터를 만들지 않는다.
- 승인된 후보가 생기면 이 표에 검토 상태와 출처 표기 방식을 먼저 기록한 뒤 적재 workflow를 별도로 작성한다.
- PR 본문에는 F-01, `18_코드_품질_게이트.md` §3.7, `22_구현_저장소_반영_체크리스트.md`의 P0 성경 JSON 소스 검토 표 항목을 근거로 남긴다.
- PR 본문에는 "본문 텍스트 커밋 없음"을 명시한다.
