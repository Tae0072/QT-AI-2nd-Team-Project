import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Input,
  List,
  Popconfirm,
  Modal,
  Progress,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  getMemberDetail,
  getMemberMissions,
  listMembers,
  listMemberComments,
  listMemberLikes,
  listMemberNotes,
  listMemberPosts,
  updateMemberStatus,
  type AdminMember,
  type AdminMemberCommentItem,
  type AdminMemberDetail,
  type AdminMemberLikedPostItem,
  type AdminMemberPostItem,
  type AdminNoteItem,
  type MemberListParams,
  type MemberStatus,
  type MissionProgress,
} from '../api/members';
import type { Page, PageParams } from '../api/types';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-12 회원 관리 (F-04 / F-10) =====
// 닉네임 검색 + 상태 필터 + 서버 페이지네이션 + 상세 보기(닉네임 변경 시각·신고/나눔 집계) + 정지/정지해제.
// 권한: OPERATOR / SUPER_ADMIN. 개인정보(이메일·카카오ID)는 표시하지 않는다.

const STATUS_OPTIONS: { label: string; value: MemberStatus }[] = [
  { label: '활성(ACTIVE)', value: 'ACTIVE' },
  { label: '정지(SUSPENDED)', value: 'SUSPENDED' },
  { label: '탈퇴(WITHDRAWN)', value: 'WITHDRAWN' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    ACTIVE: { color: 'green', text: '활성' },
    SUSPENDED: { color: 'red', text: '정지' },
    WITHDRAWN: { color: 'default', text: '탈퇴' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

// 회원 상세 — 요약 정보(닉네임 변경 시각·신고/나눔 집계).
function MemberSummary({ memberId }: { memberId: number }) {
  const [detail, setDetail] = useState<AdminMemberDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getMemberDetail(memberId)
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
  }, [memberId]);

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin /> <Typography.Text type="secondary">불러오는 중…</Typography.Text>
      </div>
    );
  }
  if (error || !detail) {
    return <Typography.Text type="danger">{error ?? '상세 없음'}</Typography.Text>;
  }
  return (
    <Descriptions size="small" column={1} bordered>
      <Descriptions.Item label="ID">{detail.id}</Descriptions.Item>
      <Descriptions.Item label="닉네임">{detail.nickname}</Descriptions.Item>
      <Descriptions.Item label="권한">{detail.role}</Descriptions.Item>
      <Descriptions.Item label="상태">{statusTag(detail.status)}</Descriptions.Item>
      <Descriptions.Item label="가입일">{formatDateTime(detail.createdAt)}</Descriptions.Item>
      <Descriptions.Item label="최근 닉네임 변경">
        {detail.nicknameChangedAt ? formatDateTime(detail.nicknameChangedAt) : '변경 이력 없음'}
      </Descriptions.Item>
      <Descriptions.Item label="탈퇴일">
        {detail.withdrawnAt ? formatDateTime(detail.withdrawnAt) : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="작성 공유글 수">{detail.sharingPostCount}건</Descriptions.Item>
      <Descriptions.Item label="신고한 횟수(신고자)">{detail.reportsFiledCount}건</Descriptions.Item>
      <Descriptions.Item label="받은 신고 수(공유글·댓글)">
        <Typography.Text type={detail.reportsReceivedCount > 0 ? 'danger' : undefined}>
          {detail.reportsReceivedCount}건
        </Typography.Text>
      </Descriptions.Item>
    </Descriptions>
  );
}

