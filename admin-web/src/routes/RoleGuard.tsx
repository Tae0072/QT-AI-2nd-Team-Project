import { Button, Result } from 'antd';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import type { AdminRole } from '../constants/roles';
import { canAccessAdminRoute } from '../constants/roles';

interface RoleGuardProps {
  requiredRoles: AdminRole[];
  children: React.ReactNode;
}

export default function RoleGuard({ requiredRoles, children }: RoleGuardProps) {
  const navigate = useNavigate();
  const { adminInfo, isLoggedIn } = useAuth();

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  if (canAccessAdminRoute(adminInfo?.adminRole, requiredRoles)) {
    return <>{children}</>;
  }

  return (
    <Result
      status="403"
      title="접근 권한이 없습니다"
      subTitle="현재 관리자 권한으로는 이 화면을 열 수 없습니다."
      extra={
        <Button type="primary" onClick={() => navigate('/dashboard')}>
          대시보드로 이동
        </Button>
      }
    />
  );
}
