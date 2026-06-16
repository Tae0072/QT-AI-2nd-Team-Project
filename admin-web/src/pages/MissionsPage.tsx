import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  changeMissionStatus,
  createMission,
  listMissions,
  updateMission,
  type AdminMission,
  type MissionCreateRequest,
  type MissionMetricType,
  type MissionPeriodType,
  type MissionUpdateRequest,
} from '../api/missions';
import { formatDateTime } from '../utils/datetime';

// ===== AD-16 미션 관리 (F-13) =====
// 미션 정의 목록 + 생성·수정 + 노출 상태(ACTIVE/HIDDEN) 토글.
// 권한: CONTENT_CREATOR / OPERATOR / SUPER_ADMIN.

const METRIC_OPTIONS: { label: string; value: MissionMetricType }[] = [
  { label: '묵상 저장 일수', value: 'MEDITATION_SAVED_DAYS' },
  { label: '저장 노트 개수', value: 'NOTE_SAVED_COUNT' },
  { label: '연속 묵상 일수', value: 'STREAK_DAYS' },
];

const PERIOD_OPTIONS: { label: string; value: MissionPeriodType }[] = [
  { label: '일간', value: 'DAILY' },
  { label: '주간', value: 'WEEKLY' },
  { label: '월간', value: 'MONTHLY' },
];

function metricLabel(v: string) {
  return METRIC_OPTIONS.find((o) => o.value === v)?.label ?? v;
}
function periodLabel(v: string) {
  return PERIOD_OPTIONS.find((o) => o.value === v)?.label ?? v;
}

export default function MissionsPage() {
  const [rows, setRows] = useState<AdminMission[]>([]);
  const [loading, setLoading] = useState(false);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm<MissionCreateRequest>();
  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AdminMission | null>(null);
  const [editForm] = Form.useForm<MissionUpdateRequest>();
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    listMissions()
      .then(setRows)
      .catch((e: unknown) =>
        message.error(e instanceof Error ? e.message : '목록을 불러오지 못했습니다.'),
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    createForm.resetFields();
    setCreateOpen(true);
  };

  const submitCreate = async () => {
    let values: MissionCreateRequest;
    try {
      values = await createForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await createMission(values);
      message.success('미션을 생성했습니다.');
      setCreateOpen(false);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const openEdit = (record: AdminMission) => {
    setEditTarget(record);
    editForm.setFieldsValue({
      title: record.title,
      metricType: record.metricType,
      periodType: record.periodType,
      targetCount: record.targetCount,
    });
    setEditOpen(true);
  };

  const submitEdit = async () => {
    if (!editTarget) return;
    let values: MissionUpdateRequest;
    try {
      values = await editForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateMission(editTarget.id, values);
      message.success('미션을 수정했습니다.');
      setEditOpen(false);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '수정에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const toggleStatus = async (record: AdminMission, active: boolean) => {
    try {
      await changeMissionStatus(record.id, active ? 'ACTIVE' : 'HIDDEN');
      message.success(active ? '미션을 노출했습니다.' : '미션을 숨겼습니다.');
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '상태 변경에 실패했습니다.');
    }
  };

  const columns: ColumnsType<AdminMission> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '코드', dataIndex: 'code', width: 160 },
    { title: '제목', dataIndex: 'title' },
    {
      title: '지표',
      dataIndex: 'metricType',
      width: 130,
      render: (v: string) => metricLabel(v),
    },
    {
      title: '주기',
      dataIndex: 'periodType',
      width: 90,
      render: (v: string) => periodLabel(v),
    },
    { title: '목표', dataIndex: 'targetCount', width: 80 },
    {
      title: '노출',
      dataIndex: 'status',
      width: 90,
      render: (v: string, record: AdminMission) => (
        <Switch
          checked={v === 'ACTIVE'}
          size="small"
          onChange={(checked) => toggleStatus(record, checked)}
        />
      ),
    },
    {
      title: '수정일',
      dataIndex: 'updatedAt',
      width: 160,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      key: 'actions',
      width: 80,
      render: (_: unknown, record: AdminMission) => (
        <Tooltip title="수정">
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
        </Tooltip>
      ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-16</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            미션 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          마이페이지 미션의 정의(지표·주기·목표)를 생성·수정하고 노출 상태를 관리합니다.
          권한: CONTENT_CREATOR / OPERATOR / SUPER_ADMIN.
        </Typography.Paragraph>

        <Space wrap>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={load} />
          </Tooltip>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            미션 생성
          </Button>
        </Space>

        <Table<AdminMission>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          pagination={{ pageSize: 20, showTotal: (t: number) => `총 ${t}개` }}
        />
      </Space>

      <Modal
        open={createOpen}
        title="미션 생성"
        okText="생성"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form
          form={createForm}
          layout="vertical"
          initialValues={{ metricType: 'MEDITATION_SAVED_DAYS', periodType: 'MONTHLY', targetCount: 1 }}
        >
          <Form.Item
            name="code"
            label="코드"
            rules={[{ required: true, message: '코드를 입력하세요' }]}
          >
            <Input maxLength={50} placeholder="예: MONTHLY_MEDITATION" />
          </Form.Item>
          <Form.Item
            name="title"
            label="제목"
            rules={[{ required: true, message: '제목을 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="미션 제목" />
          </Form.Item>
          <Form.Item
            name="metricType"
            label="지표"
            rules={[{ required: true, message: '지표를 선택하세요' }]}
          >
            <Select options={METRIC_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="periodType"
            label="주기"
            rules={[{ required: true, message: '주기를 선택하세요' }]}
          >
            <Select options={PERIOD_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="targetCount"
            label="목표 수치"
            rules={[{ required: true, message: '목표 수치를 입력하세요' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={editOpen}
        title="미션 수정"
        okText="저장"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitEdit}
        onCancel={() => setEditOpen(false)}
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="title"
            label="제목"
            rules={[{ required: true, message: '제목을 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="미션 제목" />
          </Form.Item>
          <Form.Item name="metricType" label="지표">
            <Select options={METRIC_OPTIONS} />
          </Form.Item>
          <Form.Item name="periodType" label="주기">
            <Select options={PERIOD_OPTIONS} />
          </Form.Item>
          <Form.Item name="targetCount" label="목표 수치">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
