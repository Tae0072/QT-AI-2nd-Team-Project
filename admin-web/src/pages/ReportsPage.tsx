import { useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Select,
  Button,
  Tooltip,
  Modal,
  Input,
  Checkbox,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined } from '@ant-design/icons';
import {
  listReports,
  resolveReport,
  rejectReport,
  type Report,
  type ReportListParams,
} from '../api/reports';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-04 신고 처리 =====
// 목록 + 필터(상태/대상 유형) + 서버 페이지네이션 + 처리(resolve)/반려(reject) 모달.
// 권한: OPERATOR / SUPER_ADMIN (백엔드 requireOperator 에서 검증).

const STATUS_OPTIONS = [
  { label: '접수(RECEIVED)', value: 'RECEIVED' },
  { label: '검토중(REVIEWING)', value: 'REVIEWING' },
  { label: '처리완료(RESOLVED)', value: 'RESOLVED' },
  { label: '반려(REJECTED)', value: 'REJECTED' },
];

const TARGET_TYPE_OPTIONS = [
  { label: '나눔글(POST)', value: 'POST' },
  { label: '댓글(COMMENT)', value: 'COMMENT' },
  { label: 'AI Q&A(AI_QA_REQUEST)', value: 'AI_QA_REQUEST' },
  { label: 'AI 산출물(AI_ASSET)', value: 'AI_ASSET' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    RECEIVED: { color: 'blue', text: '접수' },
    REVIEWING: { color: 'gold', text: '검토중' },
    RESOLVED: { color: 'green', text: '처리완료' },
    REJECTED: { color: 'red', text: '반려' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

const isOpenStatus = (s: string) => s === 'RECEIVED' || s === 'REVIEWING';

export default function ReportsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<Report, ReportListParams>(listReports, { page: 0, size: 20 });

  const [status, setStatus] = useState<string | undefined>(undefined);
  const [targetType, setTargetType] = useState<string | undefined>(undefined);

  // 처리/반려 모달 상태
  const [action, setAction] = useState<{
    mode: 'resolve' | 'reject';
    report: Report;
  } | null>(null);
  const [reason, setReason] = useState('');
  const [notify, setNotify] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const onSearch = () =>
    applyFilters({
      status: status || undefined,
      targetType: targetType || undefined,
    });

  const onReset = () => {
    setStatus(undefined);
    setTargetType(undefined);
    applyFilters({ status: undefined, targetType: undefined });
  };

  const openAction = (mode: 'resolve' | 'reject', report: Report) => {
    setAction({ mode, report });
    setReason('');
    setNotify(true);
  };

  const submitAction = async () => {
    if (!action) return;
    setSubmitting(true);
    try {
      const payload = {
        reason: reason.trim() || undefined,
        notifyReporter: notify,
      };
      if (action.mode === 'resolve') {
        await resolveReport(action.report.id, payload);
        message.success('신고를 처리(인정)했습니다.');
      } else {
        await rejectReport(action.report.id, payload);
        message.success('신고를 반려했습니다.');
      }
      setAction(null);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<Report> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '신고일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '대상',
      width: 180,
      render: (_, r) =>
        `${r.targetType}${r.targetId != null ? ` #${r.targetId}` : ''}`,
    },
    {
      title: '사유',
      dataIndex: 'reason',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '신고자',
      dataIndex: 'reporterMemberId',
      width: 90,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 110,
      render: (v: string) => statusTag(v),
    },
    {
      title: '처리',
      width: 190,
      render: (_, r) =>
        r.processedAt
          ? `${formatDateTime(r.processedAt)}${r.processedByAdminId != null ? ` (admin #${r.processedByAdminId})` : ''}`
          : '-',
    },
    {
      title: '액션',
      width: 150,
      fixed: 'right',
      render: (_, r) =>
        isOpenStatus(r.status) ? (
          <Space>
            <Button
              size="small"
              type="primary"
              onClick={() => openAction('resolve', r)}
            >
              처리
            </Button>
            <Button size="small" danger onClick={() => openAction('reject', r)}>
              반려
            </Button>
          </Space>
        ) : (
          <Typography.Text type="secondary">완료</Typography.Text>
        ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-04</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            신고 처리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          나눔 글·댓글 신고를 검토하고 처리(인정)하거나 반려합니다. 권한:
          OPERATOR / SUPER_ADMIN.
        </Typography.Paragraph>

        <Space wrap>
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 180 }}
            value={status}
            onChange={(v) => setStatus(v)}
            options={STATUS_OPTIONS}
          />
          <Select
            placeholder="대상 유형"
            allowClear
            style={{ width: 210 }}
            value={targetType}
            onChange={(v) => setTargetType(v)}
            options={TARGET_TYPE_OPTIONS}
          />
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
        </Space>

        <Table<Report>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Typography.Paragraph
                style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}
              >
                <Typography.Text strong>상세 내용: </Typography.Text>
                {r.detail ?? '-'}
              </Typography.Paragraph>
            ),
            rowExpandable: (r) => Boolean(r.detail),
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

      <Modal
        open={action !== null}
        title={action?.mode === 'resolve' ? '신고 처리(인정)' : '신고 반려'}
        okText={action?.mode === 'resolve' ? '처리' : '반려'}
        okButtonProps={{ danger: action?.mode === 'reject', loading: submitting }}
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitAction}
        onCancel={() => setAction(null)}
        destroyOnClose
      >
        {action && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              대상 {action.report.targetType}
              {action.report.targetId != null
                ? ` #${action.report.targetId}`
                : ''}{' '}
              · 신고 #{action.report.id}
            </Typography.Text>
            <div>
              <Typography.Text>처리 사유 (선택)</Typography.Text>
              <Input.TextArea
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="처리/반려 사유를 남길 수 있습니다."
                style={{ marginTop: 4 }}
              />
            </div>
            <Checkbox
              checked={notify}
              onChange={(e) => setNotify(e.target.checked)}
            >
              신고자에게 결과 알림 보내기
            </Checkbox>
          </Space>
        )}
      </Modal>
    </Card>
  );
}
