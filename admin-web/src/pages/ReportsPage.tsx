import { useState } from 'react';
import {
  App,
  Button,
  Card,
  Checkbox,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useList } from '../hooks/useList';
import {
  listReports,
  rejectReport,
  resolveReport,
  type Report,
  type ReportListParams,
} from '../api/reports';

// 신고 상태 → 화면 라벨·색. (코드만 보면 헷갈리니 사람이 읽을 형태로)
const STATUS_META: Record<string, { label: string; color: string }> = {
  RECEIVED: { label: '접수', color: 'gold' },
  RESOLVED: { label: '처리됨', color: 'green' },
  REJECTED: { label: '반려', color: 'default' },
};

// 처리(resolve) 시 취할 조치 (04 §4.7.4).
const RESOLVE_ACTIONS = [
  { value: 'HIDE_TARGET', label: '대상 숨김' },
  { value: 'SUSPEND_USER', label: '작성자 제재' },
  { value: 'NO_ACTION', label: '조치 없음' },
];

const STATUS_FILTER = ['RECEIVED', 'RESOLVED', 'REJECTED'];
const TARGET_FILTER = ['POST', 'COMMENT', 'AI_QA_REQUEST', 'AI_ASSET'];

function formatDateTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-';
}

// 처리/반려 모달이 어떤 신고를 대상으로, 어떤 동작으로 열렸는지 담는 상태 타입.
type ActionTarget = { report: Report; type: 'resolve' | 'reject' } | null;

// AD-04 신고 처리 — 신고 목록 조회·필터 + 행별 처리/반려(모달).
export default function ReportsPage() {
  const { message } = App.useApp();
  // 목록은 AD-07과 동일하게 공통 훅 재사용(데이터·로딩·필터·페이징).
  const { rows, loading, data, params, setParams, reload } = useList<
    Report,
    ReportListParams
  >(listReports, { page: 0, size: 20 });

  // 필터 폼 제출 → params 반영(첫 페이지부터).
  const [filterForm] = Form.useForm();
  const onFilter = (v: { status?: string; targetType?: string }) => {
    setParams({ status: v.status, targetType: v.targetType, page: 0 });
  };

  // 처리/반려 모달 상태 + 모달 안 입력 폼.
  const [actionTarget, setActionTarget] = useState<ActionTarget>(null);
  const [modalForm] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const openAction = (report: Report, type: 'resolve' | 'reject') => {
    modalForm.resetFields(); // 이전 입력이 남지 않게 매번 초기화
    setActionTarget({ report, type });
  };

  // 모달 [확인]: 폼 검증 → resolve/reject API → 목록 새로고침.
  const onSubmitAction = async () => {
    if (!actionTarget) return;
    let values: { action?: string; reason?: string; notifyReporter?: boolean };
    try {
      values = await modalForm.validateFields(); // 검증 실패면 모달 유지
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      if (actionTarget.type === 'resolve') {
        await resolveReport(actionTarget.report.id, {
          action: values.action,
          reason: values.reason,
          notifyReporter: values.notifyReporter ?? false,
        });
        message.success('신고를 처리했습니다.');
      } else {
        await rejectReport(actionTarget.report.id, { reason: values.reason });
        message.success('신고를 반려했습니다.');
      }
      setActionTarget(null);
      reload(); // 처리 결과(상태 변경)를 목록에 반영
    } catch (e) {
      message.error(e instanceof Error ? e.message : '처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  // 컬럼은 액션 버튼이 openAction(클로저)을 써야 해서 컴포넌트 안에 둔다.
  const columns: ColumnsType<Report> = [
    { title: '신고시각', dataIndex: 'createdAt', key: 'createdAt', width: 150, render: formatDateTime },
    {
      title: '대상',
      key: 'target',
      width: 160,
      render: (_, r) => `${r.targetType}${r.targetId != null ? ` #${r.targetId}` : ''}`,
    },
    { title: '사유', dataIndex: 'reason', key: 'reason', width: 140 },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: string) => {
        const meta = STATUS_META[s] ?? { label: s, color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      // 접수(RECEIVED) 상태만 처리/반려 가능. 이미 처리/반려된 건 버튼 숨김.
      title: '액션',
      key: 'action',
      width: 150,
      render: (_, r) =>
        r.status === 'RECEIVED' ? (
          <Space>
            <Button size="small" type="primary" onClick={() => openAction(r, 'resolve')}>
              처리
            </Button>
            <Button size="small" danger onClick={() => openAction(r, 'reject')}>
              반려
            </Button>
          </Space>
        ) : (
          <span style={{ color: '#aaa' }}>—</span>
        ),
    },
  ];

  const isResolve = actionTarget?.type === 'resolve';

  return (
    <Card title="AD-04 신고 처리">
      {/* 필터: 상태·대상 유형을 골라 [조회]. 빈 값이면 전체. */}
      <Form form={filterForm} layout="inline" onFinish={onFilter} style={{ marginBottom: 16 }}>
        <Form.Item name="status" label="상태">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 140 }}
            options={STATUS_FILTER.map((s) => ({ value: s, label: STATUS_META[s]?.label ?? s }))}
          />
        </Form.Item>
        <Form.Item name="targetType" label="대상">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 160 }}
            options={TARGET_FILTER.map((t) => ({ value: t, label: t }))}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">
            조회
          </Button>
        </Form.Item>
      </Form>

      <Table<Report>
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        scroll={{ x: true }}
        pagination={{
          current: (params.page ?? 0) + 1, // 백엔드 0-based ↔ antd 1-based
          pageSize: params.size ?? 20,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          showTotal: (t) => `총 ${t}건`,
          onChange: (page, pageSize) => setParams({ page: page - 1, size: pageSize }),
        }}
      />

      {/* 처리/반려 공용 모달 — type에 따라 입력 항목이 달라진다. */}
      <Modal
        open={actionTarget != null}
        title={isResolve ? '신고 처리' : '신고 반려'}
        okText={isResolve ? '처리' : '반려'}
        confirmLoading={submitting}
        onOk={onSubmitAction}
        onCancel={() => setActionTarget(null)}
        destroyOnClose
      >
        <Form form={modalForm} layout="vertical">
          {isResolve && (
            <Form.Item
              name="action"
              label="조치"
              rules={[{ required: true, message: '조치를 선택하세요' }]}
            >
              <Select placeholder="조치 선택" options={RESOLVE_ACTIONS} />
            </Form.Item>
          )}
          <Form.Item
            name="reason"
            label="사유"
            rules={[{ required: true, message: '사유를 입력하세요' }]}
          >
            <Input.TextArea rows={3} placeholder="처리/반려 사유" />
          </Form.Item>
          {isResolve && (
            <Form.Item name="notifyReporter" valuePropName="checked" initialValue={false}>
              <Checkbox>신고자에게 처리 결과 알림</Checkbox>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </Card>
  );
}
