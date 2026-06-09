# QT-AI MSA — 로컬 Kubernetes 배포 (Docker Desktop)

회의록 2026-06-09 §9 "로컬은 쿠버네티스 배포로 마무리" + 2026-06-10 Lead 결정(로컬 한정 k8s 허용)에 따른 로컬 배포 매니페스트입니다. **로컬 학습·시연 한정**이며 운영(prod) 도입은 별도 결정 대상입니다.

## 구성

| 구성요소 | 종류 | 노출 | 비고 |
|---|---|---|---|
| mysql | Deployment + PVC | ClusterIP | 단일 DB(`qtai`) 공유 — 도메인 테이블로 구분 |
| redis | Deployment | ClusterIP | service-user Refresh Token 저장소 |
| service-user | Deployment | NodePort 30081 | JWT RS256 **발급**(개인키) |
| service-bible | Deployment | NodePort 30082 | 공개키 **검증만** |
| service-note | Deployment | NodePort 30083 | 공개키 **검증만** |
| service-ai | Deployment | NodePort 30084 | 공개키 **검증만** |
| service-admin | Deployment | NodePort 30090 | 모놀리식 복사본 — 개인키+공개키, Flyway off+JPA update, `SERVER_PORT=8090` |

`lib-common`은 라이브러리라 배포 대상이 아닙니다.

## 사전 준비

1. Docker Desktop에서 Kubernetes 활성화(Settings → Kubernetes → Enable). `kubectl config use-context docker-desktop`.
2. 서비스 이미지 빌드(로컬 Docker 이미지 스토어에 올라가며, Docker Desktop k8s가 그대로 사용 — `imagePullPolicy: IfNotPresent`):
   ```bash
   cd qtai-server
   ./gradlew :service-user:bootJar :service-bible:bootJar :service-note:bootJar :service-ai:bootJar :admin-server:bootJar
   cd ..
   docker compose build      # docker-compose.yml의 5개 서비스 이미지(qtai/service-*:local, qtai/admin-server:local) 생성
   ```
   (compose 없이 개별 빌드도 가능: `docker build -t qtai/service-user:local qtai-server/service-user` 등)

## 배포

```bash
# 1) 네임스페이스 + 설정
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-configmap.yaml

# 2) Secret 생성 (평문 키/비밀번호는 커밋하지 않는다 — 스크립트가 클러스터에 직접 주입)
./k8s/scripts/generate-secret.ps1     # Windows
# ./k8s/scripts/generate-secret.sh    # Linux/macOS
#   또는 수동: k8s/15-secret.example.yaml → k8s/secret.local.yaml 복사(.gitignore됨) 후 값 채워 apply

# 3) 데이터스토어 → 서비스
kubectl apply -f k8s/20-mysql.yaml -f k8s/25-redis.yaml
kubectl apply -f k8s/30-service-user.yaml -f k8s/31-service-bible.yaml \
              -f k8s/32-service-note.yaml -f k8s/33-service-ai.yaml -f k8s/34-service-admin.yaml
# (또는 한 번에: kubectl apply -f k8s/)
```

## 확인

```bash
kubectl -n qtai get pods,svc
kubectl -n qtai rollout status deploy/service-user
# NodePort 접근(서비스 부팅 확인 — actuator 미탑재라 인증 없는 경로는 401/404가 정상 응답)
curl -i http://localhost:30081/        # 8081
```

## 정리

```bash
kubectl delete namespace qtai          # 전체 삭제(PVC 포함)
```

## 비밀 주입 정책 (최소 권한)

- **개인키(`JWT_PRIVATE_KEY`)는 service-user에만** 주입합니다. bible/note/ai/admin은 `SECURITY_JWT_PUBLIC_KEY`(= 공개키)만 받아 검증만 합니다.
- 비밀번호·키·API key는 모두 `qtai-secret`(Secret)에서 주입하며 매니페스트/저장소에 평문으로 두지 않습니다. `k8s/secret.local.yaml`, `k8s/secret.yaml`은 `.gitignore` 처리됩니다.

## 참고

- 스키마: 로컬은 `JPA_DDL_AUTO=update`(엔티티에서 테이블 생성). 단일 DB를 4개 서비스가 공유하므로 최초 기동 시 동시 DDL 경합 가능성이 있으나, `restart`/probe로 자기치유됩니다. 운영 기본은 `validate`.
- Docker Desktop k8s는 로컬 Docker 이미지 스토어를 공유하므로 `image load`/레지스트리 push가 필요 없습니다(minikube/kind는 별도 적재 필요).
