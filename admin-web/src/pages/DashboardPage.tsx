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
  Popconfirm,
  Empty,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  getDashboard,
  type DashboardSummary,
  type RecentAuditLog,
} from '../api/dashboard';
import { generateQtPassageExplanation } from '../api/aiAssets';
import { ApiClientError } from '../api/client';
import { formatDateTime } from '../utils/datetime';

// ===== AD-01 대시보드 =====
// GET /api/v1/admin/dashboard — 운영 요약 지표 + 오늘 QT 상태 + 최근 감사 로그.
// 백엔드 AdminDashboardResponse(admin-server) 기준 실데이터 렌더.

function actorDetailLabel(log: RecentAuditLog) {
  if (log.adminUserId != null) return `#${log.adminUserId}`;
  if (log.actorType === 'ADMIN') return '관리자 정보 없음';
  if (log.actorType === 'SYSTEM_BATCH') return '시스템';
  return '-';
}

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
        <span>{actorDetailLabel(r)}</span>
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

// 문제 상태에서 조치 화면으로 보내는 인라인 CTA 링크.
function CtaLink({ label, to }: { label: string; to: string }) {
  const navigate = useNavigate();
  return (
    <Button type="link" size="small" style={{ padding: 0, height: 'auto' }} onClick={() => navigate(to)}>
      {label} <ArrowRightOutlined />
    </Button>
  );
}

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
  // 오늘 QT 문제 조치 동선: qtPassageId 있으면 해당 본문으로 포커스, 없으면 목록으로.
  const qtLink = todayQt?.qtPassageId
    ? `/qt-passages?focusId=${todayQt.qtPassageId}`
    : '/qt-passages';

  // 해설 "없음" → 관리자 해설 생성 트리거(F-02/F-06). 생성은 배치/시스템 처리라 즉시 완료가 아니다.
  const [generating, setGenerating] = useState(false);
  const onGenerateExplanation = async () => {
    if (!todayQt?.qtPassageId) return;
    setGenerating(true);
    try {
      const r = await generateQtPassageExplanation(todayQt.qtPassageId);
      if (r.createdCount > 0) {
        message.success(`해설 생성 요청됨: ${r.createdCount}건 (잠시 후 검증 대기 목록에 표시)`);
      } else {
        message.info(
          r.reason
            ? `생성할 해설이 없습니다 (${r.reason})`
            : '생성 대상 절이 없습니다 (이미 생성/승인됨).',
        );
      }
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '해설 생성 요청에 실패했습니다.');
    } finally {
      setGenerating(false);
    }
  };

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
                {/* 운영 카운트 3종 — 대기 건이 있으면 처리 화면으로 가는 CTA 노출.
                    align=stretch + Card height:100% 로 CTA 유무와 무관하게 3칸 높이를 맞춘다. */}
                <Row gutter={[16, 16]} align="stretch">
                  <Col xs={24} sm={8}>
                    <Card size="small" style={{ height: '100%' }}>
                      <Statistic title="AI 검증 대기" value={data.pendingAiValidationCount} />
                      {data.pendingAiValidationCount > 0 && (
                        <CtaLink label="검증하러 가기" to="/ai-assets" />
                      )}
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card size="small" style={{ height: '100%' }}>
                      <Statistic title="신고 접수" value={data.receivedReportCount} />
                      {data.receivedReportCount > 0 && (
                        <CtaLink label="신고 처리하기" to="/reports?status=RECEIVED" />
                      )}
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card size="small" style={{ height: '100%' }}>
                      <Statistic title="신고 검토 중" value={data.reviewingReportCount} />
                      {data.reviewingReportCount > 0 && (
                        <CtaLink label="검토 이어가기" to="/reports?status=REVIEWING" />
                      )}
                    </Card>
                  </Col>
                </Row>

                {/* 오늘 QT 상태 */}
                {todayQt && (
                  <Card size="small" title="오늘 QT">
                    <Descriptions column={{ xs: 1, sm: 2 }} size="small">
                      <Descriptions.Item label="날짜">{todayQt.qtDate || '-'}</Descriptions.Item>
                      <Descriptions.Item label="상태">
                        <Space size={8}>
                          <Tag color={todayQt.status === 'READY' ? 'green' : 'orange'}>
                            {todayQt.status}
                          </Tag>
                          {todayQt.status !== 'READY' && (
                            <CtaLink label="QT 관리" to={qtLink} />
                          )}
                        </Space>
                      </Descriptions.Item>
                      <Descriptions.Item label="제목">{todayQt.title || '-'}</Descriptions.Item>
                      <Descriptions.Item label="시뮬레이터">
                        <Space size={8}>
                          <Tag
                            color={todayQt.simulatorStatus === 'READY' ? 'green' : 'orange'}
                          >
                            {todayQt.simulatorStatus || '-'}
                          </Tag>
                          {/* 시뮬레이터 전용 관리 화면은 후속 PR(B-1). 임시로 QT 본문 관리로 연결. */}
                          {todayQt.simulatorStatus && todayQt.simulatorStatus !== 'READY' && (
                            <CtaLink label="QT 관리" to={qtLink} />
                          )}
                        </Space>
                      </Descriptions.Item>
                      <Descriptions.Item label="해설">
                        <Space size={8} wrap>
                          <span>{todayQt.hasExplanation ? '있음' : '없음'}</span>
                          {!todayQt.hasExplanation && (
                            <>
                              {/* 미생성 해설 → 관리자 생성 트리거(배치/시스템 처리, 즉시 완료 아님) */}
                              <Popconfirm
                                title="이 본문의 해설 생성을 요청할까요?"
                                description="미생성 절에 대해 해설 생성 job을 큐잉합니다."
                                okText="생성 요청"
                                cancelText="취소"
                                onConfirm={onGenerateExplanation}
                                disabled={!todayQt.qtPassageId}
                              >
                                <Button
                                  type="link"
                                  size="small"
                                  loading={generating}
                                  disabled={!todayQt.qtPassageId}
                                  style={{ padding: 0, height: 'auto' }}
                                >
                                  해설 생성 요청
                                </Button>
                              </Popconfirm>
                              {/* 이미 생성됐으나 미승인인 경우 → 검증 화면 */}
                              <CtaLink label="검증하러 가기" to="/ai-assets" />
                            </>
                          )}
                        </Space>
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
