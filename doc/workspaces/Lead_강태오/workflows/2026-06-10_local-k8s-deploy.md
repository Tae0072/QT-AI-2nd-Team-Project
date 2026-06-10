# 2026-06-10 Lead 결정 — 로컬 배포 한정 Kubernetes/Helm 허용 (CI 가드 완화)

## 배경
- 회의록 2026-06-09 §9: "남은 기간 약 3일 — MSA 4개 분리 + 관리자 서버 분리, **로컬은 쿠버네티스 배포 형태로 마무리**."
- 그러나 `CLAUDE.md §8`은 "Kafka, Kubernetes, Helm v1 도입"을 금지하고, CI `qt-ai-ci.yml`의 Requirements Guard #6이 `k8s/`·`helm/`·`Chart.yaml` 디렉터리를 BLOCK("v1은 Docker Compose만")한다.
- 회의록(로컬 k8s 마무리)과 SSoT(금지)가 충돌 → 가드를 풀려면 SSoT부터 갱신해야 한다(우선순위 07→03→04→18→23→09, 가드가 SSoT보다 먼저 완화될 수 없음).

## 결정 (2026-06-10 Lead 승인)
- **로컬 배포 한정으로 Kubernetes/Helm 매니페스트를 허용**한다. (학습·시연 마무리 목적)
- **운영(프로덕션) K8s/Helm 도입은 별도 결정**이 필요하다 — 본 예외는 로컬 배포에 한정.
- **Kafka 확장 금지는 유지** — Kafka는 AI 영역만(회의록 §3). 본 예외는 Kafka와 무관.
- 그 외 금지 가드(PostgreSQL, SSE/세션형 AI, RAG/Vector DB, Anthropic SDK, 금지 번역본, 교회 인증, 찬양 가사/음원, javax.* 등)는 그대로 유지.

## 반영
1. `CLAUDE.md §8`: "Kafka, Kubernetes, Helm v1 도입" 항목에 예외 주석 추가(로컬 배포 한정 K8s/Helm 허용, 운영 도입 별도 결정, Kafka 확장 금지 유지). 근거로 본 문서 링크.
2. `.github/workflows/qt-ai-ci.yml` Requirements Guard #6: `[BLOCK] + FAILED=1` → `[INFO]` 비차단 로그로 완화. 다른 가드는 불변.
3. PR #432 본문에 회의록 §9 + 본 결정 문서 링크 명시.

## 후속
- 로컬 배포 매니페스트(`k8s/` Deployment/Service/Ingress 등)는 별도 PR로 추가 — 5개 서비스(user/bible/note/ai/admin) 컨테이너화 포함.
- 운영 도입 검토 시 본 예외를 재평가하고 아키텍처 문서(03)에 반영.
