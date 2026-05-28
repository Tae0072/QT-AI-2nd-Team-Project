# Docker Compose — 멀티 컨테이너 관리

> **왜 배워야 하나:** QT-AI는 서버(Spring Boot) + DB(MySQL) + 기타 서비스를 한 번에 띄우기 위해 Docker Compose를 사용한다. 노션에서 Docker 기초(이미지, 컨테이너, Dockerfile)를 배웠지만, 여러 컨테이너를 함께 관리하는 Docker Compose는 별도로 다루지 않았다.

---

## 1. Docker Compose가 뭔가?

Docker는 하나의 컨테이너를 다루는 도구다. 하지만 실제 서비스는 여러 컨테이너가 함께 돌아간다:

```
QT-AI 서비스 = Spring Boot 서버 + MySQL DB + (필요 시 Redis)
```

Docker Compose는 **여러 컨테이너를 하나의 파일로 정의**하고, 한 번에 띄우고 내릴 수 있게 해준다.

## 2. docker-compose.yml 기본 구조

```yaml
# docker-compose.yml
version: '3.8'

services:
  # 서비스 1: Spring Boot 서버
  app:
    build: .                        # Dockerfile로 이미지 빌드
    ports:
      - "8080:8080"                 # 호스트 8080 → 컨테이너 8080
    depends_on:
      - db                          # db가 먼저 시작된 후에 app 시작
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/qtai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}

  # 서비스 2: MySQL DB
  db:
    image: mysql:8.0                # 공식 MySQL 이미지 사용
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: qtai
    volumes:
      - mysql-data:/var/lib/mysql   # 데이터 영속화

# 볼륨 정의 — 컨테이너가 삭제되어도 데이터 유지
volumes:
  mysql-data:
```

## 3. 자주 쓰는 명령어

```bash
# 모든 서비스 시작 (백그라운드)
docker compose up -d

# 모든 서비스 중지
docker compose down

# 로그 확인
docker compose logs -f app        # app 서비스 로그만 실시간으로

# 서비스 상태 확인
docker compose ps

# 이미지 다시 빌드하고 시작
docker compose up -d --build

# 특정 서비스만 재시작
docker compose restart app
```

## 4. 핵심 개념

### 4.1 서비스 (Service)
하나의 컨테이너 정의. `app`, `db` 같은 이름으로 구분한다.

### 4.2 네트워크
같은 docker-compose.yml 안의 서비스들은 **서비스 이름으로 서로 통신**할 수 있다. `app`에서 `db`로 접근할 때 `db:3306`으로 접근한다 (localhost가 아님).

### 4.3 볼륨 (Volume)
컨테이너 안의 데이터를 호스트 디스크에 저장한다. 컨테이너를 삭제하고 다시 만들어도 데이터가 남는다.

### 4.4 depends_on
서비스 시작 순서를 정한다. `depends_on: db`는 "db가 먼저 시작된 후에 이 서비스를 시작하라"는 뜻이다.

### 4.5 환경 변수
`.env` 파일이나 `environment`로 비밀번호 같은 설정을 주입한다:

```
# .env 파일
DB_PASSWORD=mypassword123
```

## 5. QT-AI에서의 적용

QT-AI 프로젝트 루트에 `docker-compose.yml`이 있다:
- `app`: qtai-server (Dockerfile로 빌드)
- `db`: MySQL 8.0
- v1 배포 기준은 Docker Compose (Kubernetes는 v2 이후)

## 6. Docker와 Docker Compose 비교

| 구분 | Docker | Docker Compose |
|------|--------|---------------|
| 대상 | 컨테이너 1개 | 컨테이너 여러 개 |
| 설정 | `docker run` 명령어 | `docker-compose.yml` 파일 |
| 네트워크 | 수동 설정 | 자동으로 같은 네트워크 |
| 관리 | 개별 관리 | 한 번에 시작/중지 |

## 7. 참고 자료

- Docker Compose 공식 문서: https://docs.docker.com/compose/
- Docker Compose 파일 레퍼런스: https://docs.docker.com/compose/compose-file/