// 회원 상세 — 페이징 서브 목록(노트/공유글/댓글/좋아요). 탭이 처음 열릴 때 1회 로드.
function PagedSubTable<T extends object>({
  fetcher,
  columns,
}: {
  fetcher: (params: PageParams) => Promise<Page<T>>;
  columns: ColumnsType<T>;
}) {
  const [data, setData] = useState<Page<T> | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetcher({ page, size })
      .then((p) => {
        if (!cancelled) setData(p);
      })
      .catch(() => {
        if (!cancelled) setData({ content: [], page, size, totalElements: 0, totalPages: 0 });
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [fetcher, page, size]);

  return (
    <Table<T>
      rowKey={(_, idx) => String(idx)}
      size="small"
      loading={loading}
      columns={columns}
      dataSource={data?.content ?? []}
      scroll={{ x: 'max-content' }}
      pagination={{
        current: page + 1,
        pageSize: size,
        total: data?.totalElements ?? 0,
        showSizeChanger: true,
        showTotal: (t: number) => `총 ${t}건`,
        onChange: (p: number, ps: number) => {
          setPage(p - 1);
          setSize(ps);
        },
      }}
    />
  );
}

// 회원 상세 — 미션 진행률(페이징 없이 전체).
function MissionPanel({ memberId }: { memberId: number }) {
  const [rows, setRows] = useState<MissionProgress[] | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getMemberMissions(memberId)
      .then((r) => {
        if (!cancelled) setRows(r);
      })
      .catch(() => {
        if (!cancelled) setRows([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [memberId]);

  if (loading) return <Spin />;
  if (!rows || rows.length === 0) return <Empty description="진행 중인 미션 없음" />;
  return (
    <List
      size="small"
      dataSource={rows}
      renderItem={(m) => (
        <List.Item>
          <div style={{ width: '100%' }}>
            <Space style={{ justifyContent: 'space-between', width: '100%' }}>
              <Typography.Text strong>{m.title}</Typography.Text>
              <Typography.Text type="secondary">
                {m.currentCount}/{m.targetCount} ({m.periodType})
                {m.completed ? ' · 완료' : ''}
              </Typography.Text>
            </Space>
            <Progress
              percent={Math.round(Number(m.progressRate) || 0)}
              status={m.completed ? 'success' : 'active'}
              size="small"
            />
          </div>
        </List.Item>
      )}
    />
  );
}

const noteColumns: ColumnsType<AdminNoteItem> = [
  { title: '제목', dataIndex: 'title', render: (v: string | null) => v || '(제목 없음)' },
  { title: '분류', dataIndex: 'category', width: 110, render: (v: string | null) => v ?? '-' },
  { title: '상태', dataIndex: 'status', width: 100, render: (v: string | null) => v ?? '-' },
  { title: '공개', dataIndex: 'visibility', width: 100, render: (v: string | null) => v ?? '-' },
  { title: '작성일', dataIndex: 'createdAt', width: 160, render: (v: string) => formatDateTime(v) },
];

const postColumns: ColumnsType<AdminMemberPostItem> = [
  { title: '제목', dataIndex: 'title', render: (v: string | null) => v || '(제목 없음)' },
  { title: '분류', dataIndex: 'category', width: 110, render: (v: string | null) => v ?? '-' },
  { title: '상태', dataIndex: 'status', width: 110, render: (v: string | null) => v ?? '-' },
  { title: '작성일', dataIndex: 'createdAt', width: 160, render: (v: string) => formatDateTime(v) },
];

const commentColumns: ColumnsType<AdminMemberCommentItem> = [
  { title: '내용', dataIndex: 'body', render: (v: string | null) => v || '(삭제됨)' },
  { title: '글ID', dataIndex: 'sharingPostId', width: 80 },
  {
    title: '삭제',
    dataIndex: 'deleted',
    width: 70,
    render: (v: boolean) => (v ? <Tag color="default">삭제</Tag> : <Tag color="green">유지</Tag>),
  },
  { title: '작성일', dataIndex: 'createdAt', width: 160, render: (v: string) => formatDateTime(v) },
];

const likeColumns: ColumnsType<AdminMemberLikedPostItem> = [
  { title: '글 제목', dataIndex: 'title', render: (v: string | null) => v || '(삭제된 글)' },
  { title: '글ID', dataIndex: 'postId', width: 80 },
  { title: '글 상태', dataIndex: 'status', width: 110, render: (v: string | null) => v ?? '-' },
  { title: '좋아요한 시각', dataIndex: 'likedAt', width: 160, render: (v: string) => formatDateTime(v) },
];

// 회원 상세 모달 본문 — 탭으로 요약/노트/공유글/댓글/좋아요/미션을 본다.
function MemberDetailView({ memberId }: { memberId: number }) {
  return (
    <Tabs
      defaultActiveKey="summary"
      destroyInactiveTabPane
      items={[
        { key: 'summary', label: '요약', children: <MemberSummary memberId={memberId} /> },
        {
          key: 'notes',
          label: '노트',
          children: <PagedSubTable fetcher={(p) => listMemberNotes(memberId, p)} columns={noteColumns} />,
        },
        {
          key: 'posts',
          label: '공유글',
          children: <PagedSubTable fetcher={(p) => listMemberPosts(memberId, p)} columns={postColumns} />,
        },
        {
          key: 'comments',
          label: '댓글',
          children: <PagedSubTable fetcher={(p) => listMemberComments(memberId, p)} columns={commentColumns} />,
        },
        {
          key: 'likes',
          label: '좋아요',
          children: <PagedSubTable fetcher={(p) => listMemberLikes(memberId, p)} columns={likeColumns} />,
        },
        { key: 'missions', label: '미션', children: <MissionPanel memberId={memberId} /> },
      ]}
    />
  );
}

export default function MembersPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AdminMember, MemberListParams>(listMembers, { page: 0, size: 20 });

  const [status, setStatus] = useState<MemberStatus | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [viewTarget, setViewTarget] = useState<AdminMember | null>(null);

  const onSearch = () =>
    applyFilters({ status, q: keyword.trim() || undefined });

  const onReset = () => {
    setStatus(undefined);
    setKeyword('');
    applyFilters({ status: undefined, q: undefined });
  };

  const changeStatus = async (record: AdminMember, next: 'ACTIVE' | 'SUSPENDED') => {
    try {
      await updateMemberStatus(record.id, { status: next });
      message.success(next === 'SUSPENDED' ? '회원을 정지했습니다.' : '정지를 해제했습니다.');
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '상태 변경에 실패했습니다.');
    }
  };

  const columns: ColumnsType<AdminMember> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '닉네임', dataIndex: 'nickname' },
    { title: '권한', dataIndex: 'role', width: 110, render: (v: string) => v ?? '-' },
    { title: '상태', dataIndex: 'status', width: 90, render: (v: string) => statusTag(v) },
    {
      title: '닉네임 변경일',
      dataIndex: 'nicknameChangedAt',
      width: 160,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '가입일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '작업',
      key: 'actions',
      width: 200,
      render: (_: unknown, record: AdminMember) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => setViewTarget(record)}>
            상세
          </Button>
          {record.status === 'WITHDRAWN' ? null : record.status === 'SUSPENDED' ? (
            <Popconfirm
              title="정지 해제"
              description="이 회원의 정지를 해제하시겠습니까?"
              okText="해제"
              cancelText="취소"
              onConfirm={() => changeStatus(record, 'ACTIVE')}
            >
              <Button size="small">정지 해제</Button>
            </Popconfirm>
          ) : (
            <Popconfirm
              title="회원 정지"
              description="이 회원을 정지하시겠습니까?"
              okText="정지"
              cancelText="취소"
              okButtonProps={{ danger: true }}
              onConfirm={() => changeStatus(record, 'SUSPENDED')}
            >
              <Button size="small" danger>
                정지
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
          <Tag color="blue">AD-12</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            회원 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          닉네임 검색·상태 필터로 회원을 찾고, &lsquo;상세&rsquo;로 닉네임 변경 시각·신고/나눔 집계를 확인합니다.
          신고 회피·트롤 대응을 위해 정지/정지해제할 수 있습니다. 권한: OPERATOR / SUPER_ADMIN.
          개인정보(이메일·카카오ID)는 표시하지 않습니다.
        </Typography.Paragraph>

        <Space wrap>
          <Input
            placeholder="닉네임 검색"
            allowClear
            style={{ width: 220 }}
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

        <Table<AdminMember>
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
            showTotal: (t: number) => `총 ${t}명`,
            onChange: (p: number, ps: number) => changePage(p - 1, ps),
          }}
        />
      </Space>

      <Modal
        open={viewTarget != null}
        title={viewTarget ? `회원 #${viewTarget.id} 상세` : '회원 상세'}
        footer={null}
        width={880}
        onCancel={() => setViewTarget(null)}
        destroyOnHidden
      >
        {viewTarget && <MemberDetailView memberId={viewTarget.id} />}
      </Modal>
    </Card>
  );
}
