import { useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Select,
  Button,
  Popconfirm,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSearchParams } from 'react-router-dom';
import {
  listSimulatorClips,
  hideSimulatorClip,
  type SimulatorClip,
  type SimulatorClipListParams,
} from '../api/simulatorClips';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-14 시뮬레이터 관리 (조회 + 숨김) =====
// SimulatorClip(study) 목록·상태 + APPROVED 클립 숨김. 게시(Publish)는 후속(AD-03 승인본 연동).
// 권한: REVIEWER / SUPER_ADMIN (AdminSimulatorClipController.requireReviewer).

const STATUS_OPTIONS = [
  { label: '검토 대기(PENDING)', value: 'PENDING' },
  { label: '게시됨(APPROVED)', value: 'APPROVED' },
  { label: '반려(REJECTED)', value: 'REJECTED' },
  { label: '숨김(HIDDEN)', value: 'HIDDEN' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    PENDING: { color: 'gold', text: '검토 대기' },
    APPROVED: { color: 'green', text: '게시됨' },
    REJECTED: { color: 'red', text: '반려' },
    HIDDEN: { color: 'default', text: '숨김' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

export default function SimulatorClipsPage() {
  // 대시보드 시뮬레이터 CTA가 ?qtPassageId= 로 진입하면 해당 본문으로 초기 필터.
  const [searchParams] = useSearchParams();
  const initialPassageId = Number(searchParams.get('qtPassageId')) || undefined;

  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<SimulatorClip, SimulatorClipListParams>(listSimulatorClips, {
      page: 0,
      size: 20,
      qtPassageId: initialPassageId,
    });

  const [status, setStatus] = useState<string | undefined>(undefined);
  const [mutating, setMutating] = useState(false);

  const onSearch = () => applyFilters({ status: status || undefined });
  const onReset = () => {
    setStatus(undefined);
    applyFilters({ status: undefined, qtPassageId: undefined });
  };

  const onHide = async (aiAssetId: number) => {
    setMutating(true);
    try {
      const r = await hideSimulatorClip(aiAssetId);
      message.success(`숨김 처리되었습니다 (${r.hiddenCount}건)`);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '숨김 처리에 실패했습니다.');
    } finally {
      setMutating(false);
    }
  };

  const columns: ColumnsType<SimulatorClip> = [
    { title: 'QT 본문', dataIndex: 'qtPassageId', width: 110, render: (v: number) => `#${v}` },
    { title: '제목', dataIndex: 'title', ellipsis: true },
    {
      title: '상태',
      dataIndex: 'status',
      width: 120,
      render: (s: string) => statusTag(s),
    },
    {
      title: 'AI 산출물',
      dataIndex: 'aiAssetId',
      width: 110,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
    },
    {
      title: '승인 시각',
      dataIndex: 'approvedAt',
      width: 170,
      render: (v: string | null) => formatDateTime(v),
    },
    {
      title: '작업',
      width: 120,
      render: (_, r) =>
        r.status === 'APPROVED' && r.aiAssetId != null ? (
          <Popconfirm
            title="이 시뮬레이터 클립을 숨길까요?"
            description="사용자 노출이 중단됩니다."
            okText="숨김"
            cancelText="취소"
            onConfirm={() => onHide(r.aiAssetId as number)}
          >
            <Button size="small" danger disabled={mutating}>
              숨김
            </Button>
          </Popconfirm>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-14</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            시뮬레이터 관리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          QT 본문 시뮬레이터 클립을 조회하고 노출 중인 클립을 숨깁니다. (게시는 후속 — AI 산출물 승인 연동)
          {initialPassageId ? ` · 본문 #${initialPassageId} 필터 적용 중` : ''}
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
        </Space>

        <Table<SimulatorClip>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: '시뮬레이터 클립이 없습니다' }}
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
    </Card>
  );
}
