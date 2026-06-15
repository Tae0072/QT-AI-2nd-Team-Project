import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSearchParams } from 'react-router-dom';
import {
  changeQtVideoClipStatus,
  createSourceVideo,
  listBibleBooks,
  listQtVideoClips,
  listSegments,
  listSourceVideos,
  prepareQtVideoClip,
  replaceSegments,
  updateSourceVideo,
  type BibleBook,
  type QtVideoClip,
  type QtVideoClipParams,
  type SegmentPayload,
  type SourceVideo,
  type SourceVideoParams,
} from '../api/qtVideos';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

const SOURCE_STATUS_OPTIONS = [
  { label: '활성(ACTIVE)', value: 'ACTIVE' },
  { label: '비활성(INACTIVE)', value: 'INACTIVE' },
];

const CLIP_STATUS_OPTIONS = [
  { label: '승인(APPROVED)', value: 'APPROVED' },
  { label: '숨김(HIDDEN)', value: 'HIDDEN' },
  { label: '실패(FAILED)', value: 'FAILED' },
];

interface SourceVideoFormValues {
  bibleBookId: number;
  title: string;
  videoUrl: string;
  durationSec: number;
  status?: string;
}

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    ACTIVE: { color: 'green', text: '활성' },
    INACTIVE: { color: 'default', text: '비활성' },
    APPROVED: { color: 'green', text: '승인' },
    HIDDEN: { color: 'default', text: '숨김' },
    FAILED: { color: 'red', text: '실패' },
    PENDING: { color: 'gold', text: '대기' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

function segmentTemplate() {
  return JSON.stringify(
    [
      { chapter: 13, verse: 1, startTimeSec: 120.0, endTimeSec: 135.5 },
      { chapter: 13, verse: 2, startTimeSec: 135.5, endTimeSec: 151.0 },
    ],
    null,
    2,
  );
}

function bookOptionLabel(book: BibleBook) {
  return `${book.koreanName} (${book.code}) - ${book.id}번`;
}

export default function QtVideosPage() {
  const [searchParams] = useSearchParams();
  const initialPassageId = Number(searchParams.get('qtPassageId')) || undefined;

  const sources = usePagedList<SourceVideo, SourceVideoParams>(listSourceVideos, {
    page: 0,
    size: 20,
  });
  const clips = usePagedList<QtVideoClip, QtVideoClipParams>(listQtVideoClips, {
    page: 0,
    size: 20,
    qtPassageId: initialPassageId,
  });

  const [sourceBookId, setSourceBookId] = useState<number | undefined>();
  const [bibleBooks, setBibleBooks] = useState<BibleBook[]>([]);
  const [sourceStatus, setSourceStatus] = useState<string | undefined>();
  const [clipPassageId, setClipPassageId] = useState<number | undefined>(initialPassageId);
  const [clipStatus, setClipStatus] = useState<string | undefined>();
  const [sourceModalOpen, setSourceModalOpen] = useState(false);
  const [editingSource, setEditingSource] = useState<SourceVideo | null>(null);
  const [segmentSource, setSegmentSource] = useState<SourceVideo | null>(null);
  const [segmentText, setSegmentText] = useState(segmentTemplate());
  const [submitting, setSubmitting] = useState(false);
  const [sourceForm] = Form.useForm<SourceVideoFormValues>();

  useEffect(() => {
    let cancelled = false;
    listBibleBooks()
      .then((rows) => {
        if (!cancelled) setBibleBooks(rows);
      })
      .catch((e) => {
        message.error(e instanceof Error ? e.message : '성경권 목록을 불러오지 못했습니다.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const bibleBookOptions = bibleBooks.map((book) => ({
    label: bookOptionLabel(book),
    value: book.id,
  }));
  const bibleBookLabelById = new Map(bibleBooks.map((book) => [book.id, bookOptionLabel(book)]));

  const openCreateSource = () => {
    setEditingSource(null);
    sourceForm.resetFields();
    sourceForm.setFieldsValue({ status: 'ACTIVE' });
    setSourceModalOpen(true);
  };

  const openEditSource = (source: SourceVideo) => {
    setEditingSource(source);
    sourceForm.setFieldsValue({
      bibleBookId: source.bibleBookId,
      title: source.title,
      videoUrl: source.videoUrl,
      durationSec: source.durationSec,
      status: source.status,
    });
    setSourceModalOpen(true);
  };

  const submitSource = async () => {
    const values = await sourceForm.validateFields();
    setSubmitting(true);
    try {
      if (editingSource) {
        await updateSourceVideo(editingSource.id, {
          ...values,
          status: values.status ?? 'ACTIVE',
        });
        message.success('원본 영상을 수정했습니다.');
      } else {
        await createSourceVideo(values);
        message.success('원본 영상을 등록했습니다.');
      }
      setSourceModalOpen(false);
      sources.reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '원본 영상 저장에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const openSegments = async (source: SourceVideo) => {
    setSegmentSource(source);
    try {
      const rows = await listSegments(source.id);
      setSegmentText(
        rows.length === 0
          ? segmentTemplate()
          : JSON.stringify(
              rows.map((row) => ({
                bibleVerseId: row.bibleVerseId,
                startTimeSec: row.startTimeSec,
                endTimeSec: row.endTimeSec,
              })),
              null,
              2,
            ),
      );
    } catch (e) {
      message.error(e instanceof Error ? e.message : '구간을 불러오지 못했습니다.');
    }
  };

  const submitSegments = async () => {
    if (!segmentSource) return;
    let parsed: SegmentPayload[];
    try {
      parsed = JSON.parse(segmentText) as SegmentPayload[];
      if (!Array.isArray(parsed)) throw new Error('segments must be an array');
    } catch {
      message.error('구간 JSON 배열 형식이 올바르지 않습니다.');
      return;
    }
    setSubmitting(true);
    try {
      const rows = await replaceSegments(segmentSource.id, parsed);
      message.success(`구간 ${rows.length}건을 저장했습니다.`);
      setSegmentSource(null);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '구간 저장에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const onPrepareClip = async () => {
    if (!clipPassageId) {
      message.warning('QT 본문 ID를 입력하세요.');
      return;
    }
    setSubmitting(true);
    try {
      const result = await prepareQtVideoClip(clipPassageId);
      if (result.prepared) {
        message.success(`QT 영상 클립을 생성했습니다. (#${result.clipId})`);
      } else {
        message.warning('클립을 생성하지 못했습니다. 본문 공개 여부와 절별 구간을 확인하세요.');
      }
      clips.applyFilters({ qtPassageId: clipPassageId });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '클립 생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const onChangeClipStatus = async (clipId: number, status: string) => {
    setSubmitting(true);
    try {
      await changeQtVideoClipStatus(clipId, status);
      message.success('클립 상태를 변경했습니다.');
      clips.reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '상태 변경에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const sourceColumns: ColumnsType<SourceVideo> = [
    { title: 'ID', dataIndex: 'id', width: 80, render: (v: number) => `#${v}` },
    {
      title: '성경권',
      dataIndex: 'bibleBookId',
      width: 190,
      render: (v: number) => bibleBookLabelById.get(v) ?? `${v}번`,
    },
    { title: '제목', dataIndex: 'title', width: 220, ellipsis: true },
    { title: 'URL', dataIndex: 'videoUrl', ellipsis: true },
    { title: '길이(초)', dataIndex: 'durationSec', width: 110 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => statusTag(v),
    },
    {
      title: '등록 시각',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      width: 160,
      render: (_, row) => (
        <Space>
          <Button size="small" onClick={() => openEditSource(row)}>
            수정
          </Button>
          <Button size="small" onClick={() => openSegments(row)}>
            구간
          </Button>
        </Space>
      ),
    },
  ];

  const clipColumns: ColumnsType<QtVideoClip> = [
    { title: 'ID', dataIndex: 'id', width: 80, render: (v: number) => `#${v}` },
    { title: 'QT 본문', dataIndex: 'qtPassageId', width: 100, render: (v: number) => `#${v}` },
    { title: '제목', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '원본', dataIndex: 'sourceVideoId', width: 90, render: (v: number) => `#${v}` },
    { title: '시작', dataIndex: 'startTimeSec', width: 90 },
    { title: '끝', dataIndex: 'endTimeSec', width: 90 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => statusTag(v),
    },
    {
      title: '승인 시각',
      dataIndex: 'approvedAt',
      width: 170,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      width: 170,
      render: (_, row) => (
        <Space>
          {row.status !== 'APPROVED' && (
            <Button size="small" onClick={() => onChangeClipStatus(row.id, 'APPROVED')}>
              승인
            </Button>
          )}
          {row.status === 'APPROVED' && (
            <Popconfirm
              title="이 QT 영상을 숨길까요?"
              okText="숨김"
              cancelText="취소"
              onConfirm={() => onChangeClipStatus(row.id, 'HIDDEN')}
            >
              <Button size="small" danger disabled={submitting}>
                숨김
              </Button>
            </Popconfirm>
          )}
          {row.status !== 'FAILED' && (
            <Button size="small" onClick={() => onChangeClipStatus(row.id, 'FAILED')}>
              실패
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-20</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            QT 영상 관리
          </Typography.Title>
        </Space>

        <Tabs
          items={[
            {
              key: 'sources',
              label: '원본 영상',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Space wrap>
                    <Select
                      placeholder="성경권"
                      allowClear
                      showSearch
                      optionFilterProp="label"
                      style={{ width: 260 }}
                      value={sourceBookId}
                      options={bibleBookOptions}
                      onChange={(v) => setSourceBookId(v)}
                    />
                    <Select
                      placeholder="상태"
                      allowClear
                      style={{ width: 160 }}
                      value={sourceStatus}
                      options={SOURCE_STATUS_OPTIONS}
                      onChange={(v) => setSourceStatus(v)}
                    />
                    <Button
                      type="primary"
                      onClick={() =>
                        sources.applyFilters({
                          bibleBookId: sourceBookId,
                          status: sourceStatus,
                        })
                      }
                    >
                      조회
                    </Button>
                    <Button onClick={openCreateSource}>원본 영상 등록</Button>
                  </Space>
                  <Table<SourceVideo>
                    rowKey="id"
                    loading={sources.loading}
                    columns={sourceColumns}
                    dataSource={sources.rows}
                    scroll={{ x: 'max-content' }}
                    pagination={{
                      current: sources.page + 1,
                      pageSize: sources.size,
                      total: sources.total,
                      showSizeChanger: true,
                      showTotal: (total) => `총 ${total}건`,
                      onChange: (page, size) => sources.changePage(page - 1, size),
                    }}
                  />
                </Space>
              ),
            },
            {
              key: 'clips',
              label: 'QT 클립',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Space wrap>
                    <InputNumber
                      placeholder="QT 본문 ID"
                      value={clipPassageId}
                      onChange={(v) => setClipPassageId(v ?? undefined)}
                    />
                    <Select
                      placeholder="상태"
                      allowClear
                      style={{ width: 160 }}
                      value={clipStatus}
                      options={CLIP_STATUS_OPTIONS}
                      onChange={(v) => setClipStatus(v)}
                    />
                    <Button
                      type="primary"
                      onClick={() =>
                        clips.applyFilters({
                          qtPassageId: clipPassageId,
                          status: clipStatus,
                        })
                      }
                    >
                      조회
                    </Button>
                    <Button onClick={onPrepareClip} loading={submitting}>
                      QT 클립 생성
                    </Button>
                  </Space>
                  <Table<QtVideoClip>
                    rowKey="id"
                    loading={clips.loading}
                    columns={clipColumns}
                    dataSource={clips.rows}
                    scroll={{ x: 'max-content' }}
                    pagination={{
                      current: clips.page + 1,
                      pageSize: clips.size,
                      total: clips.total,
                      showSizeChanger: true,
                      showTotal: (total) => `총 ${total}건`,
                      onChange: (page, size) => clips.changePage(page - 1, size),
                    }}
                  />
                </Space>
              ),
            },
          ]}
        />
      </Space>

      <Modal
        open={sourceModalOpen}
        title={editingSource ? '원본 영상 수정' : '원본 영상 등록'}
        okText="저장"
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitSource}
        onCancel={() => setSourceModalOpen(false)}
        destroyOnHidden
      >
        <Form form={sourceForm} layout="vertical">
          <Form.Item name="bibleBookId" label="성경권" rules={[{ required: true }]}>
            <Select
              disabled={!!editingSource}
              showSearch
              optionFilterProp="label"
              placeholder="성경권을 선택하세요"
              options={bibleBookOptions}
            />
          </Form.Item>
          <Form.Item name="title" label="제목" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="videoUrl" label="영상 URL" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="durationSec" label="영상 길이(초)" rules={[{ required: true }]}>
            <InputNumber min={0.001} step={0.001} style={{ width: '100%' }} />
          </Form.Item>
          {editingSource && (
            <Form.Item name="status" label="상태" rules={[{ required: true }]}>
              <Select options={SOURCE_STATUS_OPTIONS} />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        open={!!segmentSource}
        title={segmentSource ? `절별 구간 저장 - 원본 #${segmentSource.id}` : '절별 구간 저장'}
        okText="저장"
        cancelText="취소"
        width={720}
        confirmLoading={submitting}
        onOk={submitSegments}
        onCancel={() => setSegmentSource(null)}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">
          JSON 배열로 저장합니다. `bibleVerseId`를 쓰거나, 원본 영상의 성경권 기준 `chapter`와 `verse`를 쓸 수 있습니다.
        </Typography.Paragraph>
        <Input.TextArea
          rows={14}
          value={segmentText}
          onChange={(event) => setSegmentText(event.target.value)}
          style={{ fontFamily: 'monospace' }}
        />
      </Modal>
    </Card>
  );
}
