import { useEffect, useState } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Typography,
  Space,
  Input,
  Button,
  Tooltip,
  Spin,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  getAiMonitoring,
  type AiMonitoringSummary,
  type AiMonitoringParams,
  type BatchRunFailure,
  type FailureReason,
  type BlockedReason,
  type ChecklistSummary,
} from '../api/aiMonitoring';
import { formatDateTime } from '../utils/datetime';

// ===== AD-08 AI 운영 모니터링 (읽기 전용) =====
// GET /api/v1/admin/ai/monitoring — 생성/검증/배치/Q&A 집계 대시보드.
// 권한: OPERATOR/REVIEWER/SUPER_ADMIN.

export default function AiMonitoringPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<AiMonitoringSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [query, setQuery] = useState<AiMonitoringParams>({});
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getAiMonitoring(query)
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((e: unknown) => {
        if (!cancelled)
          message.error(
            e instanceof Error ? e.message : '모니터링을 불러오지 못했습니다.',
          );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query, reloadKey]);

  const onSearch = () =>
    setQuery({ from: from.trim() || undefined, to: to.trim() || undefined });
  const onReset = () => {
    setFrom('');
    setTo('');
    setQuery({});
  };
  const reload = () => setReloadKey((k) => k + 1);

  const failureCols: ColumnsType<FailureReason> = [
    { title: '결과 코드', dataIndex: 'resultCode' },
    { title: '건수', dataIndex: 'count', width: 100, align: 'right' },
  ];
  const blockedCols: ColumnsType<BlockedReason> = [
    { title: '차단 사유', dataIndex: 'blockedReason' },
    { title: '건수', dataIndex: 'count', width: 100, align: 'right' },
  ];
  const checklistCols: ColumnsType<ChecklistSummary> = [
    { title: '체크리스트', dataIndex: 'checklistType' },
    {
      title: '활성 버전',
      dataIndex: 'activeVersion',
      width: 140,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '통과율',
      dataIndex: 'passRate',
      width: 120,
      align: 'right',
      render: (v: number) => `${(v * 100).toFixed(1)}%`,
    },
  ];
  const batchFailCols: ColumnsType<BatchRunFailure> = [
    {
      title: '시각',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    { title: '배치', dataIndex: 'batchName' },
    {
      title: '상태',
      dataIndex: 'status',
      width: 120,
      render: (v: string) => <Tag color="red">{v}</Tag>,
    },
    {
      title: '오류',
      dataIndex: 'errorMessage',
      render: (v: string | null) => v ?? '-',
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-08</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 운영 모니터링
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          AI 생성 작업·검증·배치·Q&amp;A 집계를 조회합니다(읽기 전용, 원문 미포함).
          권한: OPERATOR / REVIEWER / SUPER_ADMIN.
        </Typography.Paragraph>

        <Space wrap>
          <Input
            placeholder="시작일 YYYY-MM-DD"
            style={{ width: 160 }}
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            onPressEnter={onSearch}
            allowClear
          />
          <Input
            placeholder="종료일 YYYY-MM-DD"
            style={{ width: 160 }}
            value={to}
            onChange={(e) => setTo(e.target.value)}
            onPressEnter={onSearch}
            allowClear
          />
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
        </Space>

        <Spin spinning={loading}>
          {data ? (
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              {data.period && (
                <Typography.Text type="secondary">
                  기간: {data.period.from ?? '-'} ~ {data.period.to ?? '-'} (
                  {data.period.timezone ?? '-'})
                </Typography.Text>
              )}

              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} xl={6}>
                  <Card size="small" title="생성 작업" style={{ height: '100%' }}>
                    <Row gutter={8}>
                      <Col span={6}>
                        <Statistic
                          title="대기"
                          value={data.generationJobs.queued}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="실행"
                          value={data.generationJobs.running}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="성공"
                          value={data.generationJobs.succeeded}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="실패"
                          value={data.generationJobs.failed}
                          valueStyle={{
                            color:
                              data.generationJobs.failed > 0
                                ? '#cf1322'
                                : undefined,
                          }}
                        />
                      </Col>
                    </Row>
                  </Card>
                </Col>
                <Col xs={24} sm={12} xl={6}>
                  <Card size="small" title="산출물 상태" style={{ height: '100%' }}>
                    <Row gutter={8}>
                      <Col span={6}>
                        <Statistic
                          title="대기"
                          value={data.validation.waitingAssets}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="승인"
                          value={data.validation.approvedAssets}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="반려"
                          value={data.validation.rejectedAssets}
                          valueStyle={{
                            color:
                              data.validation.rejectedAssets > 0
                                ? '#cf1322'
                                : undefined,
                          }}
                        />
                      </Col>
                      <Col span={6}>
                        <Statistic
                          title="숨김"
                          value={data.validation.hiddenAssets}
                        />
                      </Col>
                    </Row>
                  </Card>
                </Col>
                <Col xs={24} sm={12} xl={6}>
                  <Card size="small" title="검증 로그" style={{ height: '100%' }}>
                    <Row gutter={8}>
                      <Col span={8}>
                        <Statistic
                          title="통과"
                          value={data.validation.passCount}
                        />
                      </Col>
                      <Col span={8}>
                        <Statistic
                          title="실패"
                          value={data.validation.failCount}
                        />
                      </Col>
                      <Col span={8}>
                        <Statistic
                          title="검토"
                          value={data.validation.needsReviewCount}
                        />
                      </Col>
                    </Row>
                  </Card>
                </Col>
                <Col xs={24} sm={12} xl={6}>
                  <Card size="small" title="Q&amp;A" style={{ height: '100%' }}>
                    <Row gutter={8}>
                      <Col span={6}>
                        <Statistic title="요청" value={data.qa.requested} />
                      </Col>
                      <Col span={6}>
                        <Statistic title="응답" value={data.qa.answered} />
                      </Col>
                      <Col span={6}>
                        <Statistic title="차단" value={data.qa.blocked} />
                      </Col>
                      <Col span={6}>
                        <Statistic title="실패" value={data.qa.failed} />
                      </Col>
                    </Row>
                  </Card>
                </Col>
              </Row>

              <Row gutter={[16, 16]}>
                <Col xs={24} md={12}>
                  <Card size="small" title="검증 로그 실패 사유">
                    <Table
                      rowKey="resultCode"
                      size="small"
                      pagination={false}
                      columns={failureCols}
                      dataSource={data.validation.failureReasons ?? []}
                      locale={{ emptyText: '없음' }}
                    />
                  </Card>
                </Col>
                <Col xs={24} md={12}>
                  <Card size="small" title="Q&amp;A 차단 사유">
                    <Table
                      rowKey="blockedReason"
                      size="small"
                      pagination={false}
                      columns={blockedCols}
                      dataSource={data.qa.blockedReasons ?? []}
                      locale={{ emptyText: '없음' }}
                    />
                  </Card>
                </Col>
              </Row>

              <Card
                size="small"
                title={`배치 실행 (성공 ${data.batchRuns.succeeded} · 부분실패 ${data.batchRuns.partialFailed} · 실패 ${data.batchRuns.failed})`}
                extra={
                  <Button
                    type="link"
                    size="small"
                    onClick={() => navigate('/ai-batch-logs')}
                  >
                    전체 로그 보기 <ArrowRightOutlined />
                  </Button>
                }
              >
                <Table
                  rowKey="id"
                  size="small"
                  pagination={false}
                  columns={batchFailCols}
                  dataSource={data.batchRuns.latestFailures ?? []}
                  locale={{ emptyText: '최근 실패 없음' }}
                  scroll={{ x: 'max-content' }}
                />
              </Card>

              <Card
                size="small"
                title="체크리스트 통과율"
                extra={
                  <Button
                    type="link"
                    size="small"
                    onClick={() => navigate('/ai-checklists')}
                  >
                    체크리스트 관리 <ArrowRightOutlined />
                  </Button>
                }
              >
                <Table
                  rowKey="checklistType"
                  size="small"
                  pagination={false}
                  columns={checklistCols}
                  dataSource={data.checklists ?? []}
                  locale={{ emptyText: '없음' }}
                />
              </Card>
            </Space>
          ) : (
            !loading && (
              <Typography.Text type="secondary">
                데이터가 없습니다.
              </Typography.Text>
            )
          )}
        </Spin>
      </Space>
    </Card>
  );
}
