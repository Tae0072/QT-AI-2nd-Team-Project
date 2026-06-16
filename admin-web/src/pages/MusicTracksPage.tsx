import { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Segmented,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Upload,
  message,
  Typography,
} from 'antd';
import type { UploadFile } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  createMusicTrack,
  deleteMusicTrack,
  hideMusicTrack,
  listMusicTracks,
  publishMusicTrack,
  updateMusicTrack,
  type MusicTrack,
  type MusicTrackCategory,
  type MusicTrackFormValues,
  type MusicTrackListParams,
  type MusicTrackStatus,
} from '../api/musicTracks';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import {
  MUSIC_TRACK_FILTERABLE_STATUSES,
  musicTrackActionsForStatus,
} from './adminPageContracts';

type MusicTrackEditorValues = Omit<MusicTrackFormValues, 'file'> & {
  fileList?: UploadFile[];
};

const STATUS_LABELS: Record<MusicTrackStatus, string> = {
  ACTIVE: '노출(ACTIVE)',
  HIDDEN: '숨김(HIDDEN)',
};

const CATEGORY_LABELS: Record<MusicTrackCategory, string> = {
  BGM: '배경음악',
  HYMN: '찬송가',
};

const STATUS_OPTIONS = MUSIC_TRACK_FILTERABLE_STATUSES.map((value) => ({
  label: STATUS_LABELS[value],
  value,
}));

const CATEGORY_OPTIONS: Array<{ label: string; value: MusicTrackCategory }> = [
  { label: CATEGORY_LABELS.BGM, value: 'BGM' },
  { label: CATEGORY_LABELS.HYMN, value: 'HYMN' },
];

