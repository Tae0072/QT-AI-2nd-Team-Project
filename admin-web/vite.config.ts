import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 설정 파일.
// - React 플러그인을 켜고, 개발 서버 포트를 5173으로 둔다.
// - 개발 중에는 브라우저의 CORS(다른 주소 호출 차단) 때문에 백엔드를 직접 못 부를 수 있다.
//   그래서 '/api' 로 시작하는 요청은 vite가 대신 백엔드(qtai-server)로 전달(proxy)한다.
// - 전달할 백엔드 주소는 .env 의 VITE_API_PROXY_TARGET 으로 정한다.
//   admin-web 은 admin-server(8090) 기준(없으면 8090). 게이트웨이가 뜨면 .env 한 줄만 바꾼다.
// - 2026-06-11 결정: 관리자 로그인은 자체 아이디/비밀번호 방식이며 admin-server(8090)가
//   직접 발급·검증한다. 따라서 카카오·service-user(8081) 인증 프록시 분리가 필요 없다.
export default defineConfig(({ mode }) => {
  // envDir '.' = vite 실행 위치(admin-web). @types/node 없이 process 미사용.
  const env = loadEnv(mode, '.', 'VITE_');
  const proxyTarget = env.VITE_API_PROXY_TARGET ?? 'http://localhost:8090';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // 모든 /api 요청을 admin-server(8090)로 전달.
        // 관리자 로그인/갱신(/api/v1/admin/auth/**)도 2026-06-11 결정으로 admin-server가 직접
        // 처리하므로 service-user(8081)로의 인증 프록시 분리가 더 이상 필요 없다.
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
    // 단일 번들(1.15MB)을 라이브러리 단위로 쪼개 첫 로딩·캐싱 개선.
    // (코드리뷰 P5c. 최초 #508로 머지됐으나 #510 롤백에 휩쓸려 revert → 재적용.)
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            antd: ['antd', '@ant-design/icons'],
            vendor: ['react', 'react-dom', 'react-router-dom', 'axios'],
          },
        },
      },
    },
  };
});
