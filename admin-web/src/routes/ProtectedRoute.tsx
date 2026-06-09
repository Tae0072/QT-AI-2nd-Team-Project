import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuth } from '../auth/useAuth';

// ===== 로그인한 사용자만 통과시키는 길목(가드) =====
// 토큰이 없으면 로그인 화면(/login)으로 돌려보낸다.
// 사용법: <Route element={<ProtectedRoute />}> 안에 보호할 화면들을 넣는다.
// (Outlet 자리에 자식 라우트가 그려진다.)
export default function ProtectedRoute() {
  const { adminInfo, adminLoading, isLoggedIn } = useAuth();
  const location = useLocation();

  if (!isLoggedIn) {
    // 원래 가려던 주소를 state.from 에 담아두면 로그인 후 되돌아올 수 있다.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (adminLoading) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <Spin tip="관리자 권한 확인 중" />
      </div>
    );
  }

  if (!adminInfo) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
