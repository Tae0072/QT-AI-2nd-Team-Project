import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 설정 파일.
// - React 플러그인을 켜고, 개발 서버 포트를 5173으로 둔다.
// - 개발 중에는 브라우저의 CORS(다른 주소 호출 차단) 때문에 백엔드를 직접 못 부를 수 있다.
//   그래서 '/api' 로 시작하는 요청은 vite가 대신 백엔드(qtai-server)로 전달(proxy)한다.
//   백엔드 주소가 다르면 target 값만 바꾸면 된다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
