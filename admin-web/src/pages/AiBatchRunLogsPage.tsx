import { useState, type CSSProperties } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Input,
  Select,
  Button,
  Tooltip,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined } from '@ant-design/icons';
import {
  listAiBatchRunLogs,
  type AiBatchRunLog,
  type AiBatchRunLogListParams,
} from '../api/aiBatchRunLogs';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-10 AI 배치 실행 로그 (읽기 전용) =====
// GET /api/v1/admin/ai/batch-run-logs — 표 + 필터(배치명/상태/기간) + 서버 페이지네이션.
// 행 펼치기로 오류 유형/메시지를 본다. 권한: OPERATOR / REVIEWER / SUPER_ADMIN.

const STATUS_OPTIONS = [
  { label: '성공(SUCCEEDED)', value: 'SUCCEEDED' },
  { label: '부분실패(PARTIAL_FAILED)', value: 'PARTIAL_FAILED' },
  { label: '실패(FAILED)', value: 'FAILED' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    SUCCEEDED: { color: 'green', text: '성공' },
    PARTIAL_FAILED: { color: 'gold', text: '부분실패' },
    FAILED: { color: 'red', text: '실패' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

const preStyle: CSSProperties = {
  margin: '4px 0 0',
  padding: 8,
  background: '#f6f8fa',
  borderRadius: 6,
  maxHeight: 220,
  overflow: 'auto',
  fontSize: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-all',
};

export default function AiBatchRunLogsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AiBatchRunLog, AiBatchRunLogListParams>(listAiBatchRunLogs, {
      page: 0,
      size: 20,
    });

  // 필터 입력 로컬 상태 (조회 버튼을 눌러야 적용)
  const [batchName, setBatchName] = useState('');
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const onSearch = () =>
    applyFilters({
      batchName: batchName.trim() || undefined,
      status: status || undefined,
      from: from.trim() || undefined,
      to: to.trim() || undefined,
    });

  const onReset = () => {
    setBatchName('');
    setStatus(undefined);
    setFrom('');
    setTo('');
    applyFilters({
      batchName: undefined,
      status: undefined,
      from: undefined,
      to: undefined,
    });
  };

  const columns: ColumnsType<AiBatchRunLog> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '시각',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    { title: '배치명', dataIndex: 'batchName', width: 220 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 110,
      render: (v: string) => statusTag(v),
    },
    {
      title: '처리/생성/실패',
      width: 150,
      align: 'right',
      render: (_, r) => (
        <span>
          {r.processedCount} / {r.createdCount} /{' '}
          <Typography.Text type={r.failedCount > 0 ? 'danger' : undefined}>
            {r.failedCount}
          </Typography.Text>
        </span>
      ),
    },
    {
      title: '시작',
      dataIndex: 'startedAt',
      width: 160,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '종료',
      dataIndex: 'finishedAt',
      width: 160,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '오류유형',
      dataIndex: 'errorType',
      width: 150,
      render: (v: string | null) =>
        v ? <Tag color="red">{v}</Tag> : <Typography.Text type="secondary">-</Typography.Text>,
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-10</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 배치 실행 로그
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          AI 해설·시뮬레이터 사전 생성/검증 배치의 실행 결과를 조회·필터링합니다.
          (읽기 전용) 권한: OPERATOR / REVIEWER / SUPER_ADMIN.
        </Typography.Paragraph>

        {/* 필터 */}
        <Space wrap>
          <Input
            placeholder="배치명 (부분 일치)"
            style={{ width: 220 }}
            value={batchName}
            onChange={(e) => setBatchName(e.target.value)}
            onPressEnter={onSearch}
            allowClear
          />
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 200 }}
            value={status}
            onChange={(v) => setStatus(v)}
            options={STATUS_OPTIONS}
          />
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

        <Table<AiBatchRunLog>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Typography.Text strong>오류 유형</Typography.Text>
                  <pre style={preStyle}>{r.errorType ?? '-'}</pre>
                </div>
                <div>
                  <Typography.Text strong>오류 메시지</Typography.Text>
                  <pre style={preStyle}>{r.errorMessage ?? '-'}</pre>
                </div>
              </Space>
            ),
            rowExpandable: (r) => Boolean(r.errorType || r.errorMessage),
          }}
          pagination={{
            current: page + 1, // antd 1-based, 서버 0-based
            pageSize: size,
            total,
            showSizeChanger: true,
            showTotal: (t) => `총 ${t}건`,
            onChange: (p, ps) => changePage(p - 1, ps),
          }}
        />
      </Space>
    </Card>
  );
}
