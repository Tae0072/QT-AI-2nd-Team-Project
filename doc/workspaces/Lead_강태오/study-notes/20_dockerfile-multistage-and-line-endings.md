# Dockerfile 멀티스테이지 빌드 & 줄바꿈(CRLF/LF) 문제

> **왜 배워야 하나:** 노션에서 Docker 기초(이미지/컨테이너)는 배웠지만, "앱을 빌드까지 포함해 이미지로 굽는 Dockerfile 멀티스테이지"와 "Windows 줄바꿈(CRLF) 때문에 리눅스 컨테이너 빌드가 깨지는 함정"은 다루지 않았다. 이번에 시연 빌드가 바로 이 줄바꿈 문제로 실패했다.

---

## 1. Dockerfile이란

이미지를 어떻게 만들지 적은 레시피 파일. `docker build`가 이 파일을 읽어 이미지를 굽는다.

## 2. 멀티스테이지 빌드 — 빌드용/실행용 분리

앱을 빌드하려면 JDK(무거움)가 필요하지만, 실행은 JRE(가벼움)면 된다. 한 파일에서 단계를 나눈다:

```dockerfile
# 1단계(build): JDK로 jar 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew --no-daemon bootJar     # build/libs/*.jar 생성

# 2단계(run): JRE에 jar만 복사 → 최종 이미지는 가벼움
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```
- `AS build`로 이름 붙인 1단계에서 빌드만 하고, 2단계는 `COPY --from=build`로 결과물(jar)만 가져온다.
- 최종 이미지에는 JDK·소스가 안 들어가서 작고 안전하다.

## 3. 이번에 터진 함정 — CRLF 줄바꿈

빌드가 이 에러로 실패했다:

```
RUN ./gradlew bootJar  →  exit code 127  (실행 불가)
```

**원인:**
- Windows에서 체크아웃한 `gradlew`(쉘 스크립트)의 줄바꿈이 **CRLF**(`\r\n`)였다.
- 리눅스 컨테이너의 `/bin/sh`는 줄바꿈을 **LF**(`\n`)로 기대한다.
- `\r`이 섞여 스크립트 첫 줄(`#!/bin/sh`)을 못 읽어 "실행 불가(127)".

**줄바꿈 두 종류:**
| | 표기 | 쓰는 곳 |
|---|---|---|
| LF | `\n` | 리눅스/맥, 쉘 스크립트 |
| CRLF | `\r\n` | Windows, 배치 파일(.bat) |

## 4. 해결 — 두 가지를 같이

**(1) Dockerfile에서 빌드 직전 줄바꿈 정규화** (지금 깨진 걸 즉시 고침):
```dockerfile
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon bootJar
```
`sed`로 각 줄 끝의 `\r`을 제거 → 리눅스가 읽을 수 있게.

**(2) `.gitattributes`로 재발 방지** (앞으로 체크아웃부터 LF로):
```gitattributes
gradlew     text eol=lf      # 쉘 래퍼는 항상 LF
*.sh        text eol=lf
gradlew.bat text eol=crlf    # 윈도우 배치는 CRLF 유지
*.bat       text eol=crlf
```
- `.gitattributes`는 "이 파일은 어떤 줄바꿈으로 저장할지"를 git에 알려준다. Windows에서 받아도 `gradlew`는 LF로 유지된다.

## 5. QT-AI에서의 적용

- `qtai-server/Dockerfile`(멀티스테이지) + 루트 `docker-compose.yml`에서 이 이미지를 빌드해 앱을 띄운다.
- CRLF 문제는 `sed` 정규화 + `.gitattributes eol=lf`로 영구 해결.

## 6. 한 줄 정리

- 멀티스테이지 = 빌드(JDK) / 실행(JRE) 분리로 작고 안전한 이미지.
- Windows에서 만든 쉘 스크립트는 리눅스 컨테이너에서 CRLF 때문에 깨질 수 있다 → `.gitattributes eol=lf` 습관.

## 7. 참고 자료

- Dockerfile 멀티스테이지: https://docs.docker.com/build/building/multi-stage/
- Git gitattributes(eol): https://git-scm.com/docs/gitattributes
