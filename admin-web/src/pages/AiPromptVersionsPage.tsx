import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  activateAiPromptVersion,
  createAiPromptVersion,
  getAiPromptVersion,
  listAiPromptVersions,
  retireAiPromptVersion,
  type AiPromptStatus,
  type AiPromptVersion,
  type AiPromptVersionListParams,
} from '../api/aiPromptVersions';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import {
  AI_PROMPT_DEFAULT_STATUS,
  AI_PROMPT_MANAGED_TYPE,
  AI_PROMPT_VERSION_STATUS_TAGS,
  aiPromptVersionListParams,
} from './adminPageContracts';

// ===== AI 프롬프트 관리 =====
// 1차 범위는 EXPLANATION 프롬프트 버전의 등록, 상세 조회, 활성화, 폐기이다.
// 권한: REVIEWER / SUPER_ADMIN.

const STATUS_OPTIONS = [
  { label: '초안(DRAFT)', value: 'DRAFT' },
  { label: '활성(ACTIVE)', value: 'ACTIVE' },
  { label: '폐기(RETIRED)', value: 'RETIRED' },
] satisfies { label: string; value: AiPromptStatus }[];

const promptBlockStyle = {
  margin: 0,
  padding: 12,
  border: '1px solid #f0f0f0',
  borderRadius: 6,
  background: '#fafafa',
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-word',
} as const;

