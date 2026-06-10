import { Layout, Menu, Typography, Button, Space, Tag } from 'antd';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { MENU_ITEMS } from '../../constants/menu';
import { ADMIN_ROLE_LABELS, canAccessAdminRoute } from '../../constants/roles';
import { useAuth } from '../../auth/useAuth';

const { Header, Sider, Content } = Layout;

// ===== 관리자 공통 레이아웃 =====
// 왼쪽 사이드바(메뉴) + 위쪽 헤더(제목/로그아웃) + 가운데 콘텐츠 구조.
// 보호 구역의 각 화면은 아래 <Outlet /> 자리에 그려진다.
export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { adminInfo, logout } = useAuth();

  // 메뉴 상수를 Ant Design Menu 형식으로 변환한다.
  // (label 앞에 화면코드를 함께 보여 줘서 명세와 대조하기 쉽게 한다.)
  const menuItems = MENU_ITEMS.filter((item) =>
    canAccessAdminRoute(adminInfo?.adminRole, item.requiredRoles),
  ).map((item) => ({
    key: item.path,
    label: `${item.label}`,
  }));

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" breakpoint="lg" collapsedWidth="0">
        <div
          style={{
            height: 48,
            margin: 16,
            color: '#fff',
            fontWeight: 700,
            fontSize: 16,
            display: 'flex',
            alignItems: 'center',
          }}
        >
          QT-AI 관리자
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingInline: 24,
          }}
        >
          <Typography.Title level={4} style={{ margin: 0 }}>
            관리자 콘솔
          </Typography.Title>
          <Space>
            {adminInfo?.adminRole && (
              <Tag color="blue">
                {ADMIN_ROLE_LABELS[adminInfo.adminRole] ?? adminInfo.adminRole}
              </Tag>
            )}
            <Button onClick={handleLogout}>로그아웃</Button>
          </Space>
        </Header>

        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
