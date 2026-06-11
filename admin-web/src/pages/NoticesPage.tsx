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
  Modal,
  Form,
  Input,
  Popconfirm,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import type { PageParams } from '../api/types';
import {
  listNotices,
  createNotice,
  updateNotice,
  publishNotice,
  hideNotice,
  type Notice,
  type NoticeStatus,
  type NoticeRequest,
} from '../api/notices';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import { ApiClientError } from '../api/client';

// ===== AD-06 시스템 공지 (풀 CRUD) =====
// 목록 + 등록/수정 폼 + 발행(알림 fan-out)/숨김. 본문 plain text(`<`,`>` 거부).
// 백엔드 AdminNotice* 계약(DevC_강상민) 기준.

const STATUS_META: Record<NoticeStatus, { label: string; color: string }> = {
  DRAFT: { label: '초안', color: 'default' },
  PUBLISHED: { label: '발행됨', color: 'green' },
  HIDDEN: { label: '숨김', color: 'gold' },
};

// 본문·제목은 plain text만 허용(stored XSS 방지, 서버도 `<`,`>` 거부).
const NO_ANGLE = { pattern: /^[^<>]*$/, message: '< > 문자는 사용할 수 없습니다' };

function errMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiClientError) return e.code ? `[${e.code}] ${e.message}` : e.message;
  return e instanceof Error ? e.message : fallback;
}

export default function NoticesPage() {
  const { rows, page, size, total, loading, error, changePage, reload } =
    usePagedList<Notice, PageParams>(listNotices, { page: 0, size: 20 });

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Notice | null>(null);
  const [editingBody, setEditingBody] = useState(''); // 목록엔 bodyPreview만 있어 수정 시 본문 미리 확보 불가 → 빈 값에서 편집
  const [saving, setSaving] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [form] = Form.useForm<NoticeRequest>();

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (r: Notice) => {
    setEditing(r);
    // 목록 응답엔 bodyPreview만 있음 → 제목은 채우고 본문은 사용자가 다시 입력(상세 GET 부재)
    form.setFieldsValue({ title: r.title, body: '' });
    setEditingBody(r.bodyPreview);
    setOpen(true);
  };

  const onSubmit = async () => {
    let values: NoticeRequest;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSaving(true);
    try {
      if (editing) {
        await updateNotice(editing.id, values);
        message.success('수정되었습니다');
      } else {
        await createNotice(values);
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

  const onPublish = async (r: Notice) => {
    setMutating(true);
    try {
      const res = await publishNotice(r.id);
      const { createdCount, failedCount } = res.notificationResult;
      message.success(
        `발행되었습니다 — 알림 ${createdCount}건 생성${failedCount > 0 ? `, ${failedCount}건 실패` : ''}`,
      );
      reload();
    } catch (e) {
      message.error(errMessage(e, '발행에 실패했습니다'));
    } finally {
      setMutating(false);
    }
  };

  const onHide = async (r: Notice) => {
    setMutating(true);
    try {
      await hideNotice(r.id);
      message.success('숨김 처리되었습니다');
      reload();
    } catch (e) {
      message.error(errMessage(e, '처리에 실패했습니다'));
    } finally {
      setMutating(false);
    }
  };

  const columns: ColumnsType<Notice> = [
    { title: '제목', dataIndex: 'title', width: 220, ellipsis: true },
    { title: '미리보기', dataIndex: 'bodyPreview', ellipsis: true },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (s: NoticeStatus) => (
        <Tag color={STATUS_META[s]?.color ?? 'default'}>{STATUS_META[s]?.label ?? s}</Tag>
      ),
    },
    {
      title: '발행 시각',
      dataIndex: 'publishedAt',
      width: 170,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      width: 200,
      render: (_, r) => {
        // DRAFT→수정·발행 / PUBLISHED→숨김 / HIDDEN→없음
        if (r.status === 'HIDDEN') return '-';
        return (
          <Space size={4}>
            {r.status === 'DRAFT' && (
              <>
                <Button size="small" onClick={() => openEdit(r)} disabled={mutating}>
                  수정
                </Button>
                <Popconfirm
                  title="이 공지를 발행할까요? 활성 회원에게 알림이 발송됩니다."
                  onConfirm={() => onPublish(r)}
                >
                  <Button size="small" type="primary" disabled={mutating}>
                    발행
                  </Button>
                </Popconfirm>
              </>
            )}
            {r.status === 'PUBLISHED' && (
              <Popconfirm title="이 공지를 숨길까요?" onConfirm={() => onHide(r)}>
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
            <Tag color="blue">AD-06</Tag>
            <Typography.Title level={3} style={{ margin: 0 }}>
              시스템 공지
            </Typography.Title>
            <Tooltip title="새로고침">
              <Button size="small" icon={<ReloadOutlined />} onClick={reload} />
            </Tooltip>
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            공지 등록
          </Button>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          앱 공지사항을 등록·수정·발행·숨김 처리합니다. 발행 시 활성 회원에게 알림이 생성됩니다.
        </Typography.Paragraph>

        {error && (
          <Alert
            type="error"
            showIcon
            message="공지 목록을 불러오지 못했습니다"
            description={error}
            action={
              <Button size="small" onClick={reload}>
                재시도
              </Button>
            }
          />
        )}

        <Table<Notice>
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

      {/* 등록/수정 모달 */}
      <Modal
        title={editing ? '공지 수정' : '공지 등록'}
        open={open}
        onOk={onSubmit}
        onCancel={() => setOpen(false)}
        confirmLoading={saving}
        okText={editing ? '수정' : '등록'}
        cancelText="취소"
        forceRender
      >
        {editing && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="본문 전체를 불러올 수 없습니다 (상세 GET 미제공)"
            description={`수정하려면 본문을 다시 입력해 주세요. 기존 미리보기: ${editingBody || '(없음)'}`}
          />
        )}
        <Form form={form} layout="vertical">
          <Form.Item
            label="제목"
            name="title"
            rules={[{ required: true, message: '제목을 입력해 주세요' }, NO_ANGLE]}
          >
            <Input placeholder="공지 제목" maxLength={200} />
          </Form.Item>
          <Form.Item
            label="본문 (plain text)"
            name="body"
            rules={[{ required: true, message: '본문을 입력해 주세요' }, NO_ANGLE]}
            extra="HTML 태그(< >)는 사용할 수 없습니다."
          >
            <Input.TextArea rows={6} placeholder="공지 본문" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
