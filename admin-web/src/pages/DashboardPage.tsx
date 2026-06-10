import { useEffect, useState } from 'react';
import {
  Card,
  Tag,
  Typography,
  Space,
  Alert,
  Spin,
  Row,
  Col,
  Statistic,
} from 'antd';
import { getDashboard, type DashboardSummary } from '../api/dashboard';

// ===== AD-01 대시보드 =====
// GET /api/v1/admin/dashboard — 운영 요약 지표. 백엔드 미구현 시 '준비 중' 안내(자동 활성화).

export default function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getDashboard()
      .then((res) => {
        if (!cancelled) {
          setData(res);
          setReady(true);
        }
      })
      .catch(() => {
        if (!cancelled) setReady(false);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const renderValue = (v: unknown): string | number =>
    typeof v === 'number' || typeof v === 'string' ? v : JSON.stringify(v);

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-01</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            대시보드
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          운영 요약 지표(대기 중 검증·신고 건수, 오늘 QT 상태 등)를 한눈에 봅니다.
        </Typography.Paragraph>

        <Spin spinning={loading}>
          {ready && data ? (
            <Row gutter={[16, 16]}>
              {Object.entries(data).map(([k, v]) => (
                <Col xs={12} md={6} key={k}>
                  <Card size="small">
                    <Statistic title={k} value={renderValue(v)} />
                  </Card>
                </Col>
              ))}
            </Row>
          ) : (
            !loading && (
              <Alert
                type="info"
                showIcon
                message="백엔드 준비 중 (E단계)"
                description="대시보드 API(GET /api/v1/admin/dashboard)가 아직 구현되지 않았습니다. 백엔드가 준비되면 이 화면이 자동으로 지표를 표시합니다."
              />
            )
          )}
        </Spin>
      </Space>
    </Card>
  );
}
