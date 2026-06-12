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
  Form,
  Popconfirm,
  Alert,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import {
  listAiChecklists,
  createAiChecklist,
  activateAiChecklist,
  retireAiChecklist,
  type AiChecklist,
  type AiChecklistListParams,
  type ChecklistType,
  type ChecklistStatus,
} from '../api/aiChecklists';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-09 AI 검증 체크리스트 관리 =====
// 목록 + 필터(유형/상태) + 등록 + 활성화/폐기. 권한: REVIEWER / SUPER_ADMIN.
// 검증 체크리스트 '버전'은 AI 산출물 승인 게이트의 기준이 된다(메타데이터만 다룸).

const TYPE_OPTIONS = [
  { label: '해설(EXPLANATION)', value: 'EXPLANATION' },
  { label: '시뮬레이터(SIMULATOR)', value: 'SIMULATOR' },
  { label: 'Q&A(QA)', value: 'QA' },
];

const STATUS_OPTIONS = [
  { label: '초안(DRAFT)', value: 'DRAFT' },
  { label: '활성(ACTIVE)', value: 'ACTIVE' },
  { label: '폐기(RETIRED)', value: 'RETIRED' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    DRAFT: { color: 'gold', text: '초안' },
    ACTIVE: { color: 'green', text: '활성' },
    RETIRED: { color: 'default', text: '폐기' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

interface CreateFormValues {
  checklistType: ChecklistType;
  version: string;
  contentHash?: string;
  status?: ChecklistStatus;
}

export default function AiChecklistsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AiChecklist, AiChecklistListParams>(listAiChecklists, {
      page: 0,
      size: 20,
    });

  const [checklistType, setChecklistType] = useState<string | undefined>(
    undefined,
  );
  const [status, setStatus] = useState<string | undefined>(undefined);

  // 등록 모달
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<CreateFormValues>();

  // 행 단위 활성화/폐기 진행 중 표시 (id)
  const [busyId, setBusyId] = useState<number | null>(null);

  const onSearch = () =>
    applyFilters({
      checklistType: checklistType || undefined,
      status: status || undefined,
    });

  const onReset = () => {
    setChecklistType(undefined);
    setStatus(undefined);
    applyFilters({ checklistType: undefined, status: undefined });
  };

  const submitCreate = async () => {
    try {
      const values = await form.validateFields();
      setCreating(true);
      await createAiChecklist({
        checklistType: values.checklistType,
        version: values.version.trim(),
        contentHash: values.contentHash?.trim() || undefined,
        status: values.status,
      });
      message.success('체크리스트 버전을 등록했습니다.');
      setCreateOpen(false);
      form.resetFields();
      reload();
    } catch (e) {
      // validateFields 실패(폼 검증)면 안내 없이 모달 유지
      if (e instanceof Error && e.message) {
        message.error(e.message);
      }
    } finally {
      setCreating(false);
    }
  };

  const doActivate = async (row: AiChecklist) => {
    setBusyId(row.id);
    try {
      await activateAiChecklist(row.id);
      message.success(`#${row.id} 버전을 활성화했습니다.`);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '활성화에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const doRetire = async (row: AiChecklist) => {
    setBusyId(row.id);
    try {
      await retireAiChecklist(row.id);
      message.success(`#${row.id} 버전을 폐기했습니다.`);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '폐기에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const columns: ColumnsType<AiChecklist> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '유형',
      dataIndex: 'checklistType',
      width: 150,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    { title: '버전', dataIndex: 'version', width: 140 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => statusTag(v),
    },
    {
      title: '콘텐츠 해시',
      dataIndex: 'contentHash',
      width: 200,
      ellipsis: true,
      render: (v: string | null) =>
        v ? <Typography.Text code>{v}</Typography.Text> : '-',
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '활성/폐기',
      width: 170,
      render: (_, r) => (
        <Space direction="vertical" size={0}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            활성 {formatDateTime(r.activatedAt)}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            폐기 {formatDateTime(r.retiredAt)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '액션',
      width: 170,
      fixed: 'right',
      render: (_, r) => {
        if (r.status === 'RETIRED') {
          return <Typography.Text type="secondary">-</Typography.Text>;
        }
        return (
          <Space>
            {r.status === 'DRAFT' && (
              <Popconfirm
                title="이 버전을 활성화할까요?"
                description="같은 유형의 기존 활성 버전은 교체될 수 있습니다."
                okText="활성화"
                cancelText="취소"
                onConfirm={() => doActivate(r)}
              >
                <Button size="small" type="primary" loading={busyId === r.id}>
                  활성화
                </Button>
              </Popconfirm>
            )}
            <Popconfirm
              title="이 버전을 폐기할까요?"
              okText="폐기"
              okButtonProps={{ danger: true }}
              cancelText="취소"
              onConfirm={() => doRetire(r)}
            >
              <Button size="small" danger loading={busyId === r.id}>
                폐기
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center" wrap>
          <Tag color="blue">AD-09</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 검증 체크리스트 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          AI 산출물 검증·승인의 기준이 되는 체크리스트 버전을 등록하고
          활성화/폐기합니다. 권한: REVIEWER / SUPER_ADMIN. (버전·해시·상태 등
          메타데이터만 다루며, 체크 항목 원문은 노출하지 않습니다.)
        </Typography.Paragraph>

        <Alert
          type="info"
          showIcon
          message="유형별로 활성(ACTIVE) 버전은 하나입니다."
          description="새 버전을 활성화하면 같은 유형의 기존 활성 버전이 교체될 수 있습니다. 신중히 진행하세요."
        />

        <Space wrap>
          <Select
            placeholder="유형"
            allowClear
            style={{ width: 200 }}
            value={checklistType}
            onChange={(v) => setChecklistType(v)}
            options={TYPE_OPTIONS}
          />
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 180 }}
            value={status}
            onChange={(v) => setStatus(v)}
            options={STATUS_OPTIONS}
          />
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              setCreateOpen(true);
            }}
          >
            버전 등록
          </Button>
        </Space>

        <Table<AiChecklist>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
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
        open={createOpen}
        title="검증 체크리스트 버전 등록"
        okText="등록"
        cancelText="취소"
        confirmLoading={creating}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form<CreateFormValues>
          form={form}
          layout="vertical"
          initialValues={{ status: 'DRAFT' }}
          preserve={false}
        >
          <Form.Item
            name="checklistType"
            label="유형"
            rules={[{ required: true, message: '유형을 선택하세요.' }]}
          >
            <Select placeholder="유형 선택" options={TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="version"
            label="버전"
            rules={[{ required: true, message: '버전을 입력하세요.' }]}
            extra="예: 2026-06-10 또는 v1.2 같은 식별 가능한 버전 문자열"
          >
            <Input placeholder="버전 문자열" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="contentHash"
            label="콘텐츠 해시 (선택)"
            extra="체크리스트 내용의 무결성 확인용 해시. 비워두면 서버 정책에 따릅니다."
          >
            <Input placeholder="예: sha256:..." maxLength={128} />
          </Form.Item>
          <Form.Item
            name="status"
            label="초기 상태"
            extra="기본은 초안(DRAFT)입니다. 등록 후 목록에서 활성화할 수 있습니다."
          >
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
