import { useState } from 'react';
import { Card, Form, Input, Button, Typography, Alert, Space } from 'antd';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

// ===== 로그인 화면 (임시 토큰 입력 방식) =====
// 지금은 카카오 웹 로그인 대신, 발급받은 ADMIN 액세스 토큰을 직접 붙여넣어 로그인한다.
// 토큰을 저장하면 이후 모든 API 요청에 Authorization 헤더가 자동으로 붙는다. (api/client.ts)
// 추후 카카오 웹 로그인 / 서버측 OAuth 로 교체 예정.
export default function LoginPage() {
  const { login, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [token, setToken] = useState('');

  // 로그인 후 되돌아갈 주소 (보호 화면에서 튕겨 왔다면 그 주소, 없으면 대시보드)
  const from =
    (location.state as { from?: { pathname: string } } | null)?.from
      ?.pathname ?? '/dashboard';

  // 이미 로그인된 상태면 보호 화면으로 바로 보낸다.
  if (isLoggedIn) {
    return <Navigate to={from} replace />;
  }

  const handleSubmit = () => {
    const trimmed = token.trim();
    if (!trimmed) return;
    login(trimmed); // 토큰 저장 = 로그인
    navigate(from, { replace: true });
  };

  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 440 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ margin: 0, textAlign: 'center' }}>
            QT-AI 관리자
          </Typography.Title>

          <Alert
            type="info"
            showIcon
            message="임시 로그인 방식"
            description="발급받은 ADMIN 액세스 토큰을 아래에 붙여넣어 주세요. (카카오 웹 로그인 연동 전까지의 임시 방식입니다.)"
          />

          <Form layout="vertical" onFinish={handleSubmit}>
            <Form.Item label="ADMIN 액세스 토큰" required>
              <Input.Password
                value={token}
                onChange={(e) => setToken(e.target.value)}
                placeholder="'Bearer ' 없이 토큰 값만 붙여넣기"
                autoComplete="off"
              />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              disabled={!token.trim()}
            >
              로그인
            </Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
}
