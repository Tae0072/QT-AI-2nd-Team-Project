# 모니터링 스택
#
# 표준 스택:
#   - Prometheus + Micrometer (메트릭)
#   - Loki + Promtail DaemonSet (로그)
#   - Jaeger + OpenTelemetry (트레이스)
#   - Grafana (대시보드)
#
# ⚠️ Tempo 트레이스 절대 금지 — Jaeger만 허용
