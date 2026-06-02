// QT-AI 기본 부하 점검 (k6 smoke/load)
//
// 대상: dev 프로파일(dev-bypass=인증 비활성) 기준 핵심 읽기 엔드포인트.
// 목적: 시연 전 "핵심 읽기 경로가 모듈한 동시 부하에서 빠르고 안정적인가"를 확인.
//
// 실행:
//   docker compose up -d                # 앱+MySQL+Redis 기동 (dev 프로파일)
//   docker run --rm -i --network host -e BASE_URL=http://localhost:8080 \
//     grafana/k6 run - < perf/k6-smoke.js
//   (Windows/Mac Docker Desktop은 --network host 대신 -e BASE_URL=http://host.docker.internal:8080)
//
// 임계값(threshold)을 못 넘기면 k6가 실패 종료코드를 반환한다 → CI/게이트로도 활용 가능.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  // 20 VU까지 올렸다가 유지 후 정리 — "기본" 수준의 동시 부하
  stages: [
    { duration: '20s', target: 20 },
    { duration: '40s', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],     // 에러율 1% 미만
    http_req_duration: ['p(95)<500'],   // 95% 요청이 500ms 미만
  },
};

// dev 프로파일은 X-Dev-User-Id 헤더로 memberId를 주입한다(개인화 읽기 경로용).
const params = { headers: { 'X-Dev-User-Id': '1' } };

// 핵심 읽기 경로(전부 GET, dev 프로파일에서 200). passage id 1~3은 V8 시드 본문.
const endpoints = [
  '/api/v1/qt/passages/1',        // QT 본문 단건
  '/api/v1/qt/today',             // 오늘의 QT
  '/api/v1/qt/1/simulator',       // 시뮬레이터 상태
  '/api/v1/qt/1/study-content',   // 학습 콘텐츠 진입
];

export default function () {
  for (const path of endpoints) {
    const res = http.get(`${BASE}${path}`, params);
    check(res, {
      'status 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  }
  sleep(1);
}
