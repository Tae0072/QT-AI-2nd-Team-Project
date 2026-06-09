import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 설정 파일.
// - React 플러그인을 켜고, 개발 서버 포트를 5173으로 둔다.
// - 개발 중에는 브라우저의 CORS(다른 주소 호출 차단) 때문에 백엔드를 직접 못 부를 수 있다.
//   그래서 '/api' 로 시작하는 요청은 vite가 대신 백엔드(qtai-server)로 전달(proxy)한다.
// - 전달할 백엔드 주소는 .env 의 VITE_API_PROXY_TARGET 으로 정한다(없으면 8080 모놀리식).
//   MSA 게이트웨이(service-gateway)가 뜨면 .env 한 줄(8080→8000)만 바꾸면 되고 코드는 그대로다.
export default defineConfig(({ mode }) => {
  // envDir '.' = vite 실행 위치(admin-web). @types/node 없이 process 미사용.
  const env = loadEnv(mode, '.', 'VITE_');
  const proxyTarget = env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
