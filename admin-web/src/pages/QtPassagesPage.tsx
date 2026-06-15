import { useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Alert,
  Button,
  Tooltip,
  Select,
  Input,
  InputNumber,
  Modal,
  Form,
  Popconfirm,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import {
  listQtPassages,
  createQtPassage,
  updateQtPassage,
  publishQtPassage,
  hideQtPassage,
  type QtPassage,
  type QtPassageStatus,
  type QtPassageListParams,
  type QtPassageRequest,
} from '../api/qtPassages';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import { ApiClientError } from '../api/client';
import {
  QT_PASSAGE_FILTERABLE_STATUSES,
  qtPassageActionsForStatus,
} from './adminPageContracts';

// ===== AD-02 오늘 QT 관리 (풀 CRUD) =====
// 목록·필터 + 등록/수정 폼 + 상태별 게시/숨김. (공개 00:00 KST / 노출 04:00 KST)
// 백엔드 AdminQtPassage* 계약(DevA_이지윤) 기준.

// 상태 표시(한글 라벨 + 색). 소문자 enum → 운영자용 라벨.
const STATUS_META: Record<QtPassageStatus, { label: string; color: string }> = {
  pending_review: { label: '검토 대기', color: 'gold' },
  active: { label: '게시됨', color: 'green' },
  hidden: { label: '숨김', color: 'default' },
  deletion_notified: { label: '삭제 예정', color: 'orange' },
  removed: { label: '제거됨', color: 'red' },
};

const STATUS_OPTIONS = QT_PASSAGE_FILTERABLE_STATUSES.map((s) => ({
  label: STATUS_META[s].label,
  value: s,
}));

function errMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiClientError) return e.code ? `[${e.code}] ${e.message}` : e.message;
  return e instanceof Error ? e.message : fallback;
}

