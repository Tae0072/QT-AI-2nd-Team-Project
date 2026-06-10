import { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  createPraiseSong,
  listPraiseSongs,
  type CreatePraiseSongRequest,
  type PraiseSong,
  type PraiseSongListParams,
  type PraiseSongStatus,
} from '../api/praiseSongs';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-05 찬양 큐레이션 =====
// 곡 메타데이터 목록 + 상태 필터 + 서버 페이지네이션 + 등록 모달. 권한: OPERATOR / SUPER_ADMIN.
// 🚫 가사·음원 파일·외부 재생 URL 은 저장하지 않는다(메타데이터만, CLAUDE.md §8 / F-09).
// 수정/숨김(PATCH·hide)은 백엔드 미구현이라 이번 범위 제외.

const STATUS_OPTIONS = [
  { label: '노출(ACTIVE)', value: 'ACTIVE' },
  { label: '숨김(HIDDEN)', value: 'HIDDEN' },
];
const SOURCE_OPTIONS = [
  { label: '큐레이션(CURATED)', value: 'CURATED' },
  { label: '디바이스(DEVICE)', value: 'DEVICE' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    ACTIVE: { color: 'green', text: '노출' },
    HIDDEN: { color: 'default', text: '숨김' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

function sourceLabel(source: string) {
  if (source === 'CURATED') return '큐레이션';
  if (source === 'DEVICE') return '디바이스';
  return source;
}

export default function PraiseSongsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<PraiseSong, PraiseSongListParams>(listPraiseSongs, {
      page: 0,
      size: 20,
    });

  const [status, setStatus] = useState<string | undefined>(undefined);

  // 등록 모달
  const [createOpen, setCreateOpen] = useState(false);
  const [form] = Form.useForm<CreatePraiseSongRequest>();
  const [submitting, setSubmitting] = useState(false);

  const onSearch = () =>
    applyFilters({ status: (status as PraiseSongStatus) || undefined });

  const onReset = () => {
    setStatus(undefined);
    applyFilters({ status: undefined });
  };

  const openCreate = () => {
    form.resetFields();
    setCreateOpen(true);
  };

  const submitCreate = async () => {
    let values: CreatePraiseSongRequest;
    try {
      values = await form.validateFields();
    } catch {
      return; // 검증 실패 시 모달 유지
    }
    setSubmitting(true);
    try {
      await createPraiseSong(values);
      message.success('찬양 곡을 등록했습니다.');
      setCreateOpen(false);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '등록에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<PraiseSong> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '곡명', dataIndex: 'title' },
    { title: '아티스트', dataIndex: 'artist', width: 160 },
    {
      title: '출처',
      dataIndex: 'sourceType',
      width: 100,
      render: (v: string) => sourceLabel(v),
    },
    {
      title: '라이선스 메모',
      dataIndex: 'licenseNote',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => statusTag(v),
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-05</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            찬양 큐레이션
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          추천 찬양 곡 메타데이터를 등록·관리합니다. 권한: OPERATOR / SUPER_ADMIN.
          가사·음원·외부 재생 URL은 저장하지 않습니다(메타데이터만).
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
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            곡 등록
          </Button>
        </Space>

        <Table<PraiseSong>
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
            showTotal: (t) => `총 ${t}곡`,
            onChange: (p, ps) => changePage(p - 1, ps),
          }}
        />
      </Space>

      {/* 등록 모달 — 곡 메타데이터만. 🚫 가사·음원·외부 URL 입력란 없음. */}
      <Modal
        open={createOpen}
        title="찬양 곡 등록"
        okText="등록"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ sourceType: 'CURATED', status: 'ACTIVE' }}
        >
          <Form.Item
            name="title"
            label="곡명"
            rules={[{ required: true, message: '곡명을 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="곡명" />
          </Form.Item>
          <Form.Item
            name="artist"
            label="아티스트"
            rules={[{ required: true, message: '아티스트를 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="아티스트명" />
          </Form.Item>
          <Form.Item
            name="sourceType"
            label="출처"
            rules={[{ required: true, message: '출처를 선택하세요' }]}
          >
            <Select options={SOURCE_OPTIONS} />
          </Form.Item>
          <Form.Item name="licenseNote" label="라이선스 메모">
            <Input.TextArea
              rows={2}
              maxLength={200}
              placeholder="저작권 확인 메모 (가사·음원·URL 저장 금지 — 메타데이터만)"
            />
          </Form.Item>
          <Form.Item
            name="status"
            label="상태"
            rules={[{ required: true, message: '상태를 선택하세요' }]}
          >
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
