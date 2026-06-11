import { useState } from 'react';
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  Alert,
  Space,
  Divider,
} from 'antd';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { loginWithKakao } from '../auth/kakao';
import { loginAdminWithKakao } from '../api/adminAuth';
import { ApiClientError } from '../api/client';

// ===== 관리자 로그인 (카카오 JS SDK) =====
// 흐름: 카카오 로그인 → 카카오 access token → POST /api/v1/admin/auth/kakao → ADMIN 토큰 저장.
// 서버 /oauth2 미사용(2026-06-10 결정 ①). 응답 계약: 2026-06-10 admin-kakao-auth-contract.
// 백엔드(#452)·라우팅(dev vite가 /api/v1/admin/auth → service-user 8081로 분리) 연결 완료.
// 카카오 JS 키(VITE_KAKAO_JS_KEY)만 주입하면 실로그인이 동작한다.
// 키 주입 전 로컬 작업을 막지 않도록 '개발용 토큰' 입력을 dev 모드에서만 임시 유지한다(prod 빌드에서 제거).
export default function LoginPage() {
  const { login, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [devToken, setDevToken] = useState('');

  // 로그인 후 되돌아갈 주소 (보호 화면에서 튕겨 왔다면 그 주소, 없으면 대시보드)
  const from =
    (location.state as { from?: { pathname: string } } | null)?.from
      ?.pathname ?? '/dashboard';

  // 이미 로그인된 상태면 보호 화면으로 바로 보낸다.
  if (isLoggedIn) {
    return <Navigate to={from} replace />;
  }

  const handleKakaoLogin = async () => {
    setError(null);
    setLoading(true);
    try {
      const kakaoToken = await loginWithKakao(); // 카카오 access token
      const res = await loginAdminWithKakao(kakaoToken); // ADMIN 토큰 발급
      login(res.accessToken); // accessToken 저장 = 로그인
      navigate(from, { replace: true });
    } catch (e) {
      // 합의 ④: 권한 부족(403 ADMIN_USER_NOT_FOUND) 등은 별도 화면 없이 ErrorCode 그대로 표시.
      if (e instanceof ApiClientError) {
        setError(e.code ? `[${e.code}] ${e.message}` : e.message);
      } else {
        setError(e instanceof Error ? e.message : '로그인에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = () => {
    const trimmed = devToken.trim();
    if (!trimmed) return;
    login(trimmed); // 토큰 저장 = 로그인 (개발용)
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

          {error && (
            <Alert type="error" showIcon message="로그인 실패" description={error} />
          )}

          <Button
            type="primary"
            block
            size="large"
            loading={loading}
            onClick={handleKakaoLogin}
            style={{
              background: '#FEE500',
              color: '#191600',
              border: 'none',
              fontWeight: 600,
            }}
          >
            카카오로 로그인
          </Button>
          <Typography.Paragraph
            type="secondary"
            style={{ marginBottom: 0, fontSize: 12, textAlign: 'center' }}
          >
            카카오 계정으로 로그인합니다. 관리자 권한이 없으면 접근이 거부됩니다.
          </Typography.Paragraph>

          {/* 개발용 토큰 입력: dev 모드에서만 노출(백엔드 카카오 인증 준비 전 임시). prod 빌드에서는 제거됨. */}
          {import.meta.env.DEV && (
            <>
              <Divider style={{ margin: '8px 0' }} plain>
                개발용 (백엔드 준비 전 임시)
              </Divider>
              <Form layout="vertical" onFinish={handleDevLogin}>
                <Form.Item label="개발용 ADMIN 토큰" style={{ marginBottom: 8 }}>
                  <Input.Password
                    value={devToken}
                    onChange={(e) => setDevToken(e.target.value)}
                    placeholder="'Bearer ' 없이 토큰 값만 붙여넣기"
                    autoComplete="off"
                  />
                </Form.Item>
                <Button htmlType="submit" block disabled={!devToken.trim()}>
                  개발용 토큰으로 로그인
                </Button>
              </Form>
            </>
          )}
        </Space>
      </Card>
    </div>
  );
}
