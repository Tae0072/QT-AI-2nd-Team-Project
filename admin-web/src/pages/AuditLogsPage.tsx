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
  listAuditLogs,
  type AuditLog,
  type AuditLogListParams,
} from '../api/auditLogs';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-07 감사 로그 (읽기 전용) =====
// GET /api/v1/admin/audit-logs — 표 + 필터(행위자/액션/기간) + 서버 페이지네이션.
// 행 펼치기로 변경 전/후(JSON)를 본다. 권한: SUPER_ADMIN (백엔드에서 검증).

const ACTOR_TYPE_OPTIONS = [
  { label: 'ADMIN', value: 'ADMIN' },
  { label: 'SYSTEM_BATCH', value: 'SYSTEM_BATCH' },
  { label: 'USER', value: 'USER' },
];

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

export default function AuditLogsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AuditLog, AuditLogListParams>(listAuditLogs, {
      page: 0,
      size: 20,
    });

  // 필터 입력 로컬 상태 (조회 버튼을 눌러야 적용)
  const [actorType, setActorType] = useState<string | undefined>(undefined);
  const [actionType, setActionType] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const onSearch = () => {
    applyFilters({
      actorType: actorType || undefined,
      actionType: actionType.trim() || undefined,
      from: from.trim() || undefined,
      to: to.trim() || undefined,
    });
  };

  const onReset = () => {
    setActorType(undefined);
    setActionType('');
    setFrom('');
    setTo('');
    applyFilters({
      actorType: undefined,
      actionType: undefined,
      from: undefined,
      to: undefined,
    });
  };

  const columns: ColumnsType<AuditLog> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '시각',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '행위자',
      width: 220,
      render: (_, r) => (
        <Space size={4}>
          <Tag
            color={
              r.actorType === 'ADMIN'
                ? 'geekblue'
                : r.actorType === 'SYSTEM_BATCH'
                  ? 'gold'
                  : 'default'
            }
          >
            {r.actorType ?? '-'}
          </Tag>
          <span>
            {r.actorLabel ?? (r.actorId != null ? `#${r.actorId}` : '-')}
          </span>
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
        r.targetType
          ? `${r.targetType}${r.targetId != null ? ` #${r.targetId}` : ''}`
          : '-',
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-07</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            감사 로그
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          관리자/시스템(SYSTEM_BATCH)의 주요 행위 기록을 조회·필터링합니다. (읽기
          전용)
        </Typography.Paragraph>

        {/* 필터 */}
        <Space wrap>
          <Select
            placeholder="행위자 유형"
            allowClear
            style={{ width: 160 }}
            value={actorType}
            onChange={(v) => setActorType(v)}
            options={ACTOR_TYPE_OPTIONS}
          />
          <Input
            placeholder="액션 (예: AI_ASSET_APPROVE)"
            style={{ width: 240 }}
            value={actionType}
            onChange={(e) => setActionType(e.target.value)}
            onPressEnter={onSearch}
            allowClear
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

        <Table<AuditLog>
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
                  <Typography.Text strong>변경 전</Typography.Text>
                  <pre style={preStyle}>{r.beforeJson ?? '-'}</pre>
                </div>
                <div>
                  <Typography.Text strong>변경 후</Typography.Text>
                  <pre style={preStyle}>{r.afterJson ?? '-'}</pre>
                </div>
              </Space>
            ),
            rowExpandable: (r) => Boolean(r.beforeJson || r.afterJson),
          }}
          pagination={{
            current: page + 1, // antd는 1-based, 서버는 0-based
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
