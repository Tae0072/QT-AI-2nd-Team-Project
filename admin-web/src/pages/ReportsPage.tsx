import { useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Select,
  Button,
  Tooltip,
  Modal,
  Input,
  Checkbox,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import {
  listReports,
  resolveReport,
  rejectReport,
  type Report,
  type ReportListParams,
} from '../api/reports';
import {
  listEvaluationSets,
  createReportEvaluationCandidate,
  type EvaluationSet,
} from '../api/aiEvaluations';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import {
  buildReportProcessPayload,
  isAiReport,
  isOpenReportStatus,
  reportEvaluationSetListParams,
} from './adminPageContracts';

// ===== AD-04 신고 처리 =====
// 목록 + 필터(상태/대상 유형) + 서버 페이지네이션 + 처리(resolve)/반려(reject) 모달.
// 권한: OPERATOR / SUPER_ADMIN (백엔드 requireOperator 에서 검증).

const STATUS_OPTIONS = [
  { label: '접수(RECEIVED)', value: 'RECEIVED' },
  { label: '검토중(REVIEWING)', value: 'REVIEWING' },
  { label: '처리완료(RESOLVED)', value: 'RESOLVED' },
  { label: '반려(REJECTED)', value: 'REJECTED' },
];

const TARGET_TYPE_OPTIONS = [
  { label: '나눔글(POST)', value: 'POST' },
  { label: '댓글(COMMENT)', value: 'COMMENT' },
  { label: 'AI Q&A(AI_QA_REQUEST)', value: 'AI_QA_REQUEST' },
  { label: 'AI 산출물(AI_ASSET)', value: 'AI_ASSET' },
];

function statusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    RECEIVED: { color: 'blue', text: '접수' },
    REVIEWING: { color: 'gold', text: '검토중' },
    RESOLVED: { color: 'green', text: '처리완료' },
    REJECTED: { color: 'red', text: '반려' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

export default function ReportsPage() {
  // 대시보드(AD-01) 신고 CTA가 ?status=RECEIVED|REVIEWING 으로 진입할 때 초기 필터를 맞춘다.
  // 허용된 상태값만 수용하고, 그 외/없음이면 전체 조회.
  const [searchParams] = useSearchParams();
  const initialStatus = STATUS_OPTIONS.some((o) => o.value === searchParams.get('status'))
    ? (searchParams.get('status') as string)
    : undefined;

  const { rows, page, size, total, loading, applyFilters, changePage, reload } =
    usePagedList<Report, ReportListParams>(listReports, {
      page: 0,
      size: 20,
      status: initialStatus,
    });

  const [status, setStatus] = useState<string | undefined>(initialStatus);
  const [targetType, setTargetType] = useState<string | undefined>(undefined);

  // 처리/반려 모달 상태
  const [action, setAction] = useState<{
    mode: 'resolve' | 'reject';
    report: Report;
  } | null>(null);
  const [reason, setReason] = useState('');
  const [notify, setNotify] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // 평가 항목으로 등록(USER_REPORT) 모달 — FE는 평가 세트만 고르고, 백엔드가 신고 메타로 inputJson 조립.
  const [candidateReport, setCandidateReport] = useState<Report | null>(null);
  const [candidateSetId, setCandidateSetId] = useState<number | null>(null);
  const [candidateSets, setCandidateSets] = useState<EvaluationSet[]>([]);
  const [candidateSetsLoading, setCandidateSetsLoading] = useState(false);
  const [candidateSubmitting, setCandidateSubmitting] = useState(false);

  const onSearch = () =>
    applyFilters({
      status: status || undefined,
      targetType: targetType || undefined,
    });

  const onReset = () => {
    setStatus(undefined);
    setTargetType(undefined);
    applyFilters({ status: undefined, targetType: undefined });
  };

  const openAction = (mode: 'resolve' | 'reject', report: Report) => {
    setAction({ mode, report });
    setReason('');
    setNotify(true);
  };

  const submitAction = async () => {
    if (!action) return;
    setSubmitting(true);
    try {
      const payload = buildReportProcessPayload(
        action.mode,
        action.report,
        reason,
        notify,
      );
      if (action.mode === 'resolve') {
        await resolveReport(action.report.id, payload);
        message.success('신고를 처리(인정)했습니다.');
      } else {
        await rejectReport(action.report.id, payload);
        message.success('신고를 반려했습니다.');
      }
      setAction(null);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const openCandidate = async (report: Report) => {
    setCandidateReport(report);
    setCandidateSetId(null);
    setCandidateSetsLoading(true);
    try {
      // AI_QA_REQUEST → QA_REQUEST 평가 세트만. AI_ASSET은 대상유형을 모르니 전체 로드(백엔드 검증).
      const res = await listEvaluationSets(reportEvaluationSetListParams(report));
      setCandidateSets(res.content);
    } catch (e) {
      message.error(
        e instanceof Error ? e.message : '평가 세트 목록을 불러오지 못했습니다.',
      );
    } finally {
      setCandidateSetsLoading(false);
    }
  };

  const submitCandidate = async () => {
    if (!candidateReport) return;
    if (candidateSetId == null) {
      message.error('평가 세트를 선택하세요.');
      return;
    }
    setCandidateSubmitting(true);
    try {
      await createReportEvaluationCandidate(candidateReport.id, {
        evaluationSetId: candidateSetId,
      });
      message.success('평가 항목으로 등록했습니다.');
      setCandidateReport(null);
    } catch (e) {
      message.error(
        e instanceof Error ? e.message : '평가 항목 등록에 실패했습니다.',
      );
    } finally {
      setCandidateSubmitting(false);
    }
  };

  const columns: ColumnsType<Report> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '신고일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '대상',
      width: 180,
      render: (_, r) =>
        `${r.targetType}${r.targetId != null ? ` #${r.targetId}` : ''}`,
    },
    {
      title: '사유',
      dataIndex: 'reason',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '신고자',
      dataIndex: 'reporterMemberId',
      width: 90,
      render: (v: number | null) => (v != null ? `#${v}` : '-'),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 110,
      render: (v: string) => statusTag(v),
    },
    {
      title: '처리',
      width: 190,
      render: (_, r) =>
        r.processedAt
          ? `${formatDateTime(r.processedAt)}${r.processedByAdminId != null ? ` (admin #${r.processedByAdminId})` : ''}`
          : '-',
    },
    {
      title: '액션',
      width: 300,
      fixed: 'right',
      render: (_, r) => (
        <Space wrap>
          {isOpenReportStatus(r.status) && (
            <>
              <Button
                size="small"
                type="primary"
                onClick={() => openAction('resolve', r)}
              >
                처리
              </Button>
              <Button size="small" danger onClick={() => openAction('reject', r)}>
                반려
              </Button>
            </>
          )}
          {isAiReport(r.targetType) && (
            <Button size="small" onClick={() => openCandidate(r)}>
              평가 항목으로 등록
            </Button>
          )}
          {!isOpenReportStatus(r.status) && !isAiReport(r.targetType) && (
            <Typography.Text type="secondary">완료</Typography.Text>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-04</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            신고 처리
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          나눔 글·댓글 신고를 검토하고 처리(인정)하거나 반려합니다. 권한:
          OPERATOR / SUPER_ADMIN. AI 신고(AI Q&A·AI 산출물)는 ‘평가 항목으로 등록’으로
          회귀 평가 세트에 추가할 수 있습니다(원문 미저장, 식별자·메타만).
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
          <Select
            placeholder="대상 유형"
            allowClear
            style={{ width: 210 }}
            value={targetType}
            onChange={(v) => setTargetType(v)}
            options={TARGET_TYPE_OPTIONS}
          />
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
        </Space>

        <Table<Report>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Typography.Paragraph
                style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}
              >
                <Typography.Text strong>상세 내용: </Typography.Text>
                {r.detail ?? '-'}
              </Typography.Paragraph>
            ),
            rowExpandable: (r) => Boolean(r.detail),
          }}
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

      <Modal
        open={action !== null}
        title={action?.mode === 'resolve' ? '신고 처리(인정)' : '신고 반려'}
        okText={action?.mode === 'resolve' ? '처리' : '반려'}
        okButtonProps={{ danger: action?.mode === 'reject', loading: submitting }}
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitAction}
        onCancel={() => setAction(null)}
        destroyOnHidden
      >
        {action && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              대상 {action.report.targetType}
              {action.report.targetId != null
                ? ` #${action.report.targetId}`
                : ''}{' '}
              · 신고 #{action.report.id}
            </Typography.Text>
            {action.mode === 'resolve' && action.report.targetType === 'POST' && (
              <Typography.Text type="warning">
                처리 시 대상 나눔글이 숨김 처리됩니다.
              </Typography.Text>
            )}
            <div>
              <Typography.Text>처리 사유 (선택)</Typography.Text>
              <Input.TextArea
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="처리/반려 사유를 남길 수 있습니다."
                style={{ marginTop: 4 }}
              />
            </div>
            <Checkbox
              checked={notify}
              onChange={(e) => setNotify(e.target.checked)}
            >
              신고자에게 결과 알림 보내기
            </Checkbox>
          </Space>
        )}
      </Modal>

      <Modal
        open={candidateReport !== null}
        title="평가 항목으로 등록"
        okText="등록"
        cancelText="취소"
        confirmLoading={candidateSubmitting}
        onOk={submitCandidate}
        onCancel={() => setCandidateReport(null)}
        destroyOnHidden
      >
        {candidateReport && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              이 AI 신고를 평가 세트의 평가 항목으로 등록합니다. 신고 원문이 아니라
              식별자·메타데이터만 저장됩니다. · 신고 #{candidateReport.id} · 대상{' '}
              {candidateReport.targetType}
              {candidateReport.targetId != null
                ? ` #${candidateReport.targetId}`
                : ''}
            </Typography.Text>
            <div>
              <Typography.Text>
                평가 세트 선택 <Typography.Text type="danger">*</Typography.Text>
              </Typography.Text>
              <Select
                style={{ display: 'block', marginTop: 4, width: '100%' }}
                placeholder="평가 세트 선택"
                loading={candidateSetsLoading}
                value={candidateSetId ?? undefined}
                onChange={(v) => setCandidateSetId(v ?? null)}
                options={candidateSets.map((s) => ({
                  label: `#${s.id} ${s.name} · ${s.version} [${s.status}]`,
                  value: s.id,
                }))}
                notFoundContent={
                  candidateSetsLoading
                    ? '불러오는 중...'
                    : '등록 가능한 평가 세트가 없습니다'
                }
              />
            </div>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              없으면 먼저 ‘AI 평가 세트’ 화면에서 만들어 주세요. (AI Q&A 신고는
              QA_REQUEST 세트만 보입니다.)
            </Typography.Text>
          </Space>
        )}
      </Modal>
    </Card>
  );
}
