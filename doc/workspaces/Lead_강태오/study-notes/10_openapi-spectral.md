# OpenAPI + Spectral — API 명세 자동 검증

> **왜 배워야 하나:** QT-AI는 API 명세를 OpenAPI(YAML) 형식으로 관리하고, Spectral이라는 도구로 명세 파일의 규칙 위반을 자동 검사한다. 노션에서 REST API 개념을 배웠지만, API 명세 작성과 검증 도구는 다루지 않았다.

---

## 1. OpenAPI가 뭔가?

OpenAPI(구 Swagger)는 REST API의 구조를 YAML이나 JSON으로 정의하는 **표준 규격**이다. API가 어떤 경로를 갖고, 어떤 데이터를 주고받는지 기계가 읽을 수 있는 문서로 만든다.

```yaml
# openapi.yaml 예시
openapi: 3.0.3
info:
  title: QT-AI API
  version: 1.0.0

paths:
  /api/v1/auth/kakao:
    post:
      summary: 카카오 로그인
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                accessToken:
                  type: string
      responses:
        '200':
          description: 로그인 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TokenResponse'
```

## 2. OpenAPI가 왜 필요한가?

- **프론트-백 소통:** Flutter 개발자와 서버 개발자가 같은 문서를 보고 개발
- **자동 문서 생성:** Swagger UI로 API 문서를 웹페이지로 볼 수 있음
- **자동 검증:** 요청/응답 형식이 명세와 맞는지 자동으로 확인
- **코드 생성:** 명세에서 클라이언트 코드를 자동으로 생성할 수도 있음

## 3. Spectral이 뭔가?

Spectral은 OpenAPI 명세 파일을 **린트(lint)**하는 도구다. "이 명세에 문제가 없는지" 자동으로 검사한다.

```bash
# Spectral 실행 (QT-AI PR 전 검증 명령 중 하나)
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
```

### 검사 예시

```
# Spectral이 잡아주는 문제들:
❌ /api/v1/members — 응답 스키마가 정의되지 않음
❌ /api/v1/qt/{id} — path parameter 'id'의 타입이 없음
⚠️ /api/v1/notes — description이 비어있음
```

## 4. .spectral.yaml 규칙 파일

프로젝트에 맞는 커스텀 규칙을 정의할 수 있다:

```yaml
# .spectral.yaml
extends: ["spectral:oas"]     # OpenAPI 기본 규칙 상속

rules:
  # 모든 경로에 설명이 있어야 함
  operation-description: warn

  # 응답에 반드시 스키마가 있어야 함
  oas3-valid-media-example: error
```

## 5. QT-AI에서의 적용

- `apis/` 폴더에 도메인별 OpenAPI 명세 파일이 있다
- PR을 올리기 전에 Spectral로 명세를 검증한다
- CI 파이프라인에서도 자동으로 검증이 실행된다
- API 명세서(`04_API_명세서.md`)와 OpenAPI YAML이 일치해야 한다

## 6. 참고 자료

- OpenAPI 공식 가이드: https://swagger.io/docs/specification/about/
- Spectral 공식 문서: https://docs.stoplight.io/docs/spectral/
- Swagger Editor (실습용): https://editor.swagger.io/
