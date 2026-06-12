import { useState, type CSSProperties } from 'react';
import {
  Alert,
  Card,
  Descriptions,
  Divider,
  Drawer,
  Table,
  Tag,
  Typography,
  Space,
  Select,
  Button,
  Tooltip,
  Modal,
  Input,
  InputNumber,
  Checkbox,
  Spin,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons';
import {
  listAiAssets,
  getAiAsset,
  approveAiAsset,
  rejectAiAsset,
  hideAiAsset,
  regenerateAiAsset,
  type AiAsset,
  type AiAssetDetail,
  type AiAssetListParams,
  type AiValidationLog,
} from '../api/aiAssets';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';

// ===== AD-03 AI 산출물 검증 =====
// 목록(메타데이터만, 원문 비노출) + 필터 + 승인/반려/숨김 모달. 권한: REVIEWER / SUPER_ADMIN.

const ASSET_TYPE_OPTIONS = [
  { label: '해설(EXPLANATION)', value: 'EXPLANATION' },
  { label: '성경구절(BIBLE_VERSE)', value: 'BIBLE_VERSE' },
];

const STATUS_OPTIONS = [
  { label: '검증중(VALIDATING)', value: 'VALIDATING' },
  { label: '검토필요(NEEDS_REVIEW)', value: 'NEEDS_REVIEW' },
  { label: '승인(APPROVED)', value: 'APPROVED' },
  { label: '반려(REJECTED)', value: 'REJECTED' },
  { label: '숨김(HIDDEN)', value: 'HIDDEN' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    VALIDATING: { color: 'blue', text: '검증중' },
    NEEDS_REVIEW: { color: 'gold', text: '검토필요' },
    APPROVED: { color: 'green', text: '승인' },
    REJECTED: { color: 'red', text: '반려' },
    HIDDEN: { color: 'default', text: '숨김' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

function validationResultTag(result: string) {
  const map: Record<string, { color: string; text: string }> = {
    PASSED: { color: 'green', text: '통과' },
    FAILED: { color: 'red', text: '실패' },
    BLOCKED: { color: 'red', text: '차단' },
    WARNING: { color: 'gold', text: '경고' },
  };
  const m = map[result] ?? { color: 'default', text: result };
  return <Tag color={m.color}>{m.text}</Tag>;
}

const isReviewable = (s: string) => s === 'VALIDATING' || s === 'NEEDS_REVIEW';

function targetLabel(target: { targetType: string | null; targetId: number | null }) {
  return target.targetType
    ? `${target.targetType}${target.targetId != null ? ` #${target.targetId}` : ''}`
    : '-';
}

function promptVersionLabel(promptVersion: AiAsset['promptVersion']) {
  if (!promptVersion) return '-';
  const label = `${promptVersion.promptType ?? ''} ${promptVersion.version ?? ''}`.trim();
  return label || '-';
}

function formatJson(value: unknown) {
  if (value == null) return '-';
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

const preStyle: CSSProperties = {
  margin: '4px 0 0',
  padding: 8,
  background: '#f6f8fa',
  borderRadius: 6,
  maxHeight: 280,
  overflow: 'auto',
  fontSize: 12,
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-all',
};

type ActionMode = 'approve' | 'reject' | 'hide';

export default function AiAssetsPage() {
  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<AiAsset, AiAssetListParams>(listAiAssets, {
      page: 0,
      size: 20,
    });

  const [assetType, setAssetType] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<string | undefined>(undefined);

  const [action, setAction] = useState<{
    mode: ActionMode;
    asset: AiAsset;
  } | null>(null);
  const [reason, setReason] = useState('');
  const [activate, setActivate] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [selectedAsset, setSelectedAsset] = useState<AiAssetDetail | null>(null);
  const [regenerateOpen, setRegenerateOpen] = useState(false);
  const [regenerateReason, setRegenerateReason] = useState('');
  const [regeneratePromptVersionId, setRegeneratePromptVersionId] = useState<
    number | null
  >(null);
  const [regenerating, setRegenerating] = useState(false);

  const onSearch = () =>
    applyFilters({
      assetType: assetType || undefined,
      status: status || undefined,
    });
  const onReset = () => {
    setAssetType(undefined);
    setStatus(undefined);
    applyFilters({ assetType: undefined, status: undefined });
  };

  const openAction = (mode: ActionMode, asset: AiAsset) => {
    setAction({ mode, asset });
    setReason('');
    setActivate(true);
  };

  const closeDetail = () => {
    setDetailOpen(false);
    setSelectedAsset(null);
    setDetailError(null);
    setRegenerateOpen(false);
  };

  const loadDetail = async (assetId: number) => {
    setDetailLoading(true);
    setDetailError(null);
    try {
      const detail = await getAiAsset(assetId);
      setSelectedAsset(detail);
    } catch (e) {
      const errorMessage =
        e instanceof Error ? e.message : '상세 조회에 실패했습니다.';
      setDetailError(errorMessage);
      message.error(errorMessage);
    } finally {
      setDetailLoading(false);
    }
  };

  const openDetail = (asset: AiAsset) => {
    setDetailOpen(true);
    setSelectedAsset(null);
    void loadDetail(asset.id);
  };

  const openRegenerate = () => {
    if (!selectedAsset) return;
    setRegenerateReason('');
    setRegeneratePromptVersionId(
      selectedAsset.promptVersion?.id ?? selectedAsset.generationJob.promptVersionId ?? null,
    );
    setRegenerateOpen(true);
  };

  const submitRegenerate = async () => {
    if (!selectedAsset) return;
    const trimmedReason = regenerateReason.trim();
    if (!trimmedReason) {
      message.error('재생성 사유를 입력하세요.');
      return;
    }
    if (regeneratePromptVersionId == null || regeneratePromptVersionId <= 0) {
      message.error('프롬프트 버전 ID를 입력하세요.');
      return;
    }

    setRegenerating(true);
    try {
      const result = await regenerateAiAsset(selectedAsset.id, {
        reason: trimmedReason,
        promptVersionId: regeneratePromptVersionId,
      });
      message.success(
        `재생성 작업을 요청했습니다. job #${result.generationJobId} (${result.status})`,
      );
      setRegenerateOpen(false);
      await loadDetail(selectedAsset.id);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '재생성 요청에 실패했습니다.');
    } finally {
      setRegenerating(false);
    }
  };

  const submitAction = async () => {
    if (!action) return;
    setSubmitting(true);
    try {
      const id = action.asset.id;
      if (action.mode === 'approve') {
        await approveAiAsset(id, {
          reason: reason.trim() || undefined,
          activateForTarget: activate,
        });
        message.success('승인했습니다.');
      } else if (action.mode === 'reject') {
        await rejectAiAsset(id, reason.trim() || undefined);
        message.success('반려했습니다.');
      } else {
        await hideAiAsset(id, reason.trim() || undefined);
        message.success('숨김 처리했습니다.');
      }
      setAction(null);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const validationLogColumns: ColumnsType<AiValidationLog> = [
    { title: 'ID', dataIndex: 'validationLogId', width: 80 },
    {
      title: '시각',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    { title: 'Layer', dataIndex: 'layer', width: 80 },
    { title: '검토자', dataIndex: 'reviewerType', width: 110 },
    {
      title: '결과',
      dataIndex: 'result',
      width: 100,
      render: (v: string) => validationResultTag(v),
    },
    {
      title: '체크리스트',
      dataIndex: 'checklistVersionId',
      width: 110,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
    },
    {
      title: '참조 job',
      dataIndex: 'validationReferenceJobId',
      width: 110,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
    },
    {
      title: '오류',
      dataIndex: 'errorMessage',
      ellipsis: true,
      render: (v: string | null) => v ?? '-',
    },
  ];

  const columns: ColumnsType<AiAsset> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '생성일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '유형',
      dataIndex: 'assetType',
      width: 130,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    {
      title: '대상',
      width: 150,
      render: (_, r) => targetLabel(r),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 110,
      render: (v: string) => statusTag(v),
    },
    {
      title: '프롬프트버전',
      width: 150,
      render: (_, r) => promptVersionLabel(r.promptVersion),
    },
    {
      title: '검증결과',
      dataIndex: 'latestValidationResult',
      width: 140,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '원문라벨',
      dataIndex: 'sourceLabelPresent',
      width: 90,
      render: (v: boolean) =>
        v ? <Tag color="green">있음</Tag> : <Tag>없음</Tag>,
    },
    {
      title: '액션',
      width: 260,
      fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openDetail(r)}
          >
            상세
          </Button>
          {isReviewable(r.status) && (
            <>
              <Button
                size="small"
                type="primary"
                onClick={() => openAction('approve', r)}
              >
                승인
              </Button>
              <Button size="small" danger onClick={() => openAction('reject', r)}>
                반려
              </Button>
            </>
          )}
          {r.status === 'APPROVED' && (
            <Button size="small" onClick={() => openAction('hide', r)}>
              숨김
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const modalTitle =
    action?.mode === 'approve'
      ? 'AI 산출물 승인'
      : action?.mode === 'reject'
        ? 'AI 산출물 반려'
        : 'AI 산출물 숨김';

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-03</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 산출물 검증
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          AI 생성 해설·구절 산출물을 검토해 승인/반려/숨김 처리합니다. 권한:
          REVIEWER / SUPER_ADMIN.
          <br />
          승인 전 원문·검증 참조 자료는 목록에 노출하지 않습니다(메타데이터만
          표시).
        </Typography.Paragraph>

        <Space wrap>
          <Select
            placeholder="유형"
            allowClear
            style={{ width: 200 }}
            value={assetType}
            onChange={(v) => setAssetType(v)}
            options={ASSET_TYPE_OPTIONS}
          />
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 200 }}
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

        <Table<AiAsset>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <Typography.Text type="secondary">
                  메타데이터만 표시합니다. 산출물 원문·검증 참조 자료는 정책상
                  노출하지 않습니다(CLAUDE.md §7).
                </Typography.Text>
                <Typography.Text>
                  <Typography.Text strong>프롬프트 버전: </Typography.Text>
                  {r.promptVersion
                    ? `${r.promptVersion.promptType ?? '-'} / ${r.promptVersion.version ?? '-'} (${r.promptVersion.status ?? '-'}, id ${r.promptVersion.id ?? '-'})`
                    : '-'}
                </Typography.Text>
                <Typography.Text>
                  <Typography.Text strong>검증 체크리스트 버전 ID: </Typography.Text>
                  {r.checklistVersionId != null ? `#${r.checklistVersionId}` : '-'}
                  <Typography.Text type="secondary">
                    {' '}
                    (AD-09 검증 체크리스트 관리에서 버전을 확인할 수 있습니다)
                  </Typography.Text>
                </Typography.Text>
                <Typography.Text>
                  <Typography.Text strong>최신 검증 결과: </Typography.Text>
                  {r.latestValidationResult ?? '-'}
                </Typography.Text>
                <Typography.Text>
                  <Typography.Text strong>원문 라벨 존재: </Typography.Text>
                  {r.sourceLabelPresent ? '있음' : '없음'}
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

      <Drawer
        open={detailOpen}
        title={selectedAsset ? `AI 산출물 #${selectedAsset.id}` : 'AI 산출물 상세'}
        width={760}
        onClose={closeDetail}
        destroyOnClose
        extra={
          selectedAsset ? (
            <Button
              icon={<SyncOutlined />}
              loading={regenerating}
              onClick={openRegenerate}
            >
              재생성
            </Button>
          ) : null
        }
      >
        {detailLoading && !selectedAsset ? (
          <Spin />
        ) : (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {detailError && <Alert type="error" showIcon message={detailError} />}
            {selectedAsset && (
              <>
                <Descriptions bordered size="small" column={1}>
                  <Descriptions.Item label="유형">
                    <Tag>{selectedAsset.assetType}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="대상">
                    {targetLabel(selectedAsset)}
                  </Descriptions.Item>
                  <Descriptions.Item label="상태">
                    {statusTag(selectedAsset.status)}
                  </Descriptions.Item>
                  <Descriptions.Item label="생성일">
                    {formatDateTime(selectedAsset.createdAt)}
                  </Descriptions.Item>
                  <Descriptions.Item label="검토일">
                    {formatDateTime(selectedAsset.reviewedAt)}
                  </Descriptions.Item>
                  <Descriptions.Item label="원문 라벨">
                    {selectedAsset.sourceLabel ? (
                      <Typography.Text code>{selectedAsset.sourceLabel}</Typography.Text>
                    ) : (
                      '-'
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="프롬프트 버전">
                    {selectedAsset.promptVersion
                      ? `${selectedAsset.promptVersion.promptType ?? '-'} / ${
                          selectedAsset.promptVersion.version ?? '-'
                        } (${selectedAsset.promptVersion.status ?? '-'}, id ${
                          selectedAsset.promptVersion.id ?? '-'
                        })`
                      : '-'}
                  </Descriptions.Item>
                </Descriptions>

                <Divider orientation="left">생성 작업</Divider>
                <Descriptions bordered size="small" column={1}>
                  <Descriptions.Item label="Job ID">
                    #{selectedAsset.generationJob.id}
                  </Descriptions.Item>
                  <Descriptions.Item label="유형">
                    {selectedAsset.generationJob.jobType}
                  </Descriptions.Item>
                  <Descriptions.Item label="대상">
                    {targetLabel(selectedAsset.generationJob)}
                  </Descriptions.Item>
                  <Descriptions.Item label="프롬프트 버전 ID">
                    #{selectedAsset.generationJob.promptVersionId}
                  </Descriptions.Item>
                  <Descriptions.Item label="상태">
                    <Tag>{selectedAsset.generationJob.status}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="시작/종료">
                    {formatDateTime(selectedAsset.generationJob.startedAt)} /{' '}
                    {formatDateTime(selectedAsset.generationJob.finishedAt)}
                  </Descriptions.Item>
                  <Descriptions.Item label="오류">
                    {selectedAsset.generationJob.errorMessage ?? '-'}
                  </Descriptions.Item>
                </Descriptions>

                <Divider orientation="left">Payload JSON</Divider>
                <pre style={preStyle}>{formatJson(selectedAsset.payloadJson)}</pre>

                <Divider orientation="left">검증 로그</Divider>
                <Table<AiValidationLog>
                  rowKey="validationLogId"
                  size="small"
                  columns={validationLogColumns}
                  dataSource={selectedAsset.validationLogs}
                  pagination={false}
                  scroll={{ x: 'max-content' }}
                  expandable={{
                    expandedRowRender: (r) => (
                      <pre style={preStyle}>{r.errorMessage ?? '-'}</pre>
                    ),
                    rowExpandable: (r) => Boolean(r.errorMessage),
                  }}
                />
              </>
            )}
          </Space>
        )}
      </Drawer>

      <Modal
        open={regenerateOpen}
        title="AI 산출물 재생성"
        okText="재생성 요청"
        cancelText="취소"
        confirmLoading={regenerating}
        onOk={submitRegenerate}
        onCancel={() => setRegenerateOpen(false)}
        destroyOnClose
      >
        {selectedAsset && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              {selectedAsset.assetType} · 대상 {targetLabel(selectedAsset)} · 산출물 #
              {selectedAsset.id}
            </Typography.Text>
            <div>
              <Typography.Text>사유</Typography.Text>
              <Input.TextArea
                rows={3}
                value={regenerateReason}
                onChange={(e) => setRegenerateReason(e.target.value)}
                placeholder="재생성 사유"
                style={{ marginTop: 4 }}
              />
            </div>
            <div>
              <Typography.Text>프롬프트 버전 ID</Typography.Text>
              <InputNumber
                min={1}
                precision={0}
                value={regeneratePromptVersionId ?? undefined}
                onChange={(value) =>
                  setRegeneratePromptVersionId(
                    typeof value === 'number' ? value : null,
                  )
                }
                style={{ display: 'block', marginTop: 4, width: '100%' }}
              />
            </div>
          </Space>
        )}
      </Modal>

      <Modal
        open={action !== null}
        title={modalTitle}
        okText={
          action?.mode === 'approve'
            ? '승인'
            : action?.mode === 'reject'
              ? '반려'
              : '숨김'
        }
        okButtonProps={{ danger: action?.mode === 'reject', loading: submitting }}
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitAction}
        onCancel={() => setAction(null)}
        destroyOnClose
      >
        {action && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              {action.asset.assetType} · 대상 {action.asset.targetType ?? '-'}
              {action.asset.targetId != null
                ? ` #${action.asset.targetId}`
                : ''}{' '}
              · 산출물 #{action.asset.id}
            </Typography.Text>
            <div>
              <Typography.Text>사유 (선택)</Typography.Text>
              <Input.TextArea
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="검토 사유를 남길 수 있습니다."
                style={{ marginTop: 4 }}
              />
            </div>
            {action.mode === 'approve' && (
              <Checkbox
                checked={activate}
                onChange={(e) => setActivate(e.target.checked)}
              >
                승인과 함께 대상에 게시(활성화)
              </Checkbox>
            )}
          </Space>
        )}
      </Modal>
    </Card>
  );
}
