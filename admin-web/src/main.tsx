import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import koKR from 'antd/locale/ko_KR';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import 'antd/dist/reset.css';

// ===== 앱의 시작점(진입점) =====
// 브라우저가 가장 먼저 실행하는 파일이다. 아래 순서로 화면을 감싼다.
//  1) ConfigProvider : Ant Design(UI 라이브러리)을 한국어로 설정
//  2) AuthProvider    : 로그인 토큰 상태를 모든 화면이 공유하도록 제공
//  3) BrowserRouter   : 주소(URL)에 따라 보여줄 화면을 바꿔주는 라우터
//  4) App             : 실제 라우트(주소-화면 연결)가 정의된 곳
ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ConfigProvider locale={koKR}>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </ConfigProvider>
  </React.StrictMode>,
);