function statusTag(status: MusicTrackStatus) {
  return status === 'ACTIVE' ? (
    <Tag color="green">노출</Tag>
  ) : (
    <Tag color="default">숨김</Tag>
  );
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} B`;
  const kb = value / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
}

function fileFromList(fileList: UploadFile[] | undefined): File | undefined {
  return fileList?.[0]?.originFileObj as File | undefined;
}

function toPayload(values: MusicTrackEditorValues): MusicTrackFormValues {
  return {
    title: values.title,
    category: values.category,
    mimeType: values.mimeType,
    durationSec: values.durationSec,
    sortOrder: values.sortOrder,
    licenseNote: values.licenseNote,
    file: fileFromList(values.fileList),
  };
}

export default function MusicTracksPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<MusicTrack, MusicTrackListParams>(listMusicTracks, {
      page: 0,
      size: 20,
    });

  const [status, setStatus] = useState<string | undefined>(undefined);
  // 분류 탭(전체/배경음악/찬송가) — 서버 category 필터로 분리 관리.
  const [categoryTab, setCategoryTab] = useState<'ALL' | MusicTrackCategory>('ALL');
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm<MusicTrackEditorValues>();
  const [submitting, setSubmitting] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MusicTrack | null>(null);
  const [editForm] = Form.useForm<MusicTrackEditorValues>();
  const [editSubmitting, setEditSubmitting] = useState(false);

  const buildFilters = (
    nextStatus: string | undefined,
    nextCategory: 'ALL' | MusicTrackCategory,
  ): MusicTrackListParams => ({
    status: (nextStatus as MusicTrackStatus) || undefined,
    category: nextCategory === 'ALL' ? undefined : nextCategory,
  });

  const onSearch = () => applyFilters(buildFilters(status, categoryTab));

  const onReset = () => {
    setStatus(undefined);
    setCategoryTab('ALL');
    applyFilters({ status: undefined, category: undefined });
  };

  // 분류 탭 전환 — 즉시 재조회.
  const onCategoryChange = (next: 'ALL' | MusicTrackCategory) => {
    setCategoryTab(next);
    applyFilters(buildFilters(status, next));
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteMusicTrack(id);
      message.success('배경음악을 삭제했습니다.');
      reload();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '삭제에 실패했습니다.');
    }
  };

  const openCreate = () => {
    createForm.resetFields();
    createForm.setFieldsValue({
      category: 'BGM',
      mimeType: 'audio/mpeg',
      sortOrder: 0,
    });
    setCreateOpen(true);
  };

  const submitCreate = async () => {
    let values: MusicTrackEditorValues;
    try {
      values = await createForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await createMusicTrack(toPayload(values));
      message.success('배경음악을 등록했습니다.');
      setCreateOpen(false);
      reload();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '등록에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const openEdit = (record: MusicTrack) => {
    setEditTarget(record);
    editForm.setFieldsValue({
      title: record.title,
      category: record.category,
      mimeType: record.mimeType,
      durationSec: record.durationSec ?? undefined,
      sortOrder: record.sortOrder,
      licenseNote: record.licenseNote ?? undefined,
      fileList: [],
    });
    setEditOpen(true);
  };

  const submitEdit = async () => {
    if (!editTarget) return;
    let values: MusicTrackEditorValues;
    try {
      values = await editForm.validateFields();
    } catch {
      return;
    }
    setEditSubmitting(true);
    try {
      await updateMusicTrack(editTarget.id, toPayload(values));
      message.success('배경음악을 수정했습니다.');
      setEditOpen(false);
      reload();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '수정에 실패했습니다.');
    } finally {
      setEditSubmitting(false);
    }
  };

  const handlePublish = async (id: number) => {
    try {
      await publishMusicTrack(id);
      message.success('배경음악을 노출했습니다.');
      reload();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '노출 처리에 실패했습니다.');
    }
  };

  const handleHide = async (id: number) => {
    try {
      await hideMusicTrack(id);
      message.success('배경음악을 숨김 처리했습니다.');
      reload();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '숨김 처리에 실패했습니다.');
    }
  };

  const columns: ColumnsType<MusicTrack> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '제목', dataIndex: 'title', width: 220 },
    {
      title: '분류',
      dataIndex: 'category',
      width: 100,
      render: (value: MusicTrackCategory) => CATEGORY_LABELS[value] ?? value,
    },
    { title: 'MIME', dataIndex: 'mimeType', width: 130 },
    {
      title: '크기',
      dataIndex: 'byteSize',
      width: 110,
      render: (value: number) => formatBytes(value),
    },
    {
      title: '길이',
      dataIndex: 'durationSec',
      width: 90,
      render: (value: number | null) => (value == null ? '-' : `${value}s`),
    },
    { title: '정렬', dataIndex: 'sortOrder', width: 80 },
    {
      title: '라이선스 메모',
      dataIndex: 'licenseNote',
      render: (value: string | null) => value ?? '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (value: MusicTrackStatus) => statusTag(value),
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '작업',
      key: 'actions',
      width: 190,
      fixed: 'right',
      render: (_: unknown, record: MusicTrack) => {
        const actions = musicTrackActionsForStatus(record.status);
        return (
          <Space>
            <Tooltip title="수정">
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => openEdit(record)}
              />
            </Tooltip>
            {actions.canPublish && (
              <Popconfirm
                title="노출 확인"
                okText="노출"
                cancelText="취소"
                onConfirm={() => handlePublish(record.id)}
              >
                <Tooltip title="노출">
                  <Button size="small" icon={<CheckCircleOutlined />} />
                </Tooltip>
              </Popconfirm>
            )}
            {actions.canHide && (
              <Popconfirm
                title="숨김 확인"
                okText="숨김"
                cancelText="취소"
                onConfirm={() => handleHide(record.id)}
              >
                <Tooltip title="숨김">
                  <Button size="small" icon={<EyeInvisibleOutlined />} />
                </Tooltip>
              </Popconfirm>
            )}
            <Popconfirm
              title="삭제 확인"
              description="이 음원을 삭제하시겠습니까? 목록·앱에서 제외됩니다."
              okText="삭제"
              cancelText="취소"
              okButtonProps={{ danger: true }}
              onConfirm={() => handleDelete(record.id)}
            >
              <Tooltip title="삭제">
                <Button size="small" danger icon={<DeleteOutlined />} />
              </Tooltip>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-12</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            배경음악 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          배경음악(브금)·찬송가를 분류별로 관리합니다. 등록·수정·노출/숨김·삭제와 찬양 음원 관리를 이 화면에서 통합 처리합니다.
        </Typography.Paragraph>

        <Segmented<'ALL' | MusicTrackCategory>
          value={categoryTab}
          onChange={onCategoryChange}
          options={[
            { label: '전체', value: 'ALL' },
            { label: CATEGORY_LABELS.BGM, value: 'BGM' },
            { label: CATEGORY_LABELS.HYMN, value: 'HYMN' },
          ]}
        />

        <Space wrap>
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 180 }}
            value={status}
            onChange={(value) => setStatus(value)}
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
            등록
          </Button>
        </Space>

        <Table<MusicTrack>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: page + 1,
            pageSize: size,
            total,
            showSizeChanger: true,
            showTotal: (count) => `총 ${count}곡`,
            onChange: (nextPage, nextSize) => changePage(nextPage - 1, nextSize),
          }}
        />
      </Space>

      <Modal
        open={createOpen}
        title="배경음악 등록"
        okText="등록"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <MusicTrackForm form={createForm} requireFile />
      </Modal>

      <Modal
        open={editOpen}
        title="배경음악 수정"
        okText="저장"
        cancelText="취소"
        confirmLoading={editSubmitting}
        onOk={submitEdit}
        onCancel={() => setEditOpen(false)}
        destroyOnHidden
      >
        <MusicTrackForm form={editForm} requireFile={false} />
      </Modal>
    </Card>
  );
}

function MusicTrackForm({
  form,
  requireFile,
}: {
  form: ReturnType<typeof Form.useForm<MusicTrackEditorValues>>[0];
  requireFile: boolean;
}) {
  return (
    <Form form={form} layout="vertical">
      <Form.Item
        name="title"
        label="제목"
        rules={[{ required: true, message: '제목을 입력하세요' }]}
      >
        <Input maxLength={150} placeholder="제목" />
      </Form.Item>
      <Form.Item
        name="category"
        label="분류"
        rules={[{ required: true, message: '분류를 선택하세요' }]}
      >
        <Select options={CATEGORY_OPTIONS} />
      </Form.Item>
      <Form.Item
        name="mimeType"
        label="MIME"
        rules={[{ required: true, message: 'MIME을 입력하세요' }]}
      >
        <Input maxLength={60} placeholder="audio/mpeg" />
      </Form.Item>
      <Space size="middle" style={{ width: '100%' }}>
        <Form.Item name="durationSec" label="길이(초)">
          <InputNumber min={0} max={86_400} style={{ width: 140 }} />
        </Form.Item>
        <Form.Item name="sortOrder" label="정렬">
          <InputNumber min={0} max={10_000} style={{ width: 140 }} />
        </Form.Item>
      </Space>
      <Form.Item name="licenseNote" label="라이선스 메모">
        <Input.TextArea rows={2} maxLength={300} />
      </Form.Item>
      <Form.Item
        name="fileList"
        label="음원 파일"
        valuePropName="fileList"
        getValueFromEvent={(event) => event?.fileList}
        rules={
          requireFile
            ? [{ required: true, message: '음원 파일을 선택하세요' }]
            : undefined
        }
      >
        <Upload beforeUpload={() => false} maxCount={1} accept="audio/*">
          <Button icon={<UploadOutlined />}>파일 선택</Button>
        </Upload>
      </Form.Item>
    </Form>
  );
}