export default function QtPassagesPage() {
  // 대시보드(AD-01) 오늘 QT CTA가 ?focusId={qtPassageId} 로 진입하면 해당 본문 행을 강조한다.
  const [searchParams] = useSearchParams();
  const focusId = Number(searchParams.get('focusId')) || undefined;

  const { rows, page, size, total, loading, error, applyFilters, changePage, reload } =
    usePagedList<QtPassage, QtPassageListParams>(listQtPassages, { page: 0, size: 20 });

  // 필터 입력(조회 버튼을 눌러야 적용)
  const [status, setStatus] = useState<QtPassageStatus | undefined>(undefined);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [q, setQ] = useState('');

  // 등록/수정 모달
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<QtPassage | null>(null);
  const [saving, setSaving] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [form] = Form.useForm<QtPassageRequest>();

  const onSearch = () =>
    applyFilters({
      status,
      from: from.trim() || undefined,
      to: to.trim() || undefined,
      q: q.trim() || undefined,
    });

  const onReset = () => {
    setStatus(undefined);
    setFrom('');
    setTo('');
    setQ('');
    applyFilters({ status: undefined, from: undefined, to: undefined, q: undefined });
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (r: QtPassage) => {
    setEditing(r);
    form.setFieldsValue({
      qtDate: r.qtDate,
      bookId: r.bookId,
      chapter: r.chapter,
      startVerse: r.startVerse,
      endVerse: r.endVerse,
      title: r.title,
      mainVerseRef: r.mainVerseRef ?? undefined,
    });
    setOpen(true);
  };

  const onSubmit = async () => {
    let values: QtPassageRequest;
    try {
      values = await form.validateFields();
    } catch {
      return; // 검증 실패 — 필드에 메시지 표시, 모달 유지
    }
    setSaving(true);
    try {
      if (editing) {
        await updateQtPassage(editing.id, values);
        message.success('수정되었습니다');
      } else {
        await createQtPassage(values);
        message.success('등록되었습니다');
      }
      setOpen(false);
      reload();
    } catch (e) {
      message.error(errMessage(e, '저장에 실패했습니다'));
    } finally {
      setSaving(false);
    }
  };

  const runAction = async (fn: () => Promise<unknown>, okMsg: string) => {
    setMutating(true);
    try {
      await fn();
      message.success(okMsg);
      reload();
    } catch (e) {
      message.error(errMessage(e, '처리에 실패했습니다'));
    } finally {
      setMutating(false);
    }
  };

  const columns: ColumnsType<QtPassage> = [
    { title: '날짜', dataIndex: 'qtDate', width: 110 },
    {
      title: '본문',
      width: 200,
      render: (_, r) =>
        r.mainVerseRef ?? `#${r.bookId} ${r.chapter}:${r.startVerse}-${r.endVerse}`,
    },
    { title: '제목', dataIndex: 'title', ellipsis: true },
    {
      title: '상태',
      dataIndex: 'status',
      width: 110,
      render: (s: QtPassageStatus) => (
        <Tag color={STATUS_META[s]?.color ?? 'default'}>{STATUS_META[s]?.label ?? s}</Tag>
      ),
    },
    {
      title: '게시 시각',
      dataIndex: 'publishedAt',
      width: 170,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      width: 220,
      render: (_, r) => {
        const { canEdit, canPublish, canHide } = qtPassageActionsForStatus(r.status);
        if (!canEdit && !canPublish && !canHide) return '-';
        return (
          <Space size={4}>
            {canEdit && (
              <Button size="small" onClick={() => openEdit(r)} disabled={mutating}>
                수정
              </Button>
            )}
            {canPublish && (
              <Popconfirm
                title="이 QT를 게시할까요?"
                onConfirm={() => runAction(() => publishQtPassage(r.id), '게시되었습니다')}
              >
                <Button size="small" type="primary" disabled={mutating}>
                  게시
                </Button>
              </Popconfirm>
            )}
            {canHide && (
              <Popconfirm
                title="이 QT를 숨길까요?"
                onConfirm={() => runAction(() => hideQtPassage(r.id), '숨김 처리되었습니다')}
              >
                <Button size="small" danger disabled={mutating}>
                  숨김
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space align="center">
            <Tag color="blue">AD-02</Tag>
            <Typography.Title level={3} style={{ margin: 0 }}>
              오늘 QT 관리
            </Typography.Title>
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            QT 등록
          </Button>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          QT 본문을 등록·수정·게시·숨김 처리합니다. (공개 00:00 KST / 사용자 노출 04:00 KST)
        </Typography.Paragraph>

        {/* 필터 */}
        <Space wrap>
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 140 }}
            value={status}
            onChange={(v) => setStatus(v)}
            options={STATUS_OPTIONS}
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
          <Input
            placeholder="제목·구절 검색"
            style={{ width: 200 }}
            value={q}
            onChange={(e) => setQ(e.target.value)}
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

        {error && (
          <Alert
            type="error"
            showIcon
            message="목록을 불러오지 못했습니다"
            description={error}
            action={
              <Button size="small" onClick={reload}>
                재시도
              </Button>
            }
          />
        )}

        <Table<QtPassage>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          onRow={(record) =>
            focusId === record.id ? { style: { background: '#fffbe6' } } : {}
          }
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

      {/* 등록/수정 모달 */}
      <Modal
        title={editing ? 'QT 수정' : 'QT 등록'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        confirmLoading={saving}
        okText={editing ? '수정' : '등록'}
        cancelText="취소"
        forceRender
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="QT 날짜"
            name="qtDate"
            rules={[
              { required: true, message: 'QT 날짜를 입력해 주세요' },
              { pattern: /^\d{4}-\d{2}-\d{2}$/, message: 'YYYY-MM-DD 형식으로 입력해 주세요' },
            ]}
          >
            <Input placeholder="2026-06-10" />
          </Form.Item>
          <Space size="middle" style={{ display: 'flex' }} align="start">
            <Form.Item
              label="성경 권(ID)"
              name="bookId"
              rules={[{ required: true, message: '권 ID(1~66)' }]}
            >
              <InputNumber min={1} max={66} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item label="장" name="chapter" rules={[{ required: true, message: '장' }]}>
              <InputNumber min={1} style={{ width: 100 }} />
            </Form.Item>
          </Space>
          <Space size="middle" style={{ display: 'flex' }} align="start">
            <Form.Item
              label="시작 절"
              name="startVerse"
              rules={[{ required: true, message: '시작 절' }]}
            >
              <InputNumber min={1} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item
              label="종료 절"
              name="endVerse"
              dependencies={['startVerse']}
              rules={[
                { required: true, message: '종료 절' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const start = getFieldValue('startVerse');
                    if (value == null || start == null || value >= start) return Promise.resolve();
                    return Promise.reject(new Error('종료 절은 시작 절 이상이어야 합니다'));
                  },
                }),
              ]}
            >
              <InputNumber min={1} style={{ width: 120 }} />
            </Form.Item>
          </Space>
          <Form.Item
            label="제목"
            name="title"
            rules={[{ required: true, message: '제목을 입력해 주세요' }, { max: 200 }]}
          >
            <Input placeholder="오늘의 QT 제목" maxLength={200} />
          </Form.Item>
          <Form.Item label="대표 구절(선택)" name="mainVerseRef" rules={[{ max: 100 }]}>
            <Input placeholder="예: 시 23:1-6" maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
