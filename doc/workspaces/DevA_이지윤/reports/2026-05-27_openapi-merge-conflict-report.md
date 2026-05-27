# Report - 2026-05-27 openapi-merge-conflict

## 작업 요약

`qtai-server/apis/api-v1/openapi.yaml`에 남아 있던 Git merge conflict marker를 제거하고,
PR 검증에서 OpenAPI 도구가 파일을 파싱할 수 있도록 명세를 정상화했다.

이번 보정의 핵심은 `dev` 브랜치의 Admin AI/System AI 명세를 기준으로 복원한 뒤,
Bible Read API 명세의 tag, path, schema를 함께 포함하도록 병합한 것이다.

## 관련 F-ID

- F-08 성경 본문 조회

## 리뷰 반영 내용

- `<<<<<<<`, `=======`, `>>>>>>>` merge conflict marker를 모두 제거했다.
- `tags`에 `Bible`, `Admin AI`, `System AI`가 함께 포함되도록 정리했다.
- `/api/v1/bible/books`, `/api/v1/bible/verses` 경로를 유지했다.
- `/api/v1/admin/ai/assets`, `/api/v1/admin/ai/assets/{assetId}` 경로를 유지했다.
- System AI 내부 운영 API 경로를 `dev` 기준 명세로 유지했다.
- Bible 응답 스키마와 Admin AI 응답 스키마가 모두 `components.schemas`에 포함되도록 정리했다.
- 금지 번역본 본문 데이터나 실제 성경 본문 예시는 추가하지 않았다.

## 주요 변경 파일

- `qtai-server/apis/api-v1/openapi.yaml`

## 커밋

- `804f732 docs(api): resolve OpenAPI merge conflict`

## 검증 결과

성공:

```bash
rg -n "^(<<<<<<<|=======|>>>>>>>)" qtai-server/apis/api-v1/openapi.yaml
```

결과:

- 출력 없음
- merge conflict marker 0건 확인

성공:

```bash
npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset qtai-server/apis/api-v1/.spectral-temp.yaml
```

결과:

- `No results with a severity of 'error' found!`
- 임시 최소 ruleset 기준으로 Spectral CLI가 OpenAPI YAML을 정상 파싱함
- 검증 후 임시 ruleset 파일은 삭제함

성공:

```bash
git diff --check -- qtai-server/apis/api-v1/openapi.yaml
```

결과:

- exit code 0
- 공백 오류 없음

## 추가 확인

```bash
npx.cmd @redocly/cli lint qtai-server/apis/api-v1/openapi.yaml
```

결과:

- OpenAPI YAML 파싱 자체는 수행됨
- Redocly 기본 recommended ruleset에서 기존 명세의 `allOf + nullable` 패턴을 오류로 보고함
- 해당 오류는 conflict marker 제거와 별개인 기존 명세 스타일 이슈라 이번 범위에서는 수정하지 않음

## 실행하지 못한 검증 / 사유

- 프로젝트 지정 Spectral 명령:

```bash
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
```

사유:

- 저장소 루트와 `qtai-server` 아래에 `.spectral.yaml` 파일이 없어 동일 명령을 그대로 실행할 수 없었다.
- PowerShell에서는 `npx.ps1` 실행 정책 제한이 있어 `npx.cmd`로 대체 실행했다.

## 남은 리스크 / 후속 작업

- 프로젝트 공통 `.spectral.yaml`을 추가하면 PR 검증과 로컬 검증 명령을 동일하게 맞출 수 있다.
- Redocly recommended ruleset 기준 오류를 해소하려면 `nullable` 스키마 표현 방식을 별도 PR에서 정리해야 한다.
