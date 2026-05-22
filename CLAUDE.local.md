# CLAUDE.local.md — 김지민 개인 보조 설정

Claude Code 세션 시작 시 자동 로드되는 개인 보조 설정.
공식 규칙은 `CLAUDE.md`, 이 파일은 그 보조용.

> 이 파일은 `.gitignore` 처리됨 (개인 설정, commit 안 됨).

---

## 매 세션 자동 로드 문서 3선

다음 3개 문서는 **매 세션 시작 시 컨텍스트에 자동 로드**된다. 김지민의 노트·나눔·공유 작업에서 *코드 한 줄 짤 때마다 참조하는* 핵심 문서이기 때문이다.

@doc/standards/07_요구사항_정의서.md
@doc/standards/04_API_명세서.md
@doc/standards/23_도메인_용어사전.md

### 자동 로드 3선 선정 이유

#### 1. 07_요구사항_정의서.md
모든 결정의 SSoT (`CLAUDE.md §2`). F-ID와 핵심 정책 (`active_unique_key`, 스냅샷 원자성, 자동저장 금지, 로그인 강제) 이 여기 정의. 김지민의 모든 코드는 이 문서의 F-16(자유 노트), 나눔 관련 F-ID 와 충돌하지 않아야 함.

#### 2. 04_API_명세서.md
김지민이 만들 9개 API의 요청·응답·에러 코드 시그니처가 정의. Controller·DTO 짤 때마다 옆에 두고 봄. 임의로 필드를 추가·삭제하면 안 되며, 차이가 필요하면 04를 먼저 갱신해야 한다.

#### 3. 23_도메인_용어사전.md
5개월 차 김지민이 다른 문서·코드를 읽을 때 막히지 않도록 한국어 도메인 용어 정의 ("스냅샷", "active_unique_key", "공유본", "멘션" 등). 글자 수도 가장 작아 (1.1만) 자동 로드 부담 거의 없음.

### 로컬 캐시 갱신

`doc/standards/` 의 3개 파일은 `master` 의 스냅샷이라 시간이 지나면 원본과 어긋날 수 있다. 다음 명령으로 다시 다운로드한다.

```powershell
$docs = @('07_요구사항_정의서.md','04_API_명세서.md','23_도메인_용어사전.md')
foreach ($d in $docs) {
  Invoke-WebRequest -Uri "https://raw.githubusercontent.com/Tae0072/2nd-Team-Project/master/$d" -OutFile "doc\standards\$d" -UseBasicParsing
}
```

> 문서 충돌이 의심되거나, 한 주 이상 지난 경우 위 명령으로 갱신 후 작업.

---

## 필요할 때만 fetch하는 문서 4선

매 세션 자동 로드는 안 되지만, 특정 작업 시 `WebFetch` 도구로 가져온다. 27개 중 김지민 작업과 관련 깊은 4개.

### 4. 02_ERD_문서.md
https://raw.githubusercontent.com/Tae0072/2nd-Team-Project/master/02_ERD_문서.md

**fetch 시점**: Note·SharingPost Entity 합의 (이지윤·이승욱과), Repository 메서드 짤 때, `active_unique_key` 멱등 처리 구현 시.

### 5. 05_시퀀스_다이어그램.md
https://raw.githubusercontent.com/Tae0072/2nd-Team-Project/master/05_시퀀스_다이어그램.md

**fetch 시점**: W3에 나눔 공개 API (`POST /api/v1/notes/{id}/share`) 의 스냅샷 원자성 흐름 구현 직전. 좋아요·댓글·신고같이 여러 도메인이 얽힌 동작 흐름 의심 시.

### 6. 03_아키텍처_정의서.md
https://raw.githubusercontent.com/Tae0072/2nd-Team-Project/master/03_아키텍처_정의서.md

**fetch 시점**: UseCase·Service·Controller 파일을 어느 패키지에 둘지 모를 때. 도메인 간 호출 방식 (`client/{타도메인}/UseCaseMock.java`) 결정 시. ArchUnit 테스트 위반 발생 시.

### 7. 06_화면_기능_정의서.md
https://raw.githubusercontent.com/Tae0072/2nd-Team-Project/master/06_화면_기능_정의서.md

**fetch 시점**: Flutter 화면(N-01~04, S-01~03) 짜기 전. 빈 상태·에러 상태·로딩 상태 처리 명세 확인 시.

---

## 그 외 20개 문서

전체 목록은 https://github.com/Tae0072/2nd-Team-Project/tree/master 에서 확인. 다음 상황에서만 추가 fetch:

- PR 작성 시 → `09_Git_규칙.md`
- 테스트 커버리지 기준 확인 → `18_코드_품질_게이트.md`
- 구현 반영 순서 확인 → `22_구현_저장소_반영_체크리스트.md`
- 04 API 명세에서 시나리오가 부족할 때 → `25_기능_명세서.md`

---

## 작업별 fetch 권장 매트릭스

자동 로드 3개 외에 추가로 fetch가 필요한 경우.

| 작업 | 추가 fetch 문서 |
| --- | --- |
| `POST /api/v1/notes` 구현 | (3개로 충분) |
| Note Entity 합의 (이지윤과) | 02 |
| 나눔 공개 API (W3) | 02 + 05 |
| 좋아요·댓글·신고 (W3) | 05 |
| Flutter N-03 작성 화면 | 06 |
| 도메인 패키지 위치 결정 | 03 |
| PR 만들 때 | 09 |

---

## 김지민 개인 컨텍스트

- 5개월 차. 자유 노트 · 나눔 · 공유 도메인 담당 (백엔드 + Flutter).
- 한국어 응답 선호.
- 산출물 폴더: `doc/workspaces/DevE_김지민/`
  - `workflows/` (작업 계획) · `reports/` (작업 결과) · `study-notes/` (학습 노트)
- 워크플로우: `doc/workspaces/DevE_김지민/김지민_W1-W5_워크플로우.md`
- 공식 일정표: `doc/workspaces/DevE_김지민/김지민_공식일정표.md`
