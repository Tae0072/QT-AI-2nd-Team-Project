import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 설정 파일.
// - React 플러그인을 켜고, 개발 서버 포트를 5173으로 둔다.
// - 개발 중에는 브라우저의 CORS(다른 주소 호출 차단) 때문에 백엔드를 직접 못 부를 수 있다.
//   그래서 '/api' 로 시작하는 요청은 vite가 대신 백엔드(qtai-server)로 전달(proxy)한다.
// - 전달할 백엔드 주소는 .env 의 VITE_API_PROXY_TARGET 으로 정한다.
//   2026-06-10 결정 ③: admin-web 은 처음부터 admin-server(8090) 기준(없으면 8090).
//   게이트웨이가 뜨면 .env 한 줄만 바꾸면 되고 코드는 그대로다.
// - 단, 인증은 JWT 발급자인 service-user(8081)에만 있다:
//   · 관리자 카카오 로그인 POST /api/v1/admin/auth/**  (#452)
//   · 공용 토큰 갱신     POST /api/v1/auth/refresh 등 /api/v1/auth/**  (P2)
//   admin-server(8090)엔 이 컨트롤러가 없으므로, 게이트웨이 확정 전까지 dev 한정으로
//   인증 경로만 VITE_AUTH_PROXY_TARGET(8081)으로 분리한다. 게이트웨이가 뜨면 같은 주소로 두면 된다.
export default defineConfig(({ mode }) => {
  // envDir '.' = vite 실행 위치(admin-web). @types/node 없이 process 미사용.
  const env = loadEnv(mode, '.', 'VITE_');
  const proxyTarget = env.VITE_API_PROXY_TARGET ?? 'http://localhost:8090';
  const authProxyTarget = env.VITE_AUTH_PROXY_TARGET ?? 'http://localhost:8081';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // 구체 경로를 '/api'보다 위에 둔다(http-proxy는 정의 순서대로 매칭).
        // 인증(로그인·갱신)은 service-user(8081) 소관 — 게이트웨이 확정 전 dev 전용 분리.
        '/api/v1/admin/auth': {
          target: authProxyTarget,
          changeOrigin: true,
        },
        // 공용 토큰 갱신(POST /api/v1/auth/refresh) 등 /api/v1/auth/** 도 service-user(8081).
        '/api/v1/auth': {
          target: authProxyTarget,
          changeOrigin: true,
        },
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
