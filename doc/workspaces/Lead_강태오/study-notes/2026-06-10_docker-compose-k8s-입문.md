# 스터디노트 (입문자용) — Docker Compose & 쿠버네티스로 MSA 로컬 배포

이번에 만든 로컬 배포를 처음 보는 사람도 이해하도록 정리했습니다.

## 1. 큰 그림: 컨테이너가 뭐길래

- **컨테이너** = 앱 + 실행에 필요한 것(자바 런타임 등)을 한 상자에 담아, 어느 PC에서나 똑같이 돌아가게 한 것.
- **이미지** = 그 상자의 "설계도/스냅샷". 이미지로 컨테이너를 찍어낸다.
- **Dockerfile** = 이미지를 만드는 레시피.

우리 서비스는 스프링부트 앱 4개입니다. 각 앱을 "fat jar(`*-SNAPSHOT.jar`, 실행에 필요한 라이브러리까지 다 든 jar)"로 빌드한 뒤, Dockerfile이 그 jar를 자바21 이미지에 넣어 `java -jar`로 실행합니다.

```dockerfile
FROM eclipse-temurin:21-jre-jammy      # 자바21 런타임이 든 베이스 이미지
RUN ... useradd spring                 # 보안: 루트 대신 일반 사용자로 실행
COPY build/libs/*-SNAPSHOT.jar app.jar # 미리 빌드한 앱 jar 복사
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

## 2. Docker Compose: 여러 컨테이너를 한 번에

서비스가 4개 + DB + Redis = 6개 컨테이너를 일일이 `docker run` 하긴 번거롭습니다. **Compose**는 이 묶음을 YAML 한 파일(`docker-compose.yml`)로 정의하고 `docker compose up` 한 줄로 띄웁니다.

핵심 개념 3가지:
- **services**: 띄울 컨테이너 목록(mysql, redis, service-user…).
- **environment**: 컨테이너에 넣어줄 설정값(DB 주소, 비밀번호, JWT 키 등). 비밀값은 파일에 직접 안 쓰고 `.env`에서 끌어옵니다.
- **depends_on + healthcheck**: "MySQL이 준비(healthy)된 다음에 앱을 켜라" 같은 순서를 정합니다.

> 왜 단일 DB? 우리 설계는 서비스마다 DB를 따로 두지 않고(=DB-per-service 금지) **MySQL 하나(`qtai`)를 공유**하되, 서비스별로 자기 테이블만 씁니다. 그래서 compose에도 mysql은 1개뿐입니다.

## 3. JWT 키를 "안전하게" 다루기

- **JWT** = 로그인한 사용자임을 증명하는 서명된 토큰. 우리는 RS256(공개키 암호) 방식입니다.
- **개인키(private)**: 토큰에 서명. → **service-user(로그인 담당)만** 가집니다.
- **공개키(public)**: 서명이 진짜인지 검증. → 나머지 서비스가 가집니다.
- 절대 규칙: **키를 코드/저장소에 평문으로 넣지 않는다.** `scripts/generate-keys.ps1`로 키를 만들어 `.env`(깃 추적 제외)에만 두고, 컨테이너엔 실행 시점에 주입합니다. (CI의 gitleaks가 평문 키 커밋을 잡아냄)

이번에 만난 함정 2개:
1. 자바는 개인키를 **PKCS#8** 형식으로 기대하는데, openssl `genpkey -outform DER`이 환경에 따라 **PKCS#1**을 뱉어 "키 디코딩 실패"가 났습니다. → `openssl pkcs8 -topk8`로 PKCS#8 변환을 강제해 해결.
2. 윈도우 PowerShell 5.1은 한글 `.ps1`을 UTF-8(BOM 없음)이면 깨뜨립니다. → 스크립트를 **BOM 포함 UTF-8**로 저장.

## 4. 쿠버네티스(k8s): 한 단계 더

Compose는 "한 PC에서 묶음 실행"에 좋고, **쿠버네티스**는 "여러 대로 확장·자동복구"까지 하는 오케스트레이터입니다. 로컬에선 Docker Desktop에 내장된 k8s를 씁니다. 핵심 오브젝트:

- **Deployment**: "이 이미지를 컨테이너 N개로 항상 띄워둬"(죽으면 자동 재생성).
- **Service**: 컨테이너에 접근하는 안정적 주소. 그중 **NodePort**는 `localhost:30081`처럼 내 PC 포트로 노출.
- **ConfigMap / Secret**: 설정값 / 비밀값 주입(비밀은 Secret). 우리도 비밀번호·JWT 키는 Secret으로만.
- **PVC**: MySQL 데이터를 컨테이너가 죽어도 유지하는 디스크.

적용은 `kubectl apply -f k8s/` 한 번이면 됩니다(자세한 순서는 `k8s/README.md`).

## 5. 한 줄 요약

> bootJar → Dockerfile로 이미지 → Compose(또는 k8s)로 "단일 DB + Redis + 서비스 4개"를 띄우고, JWT 개인키는 발급 서비스에만·평문은 절대 커밋 안 함. 검증은 "컨테이너가 healthy인지 + 보안 응답(401/403)이 오는지 + 같은 DB에 테이블이 생기는지"로 확인.
