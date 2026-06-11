import { useCallback, useEffect, useState } from 'react';
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
  Descriptions,
  Table,
  Button,
  Empty,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined } from '@ant-design/icons';
import {
  getDashboard,
  type DashboardSummary,
  type RecentAuditLog,
} from '../api/dashboard';
import { ApiClientError } from '../api/client';
import { formatDateTime } from '../utils/datetime';

// ===== AD-01 대시보드 =====
// GET /api/v1/admin/dashboard — 운영 요약 지표 + 오늘 QT 상태 + 최근 감사 로그.
// 백엔드 AdminDashboardResponse(admin-server) 기준 실데이터 렌더.

const auditColumns: ColumnsType<RecentAuditLog> = [
  {
    title: '시각',
    dataIndex: 'createdAt',
    width: 170,
    render: (v: string) => formatDateTime(v),
  },
  {
    title: '행위자',
    width: 200,
    render: (_, r) => (
      <Space size={4}>
        <Tag color={r.actorType === 'ADMIN' ? 'geekblue' : r.actorType === 'SYSTEM_BATCH' ? 'gold' : 'default'}>
          {r.actorType}
        </Tag>
        <span>{r.adminUserId != null ? `#${r.adminUserId}` : '-'}</span>
      </Space>
    ),
  },
  {
    title: '액션',
    dataIndex: 'actionType',
    render: (v: string) => <Typography.Text code>{v}</Typography.Text>,
  },
  {
    title: '대상',
    width: 200,
    render: (_, r) =>
      r.targetType ? `${r.targetType}${r.targetId != null ? ` #${r.targetId}` : ''}` : '-',
  },
];

export default function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getDashboard()
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        if (e instanceof ApiClientError) {
          setError(e.code ? `[${e.code}] ${e.message}` : e.message);
        } else {
          setError(e instanceof Error ? e.message : '대시보드를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load]);

  const todayQt = data?.todayQt;

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space align="center">
            <Tag color="blue">AD-01</Tag>
            <Typography.Title level={3} style={{ margin: 0 }}>
              대시보드
            </Typography.Title>
          </Space>
          <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>
            새로고침
          </Button>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          운영 요약 지표(검증 대기·신고 건수), 오늘 QT 상태, 최근 관리자 활동을 한눈에 봅니다.
        </Typography.Paragraph>

        {error ? (
          <Alert
            type="error"
            showIcon
            message="대시보드를 불러오지 못했습니다"
            description={error}
            action={
              <Button size="small" onClick={load}>
                재시도
              </Button>
            }
          />
        ) : (
          <Spin spinning={loading}>
            {data && (
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                {/* 운영 카운트 3종 */}
                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={8}>
                    <Card size="small">
                      <Statistic title="AI 검증 대기" value={data.pendingAiValidationCount} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8}>
                    <Card size="small">
                      <Statistic title="신고 접수" value={data.receivedReportCount} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8}>
                    <Card size="small">
                      <Statistic title="신고 검토 중" value={data.reviewingReportCount} />
                    </Card>
                  </Col>
                </Row>

                {/* 오늘 QT 상태 */}
                {todayQt && (
                  <Card size="small" title="오늘 QT">
                    <Descriptions column={{ xs: 1, sm: 2 }} size="small">
                      <Descriptions.Item label="날짜">{todayQt.qtDate || '-'}</Descriptions.Item>
                      <Descriptions.Item label="상태">
                        <Tag color={todayQt.status === 'READY' ? 'green' : 'orange'}>
                          {todayQt.status}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="제목">{todayQt.title || '-'}</Descriptions.Item>
                      <Descriptions.Item label="시뮬레이터">
                        {todayQt.simulatorStatus || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="해설">
                        {todayQt.hasExplanation ? '있음' : '없음'}
                      </Descriptions.Item>
                      <Descriptions.Item label="캐시">{todayQt.cacheStatus || '-'}</Descriptions.Item>
                    </Descriptions>
                  </Card>
                )}

                {/* 최근 감사 로그 */}
                <Card size="small" title="최근 관리자 활동">
                  <Table<RecentAuditLog>
                    rowKey="id"
                    size="small"
                    columns={auditColumns}
                    dataSource={data.recentAuditLogs}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                    locale={{
                      emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="최근 활동이 없습니다" />,
                    }}
                  />
                </Card>
              </Space>
            )}
          </Spin>
        )}
      </Space>
    </Card>
  );
}
