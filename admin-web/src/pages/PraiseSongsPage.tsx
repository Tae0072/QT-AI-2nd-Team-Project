import { useState } from 'react';
import {
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Table,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useList } from '../hooks/useList';
import {
  createPraiseSong,
  listPraiseSongs,
  type CreatePraiseSongRequest,
  type PraiseSong,
  type PraiseSongListParams,
  type PraiseSongSourceType,
  type PraiseSongStatus,
} from '../api/praiseSongs';

// 상태 → 화면 라벨·색.
const STATUS_META: Record<PraiseSongStatus, { label: string; color: string }> = {
  ACTIVE: { label: '노출', color: 'green' },
  HIDDEN: { label: '숨김', color: 'default' },
};

// 출처 → 화면 라벨. 관리자 큐레이션은 CURATED 가 기본.
const SOURCE_META: Record<PraiseSongSourceType, string> = {
  CURATED: '큐레이션',
  DEVICE: '디바이스',
};

const STATUS_OPTIONS: PraiseSongStatus[] = ['ACTIVE', 'HIDDEN'];
const SOURCE_OPTIONS: PraiseSongSourceType[] = ['CURATED', 'DEVICE'];

function formatDateTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '-';
}

// AD-05 찬양 큐레이션 — 곡 메타데이터 목록 조회·필터 + 등록.
// 수정/숨김은 백엔드(PATCH·hide) 미구현이라 이번 범위에서 제외한다.
export default function PraiseSongsPage() {
  const { message } = App.useApp();
  // 목록은 공통 훅 재사용(데이터·로딩·필터·페이징).
  const { rows, loading, data, params, setParams, reload } = useList<
    PraiseSong,
    PraiseSongListParams
  >(listPraiseSongs, { page: 0, size: 20 });

  // 상태 필터 폼.
  const [filterForm] = Form.useForm();
  const onFilter = (v: { status?: PraiseSongStatus }) => {
    setParams({ status: v.status, page: 0 });
  };

  // 등록 모달 상태 + 입력 폼.
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const openCreate = () => {
    createForm.resetFields(); // 이전 입력 잔여 제거
    setCreateOpen(true);
  };

  // 모달 [등록]: 폼 검증 → POST → 목록 새로고침.
  const onSubmitCreate = async () => {
    let values: CreatePraiseSongRequest;
    try {
      values = await createForm.validateFields(); // 검증 실패면 모달 유지
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await createPraiseSong({
        title: values.title,
        artist: values.artist,
        sourceType: values.sourceType,
        licenseNote: values.licenseNote,
        status: values.status,
      });
      message.success('찬양 곡을 등록했습니다.');
      setCreateOpen(false);
      reload(); // 등록 결과를 목록에 반영
    } catch (e) {
      message.error(e instanceof Error ? e.message : '등록에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<PraiseSong> = [
    { title: '곡명', dataIndex: 'title', key: 'title' },
    { title: '아티스트', dataIndex: 'artist', key: 'artist', width: 160 },
    {
      title: '출처',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 100,
      render: (s: PraiseSongSourceType) => SOURCE_META[s] ?? s,
    },
    {
      title: '라이선스 메모',
      dataIndex: 'licenseNote',
      key: 'licenseNote',
      render: (note: string | null) => note ?? '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: PraiseSongStatus) => {
        const meta = STATUS_META[s] ?? { label: s, color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: formatDateTime,
    },
  ];

  return (
    <Card
      title="AD-05 찬양 큐레이션"
      extra={
        <Button type="primary" onClick={openCreate}>
          곡 등록
        </Button>
      }
    >
      {/* 필터: 상태를 골라 [조회]. 빈 값이면 전체. */}
      <Form
        form={filterForm}
        layout="inline"
        onFinish={onFilter}
        style={{ marginBottom: 16 }}
      >
        <Form.Item name="status" label="상태">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 140 }}
            options={STATUS_OPTIONS.map((s) => ({
              value: s,
              label: STATUS_META[s].label,
            }))}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">
            조회
          </Button>
        </Form.Item>
      </Form>

      <Table<PraiseSong>
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
          showTotal: (t) => `총 ${t}곡`,
          onChange: (page, pageSize) =>
            setParams({ page: page - 1, size: pageSize }),
        }}
      />

      {/* 등록 모달 — 곡 메타데이터만 입력. 🚫 가사·음원·외부 URL 입력란을 두지 않는다. */}
      <Modal
        open={createOpen}
        title="찬양 곡 등록"
        okText="등록"
        confirmLoading={submitting}
        onOk={onSubmitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="title"
            label="곡명"
            rules={[{ required: true, message: '곡명을 입력하세요' }]}
          >
            <Input placeholder="곡명" maxLength={100} />
          </Form.Item>
          <Form.Item
            name="artist"
            label="아티스트"
            rules={[{ required: true, message: '아티스트를 입력하세요' }]}
          >
            <Input placeholder="아티스트명" maxLength={100} />
          </Form.Item>
          <Form.Item
            name="sourceType"
            label="출처"
            initialValue={'CURATED' satisfies PraiseSongSourceType}
            rules={[{ required: true, message: '출처를 선택하세요' }]}
          >
            <Select
              options={SOURCE_OPTIONS.map((s) => ({
                value: s,
                label: SOURCE_META[s],
              }))}
            />
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
            initialValue={'ACTIVE' satisfies PraiseSongStatus}
            rules={[{ required: true, message: '상태를 선택하세요' }]}
          >
            <Select
              options={STATUS_OPTIONS.map((s) => ({
                value: s,
                label: STATUS_META[s].label,
              }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
