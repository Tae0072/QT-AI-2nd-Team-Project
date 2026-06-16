import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Input,
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
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  getSharingPost,
  hideSharingPost,
  listSharingPosts,
  restoreSharingPost,
  type AdminSharingPost,
  type SharingPostListParams,
  type SharingPostStatus,
} from '../api/sharingPosts';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-15 나눔 공유글 관리 (F-10) =====
// 전체 상태(공개/숨김/삭제) 목록 + 검색 + 보기(전체 본문 모달) + 숨김/복원.
// 권한: OPERATOR / SUPER_ADMIN. 신고 경유가 아니어도 공유글을 직접 관리할 수 있다.
// 모더레이션은 숨김/복원만 제공한다(하드 삭제 없음).

const STATUS_OPTIONS: { label: string; value: SharingPostStatus }[] = [
  { label: '공개(PUBLISHED)', value: 'PUBLISHED' },
  { label: '숨김(HIDDEN)', value: 'HIDDEN' },
  { label: '삭제(DELETED)', value: 'DELETED' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    PUBLISHED: { color: 'green', text: '공개' },
    HIDDEN: { color: 'orange', text: '숨김' },
    DELETED: { color: 'default', text: '삭제' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

// 모달에서 전체 본문을 보여주는 컴포넌트. 열릴 때 상세 API를 1회 호출한다.
function PostDetailView({ postId }: { postId: number }) {
  const [detail, setDetail] = useState<AdminSharingPost | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getSharingPost(postId)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : '상세를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [postId]);

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin /> <Typography.Text type="secondary">본문 불러오는 중…</Typography.Text>
      </div>
    );
  }
  if (error || !detail) {
    return <Typography.Text type="danger">{error ?? '상세 없음'}</Typography.Text>;
  }
  return (
    <Descriptions size="small" column={1} bordered>
      <Descriptions.Item label="상태">{statusTag(detail.status)}</Descriptions.Item>
      <Descriptions.Item label="작성자(닉네임)">
        {detail.nicknameSnapshot ?? '-'} (memberId: {detail.memberId})
      </Descriptions.Item>
      <Descriptions.Item label="제목">{detail.titleSnapshot}</Descriptions.Item>
      <Descriptions.Item label="카테고리">{detail.category}</Descriptions.Item>
      <Descriptions.Item label="QT 날짜">{detail.qtDate ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="인용 구절">{detail.verseLabel ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="본문">
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', margin: 0 }}>
          {detail.body && detail.body.length > 0 ? detail.body : '(본문 없음)'}
        </Typography.Paragraph>
      </Descriptions.Item>
      <Descriptions.Item label="좋아요 / 댓글">
        {detail.likeCount} / {detail.commentCount}
      </Descriptions.Item>
      <Descriptions.Item label="발행일">{formatDateTime(detail.createdAt)}</Descriptions.Item>
    </Descriptions>
  );
}

export default function SharingPostsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AdminSharingPost, SharingPostListParams>(listSharingPosts, {
      page: 0,
      size: 20,
    });

  const [status, setStatus] = useState<SharingPostStatus | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [viewTarget, setViewTarget] = useState<AdminSharingPost | null>(null);

  const onSearch = () => applyFilters({ status, q: keyword.trim() || undefined });

  const onReset = () => {
    setStatus(undefined);
    setKeyword('');
    applyFilters({ status: undefined, q: undefined });
  };

  const handleHide = async (id: number) => {
    try {
      await hideSharingPost(id);
      message.success('공유글을 숨겼습니다.');
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '숨김에 실패했습니다.');
    }
  };

  const handleRestore = async (id: number) => {
    try {
      await restoreSharingPost(id);
      message.success('공유글을 다시 공개했습니다.');
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '복원에 실패했습니다.');
    }
  };

  const columns: ColumnsType<AdminSharingPost> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '작성자(닉네임)',
      dataIndex: 'nicknameSnapshot',
      width: 140,
      render: (v: string | null) => v ?? '-',
    },
    { title: '제목', dataIndex: 'titleSnapshot' },
    { title: '카테고리', dataIndex: 'category', width: 110 },
    { title: '상태', dataIndex: 'status', width: 90, render: (v: string) => statusTag(v) },
    { title: '좋아요', dataIndex: 'likeCount', width: 80 },
    { title: '댓글', dataIndex: 'commentCount', width: 80 },
    {
      title: '발행일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '작업',
      key: 'actions',
      width: 190,
      render: (_: unknown, record: AdminSharingPost) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => setViewTarget(record)}>
            보기
          </Button>
          {record.status === 'HIDDEN' && (
            <Popconfirm
              title="공개 복원"
              description="이 공유글을 다시 공개하시겠습니까?"
              okText="복원"
              cancelText="취소"
              onConfirm={() => handleRestore(record.id)}
            >
              <Button size="small">복원</Button>
            </Popconfirm>
          )}
          {record.status === 'PUBLISHED' && (
            <Popconfirm
              title="공유글 숨김"
              description="이 공유글을 숨기시겠습니까?"
              okText="숨김"
              cancelText="취소"
              okButtonProps={{ danger: true }}
              onConfirm={() => handleHide(record.id)}
            >
              <Button size="small" danger>
                숨김
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
        <Space align="center">
          <Tag color="blue">AD-15</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            나눔 공유글 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          &lsquo;보기&rsquo; 버튼으로 전체 본문을 확인합니다. 신고 경유가 아니어도 공유글을 직접 숨김/복원하며,
          전체 상태(공개·숨김·삭제)를 조회합니다. 권한: OPERATOR / SUPER_ADMIN.
        </Typography.Paragraph>

        <Space wrap>
          <Input
            placeholder="제목·닉네임 검색"
            allowClear
            style={{ width: 240 }}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={onSearch}
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
        </Space>

        <Table<AdminSharingPost>
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
            showTotal: (t: number) => `총 ${t}건`,
            onChange: (p: number, ps: number) => changePage(p - 1, ps),
          }}
        />
      </Space>

      <Modal
        open={viewTarget != null}
        title={viewTarget ? `공유글 #${viewTarget.id} 상세` : '공유글 상세'}
        footer={null}
        width={720}
        onCancel={() => setViewTarget(null)}
        destroyOnHidden
      >
        {viewTarget && <PostDetailView postId={viewTarget.id} />}
      </Modal>
    </Card>
  );
}