function statusTag(status: string) {
  const m =
    AI_PROMPT_VERSION_STATUS_TAGS[
      status as keyof typeof AI_PROMPT_VERSION_STATUS_TAGS
    ] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

interface CreatePromptFormValues {
  version: string;
  systemPrompt: string;
  userPromptTemplate: string;
  modelName?: string;
  temperature: number;
  maxTokens: number;
  description?: string;
}

export default function AiPromptVersionsPage() {
  const { rows, page, size, total, loading, error, applyFilters, changePage, reload } =
    usePagedList<AiPromptVersion, AiPromptVersionListParams>(listAiPromptVersions, {
      page: 0,
      size: 20,
      ...aiPromptVersionListParams(AI_PROMPT_DEFAULT_STATUS),
    });

  const [status, setStatus] = useState<AiPromptStatus | undefined>(
    AI_PROMPT_DEFAULT_STATUS,
  );
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<CreatePromptFormValues>();
  const [busyId, setBusyId] = useState<number | null>(null);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<AiPromptVersion | null>(null);

  const onSearch = () =>
    applyFilters(aiPromptVersionListParams(status || undefined));

  const onReset = () => {
    setStatus(AI_PROMPT_DEFAULT_STATUS);
    applyFilters(aiPromptVersionListParams(AI_PROMPT_DEFAULT_STATUS));
  };

  const openDetail = async (row: AiPromptVersion) => {
    setDetailOpen(true);
    setDetail(row);
    setDetailLoading(true);
    try {
      setDetail(await getAiPromptVersion(row.id));
    } catch (e) {
      message.error(e instanceof Error ? e.message : '상세 조회에 실패했습니다.');
    } finally {
      setDetailLoading(false);
    }
  };

  const submitCreate = async () => {
    let values: CreatePromptFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setCreating(true);
    try {
      await createAiPromptVersion({
        promptType: AI_PROMPT_MANAGED_TYPE,
        version: values.version.trim(),
        systemPrompt: values.systemPrompt.trim(),
        userPromptTemplate: values.userPromptTemplate.trim(),
        modelName: values.modelName?.trim() || undefined,
        temperature: values.temperature,
        maxTokens: values.maxTokens,
        description: values.description?.trim() || undefined,
      });
      message.success('프롬프트 버전을 등록했습니다.');
      setCreateOpen(false);
      form.resetFields();
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '등록에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const doActivate = async (row: AiPromptVersion) => {
    setBusyId(row.id);
    try {
      const updated = await activateAiPromptVersion(row.id);
      message.success(`#${row.id} 프롬프트 버전을 활성화했습니다.`);
      setDetail((prev) => (prev?.id === updated.id ? updated : prev));
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '활성화에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const doRetire = async (row: AiPromptVersion) => {
    setBusyId(row.id);
    try {
      const updated = await retireAiPromptVersion(row.id);
      message.success(`#${row.id} 프롬프트 버전을 폐기했습니다.`);
      setDetail((prev) => (prev?.id === updated.id ? updated : prev));
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '폐기에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const columns: ColumnsType<AiPromptVersion> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '유형',
      dataIndex: 'promptType',
      width: 140,
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
      title: '모델',
      dataIndex: 'modelName',
      width: 160,
      render: (v: string | null) => v || '-',
    },
    {
      title: '설정',
      width: 150,
      render: (_, r) => (
        <Typography.Text>
          temp {r.temperature} / {r.maxTokens}
        </Typography.Text>
      ),
    },
    {
      title: '콘텐츠 해시',
      dataIndex: 'contentHash',
      width: 180,
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
      title: '액션',
      width: 220,
      fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button size="small" onClick={() => openDetail(r)}>
            상세
          </Button>
          {r.status === 'DRAFT' && (
            <Popconfirm
              title="이 프롬프트 버전을 활성화할까요?"
              description="서버는 최근 평가 실행 통과 여부를 기준으로 활성화를 검증합니다."
              okText="활성화"
              cancelText="취소"
              onConfirm={() => doActivate(r)}
            >
              <Button size="small" type="primary" loading={busyId === r.id}>
                활성화
              </Button>
            </Popconfirm>
          )}
          {r.status !== 'RETIRED' && (
            <Popconfirm
              title="이 프롬프트 버전을 폐기할까요?"
              okText="폐기"
              okButtonProps={{ danger: true }}
              cancelText="취소"
              onConfirm={() => doRetire(r)}
            >
              <Button size="small" danger loading={busyId === r.id}>
                폐기
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center" wrap>
          <Tag color="blue">AI</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 프롬프트 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          EXPLANATION 프롬프트 버전을 등록하고 평가 통과 후 활성화합니다. 권한:
          REVIEWER / SUPER_ADMIN.
        </Typography.Paragraph>

        {error && (
          <Alert
            type="error"
            showIcon
            message="프롬프트 목록을 불러오지 못했습니다"
            description={error}
            action={
              <Button size="small" onClick={reload}>
                재시도
              </Button>
            }
          />
        )}

        <Space wrap>
          <Select
            value={AI_PROMPT_MANAGED_TYPE}
            style={{ width: 190 }}
            disabled
            options={[{ label: '해설(EXPLANATION)', value: AI_PROMPT_MANAGED_TYPE }]}
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
            프롬프트 등록
          </Button>
        </Space>

        <Table<AiPromptVersion>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Typography.Text type="secondary">
                  설명: {r.description || '-'}
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  등록자 #{r.createdByAdminId ?? '-'} · 활성{' '}
                  {formatDateTime(r.activatedAt)} · 폐기 {formatDateTime(r.retiredAt)}
                </Typography.Text>
              </Space>
            ),
          }}
          pagination={{
            current: page + 1,
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
        title="EXPLANATION 프롬프트 등록"
        okText="등록"
        cancelText="취소"
        confirmLoading={creating}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form<CreatePromptFormValues>
          form={form}
          layout="vertical"
          initialValues={{ temperature: 0.2, maxTokens: 1200 }}
          preserve={false}
        >
          <Form.Item
            name="version"
            label="버전"
            rules={[{ required: true, message: '버전을 입력하세요.' }]}
            extra="예: 2026.06.1 또는 v1.2"
          >
            <Input placeholder="버전 문자열" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="systemPrompt"
            label="시스템 프롬프트"
            rules={[{ required: true, message: '시스템 프롬프트를 입력하세요.' }]}
          >
            <Input.TextArea rows={5} placeholder="EXPLANATION 시스템 프롬프트" />
          </Form.Item>
          <Form.Item
            name="userPromptTemplate"
            label="사용자 프롬프트 템플릿"
            rules={[
              { required: true, message: '사용자 프롬프트 템플릿을 입력하세요.' },
            ]}
          >
            <Input.TextArea rows={8} placeholder="EXPLANATION 사용자 프롬프트 템플릿" />
          </Form.Item>
          <Form.Item name="modelName" label="모델명 (선택)">
            <Input placeholder="예: deepseek-chat" maxLength={100} />
          </Form.Item>
          <Space align="start" style={{ width: '100%' }}>
            <Form.Item
              name="temperature"
              label="Temperature"
              rules={[{ required: true, message: 'temperature를 입력하세요.' }]}
            >
              <InputNumber min={0} max={2} step={0.1} style={{ width: 160 }} />
            </Form.Item>
            <Form.Item
              name="maxTokens"
              label="Max tokens"
              rules={[{ required: true, message: 'maxTokens를 입력하세요.' }]}
            >
              <InputNumber min={1} max={32000} style={{ width: 160 }} />
            </Form.Item>
          </Space>
          <Form.Item name="description" label="설명 (선택)">
            <Input.TextArea rows={2} maxLength={500} placeholder="변경 의도나 평가 기준" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        open={detailOpen}
        title={detail ? `프롬프트 버전 #${detail.id}` : '프롬프트 상세'}
        width={820}
        onClose={() => setDetailOpen(false)}
        destroyOnHidden
      >
        <Spin spinning={detailLoading}>
          {detail && (
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Descriptions column={2} bordered size="small">
                <Descriptions.Item label="유형">
                  <Tag>{detail.promptType}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="상태">
                  {statusTag(detail.status)}
                </Descriptions.Item>
                <Descriptions.Item label="버전">{detail.version}</Descriptions.Item>
                <Descriptions.Item label="모델">
                  {detail.modelName || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="Temperature">
                  {detail.temperature}
                </Descriptions.Item>
                <Descriptions.Item label="Max tokens">
                  {detail.maxTokens}
                </Descriptions.Item>
                <Descriptions.Item label="콘텐츠 해시" span={2}>
                  {detail.contentHash ? (
                    <Typography.Text code>{detail.contentHash}</Typography.Text>
                  ) : (
                    '-'
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="등록자">
                  #{detail.createdByAdminId ?? '-'}
                </Descriptions.Item>
                <Descriptions.Item label="등록일">
                  {formatDateTime(detail.createdAt)}
                </Descriptions.Item>
                <Descriptions.Item label="활성일">
                  {formatDateTime(detail.activatedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="폐기일">
                  {formatDateTime(detail.retiredAt)}
                </Descriptions.Item>
                <Descriptions.Item label="설명" span={2}>
                  {detail.description || '-'}
                </Descriptions.Item>
              </Descriptions>

              <Typography.Text strong>시스템 프롬프트</Typography.Text>
              <pre style={promptBlockStyle}>{detail.systemPrompt}</pre>

              <Typography.Text strong>사용자 프롬프트 템플릿</Typography.Text>
              <pre style={promptBlockStyle}>{detail.userPromptTemplate}</pre>
            </Space>
          )}
        </Spin>
      </Drawer>
    </Card>
  );
}
