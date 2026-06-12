import { useState } from 'react';
import { Card, Form, Input, Button, Typography, Alert, Space } from 'antd';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { loginAdminWithPassword } from '../api/adminAuth';
import { ApiClientError } from '../api/client';

// ===== 관리자 로그인 (자체 아이디/비밀번호) =====
// 흐름: username/password → POST /api/v1/admin/auth/login → ADMIN access/refresh 저장.
// 2026-06-11 결정: 카카오 로그인 제거. dev 시드 계정은 admin / admin1234.
export default function LoginPage() {
  const { login, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 로그인 후 되돌아갈 주소 (보호 화면에서 튕겨 왔다면 그 주소, 없으면 대시보드)
  const from =
    (location.state as { from?: { pathname: string } } | null)?.from
      ?.pathname ?? '/dashboard';

  // 이미 로그인된 상태면 보호 화면으로 바로 보낸다.
  if (isLoggedIn) {
    return <Navigate to={from} replace />;
  }

  const handleLogin = async (values: { username: string; password: string }) => {
    setError(null);
    setLoading(true);
    try {
      const res = await loginAdminWithPassword(values.username, values.password);
      login(res.accessToken, res.refreshToken); // access + refresh 저장 = 로그인
      navigate(from, { replace: true });
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.code ? `[${e.code}] ${e.message}` : e.message);
      } else {
        setError(e instanceof Error ? e.message : '로그인에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
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
      <Card style={{ width: 400 }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ margin: 0, textAlign: 'center' }}>
            QT-AI 관리자
          </Typography.Title>

          {error && (
            <Alert type="error" showIcon message="로그인 실패" description={error} />
          )}

          <Form
            layout="vertical"
            onFinish={handleLogin}
            requiredMark={false}
            disabled={loading}
          >
            <Form.Item
              label="아이디"
              name="username"
              rules={[{ required: true, message: '아이디를 입력하세요.' }]}
            >
              <Input
                autoComplete="username"
                placeholder="관리자 아이디"
                size="large"
              />
            </Form.Item>
            <Form.Item
              label="비밀번호"
              name="password"
              rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}
              style={{ marginBottom: 12 }}
            >
              <Input.Password
                autoComplete="current-password"
                placeholder="비밀번호"
                size="large"
              />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              size="large"
              loading={loading}
            >
              로그인
            </Button>
          </Form>

          <Typography.Paragraph
            type="secondary"
            style={{ marginBottom: 0, fontSize: 12, textAlign: 'center' }}
          >
            관리자 계정으로 로그인합니다. 관리자 권한이 없으면 접근이 거부됩니다.
          </Typography.Paragraph>
        </Space>
      </Card>
    </div>
  );
}
