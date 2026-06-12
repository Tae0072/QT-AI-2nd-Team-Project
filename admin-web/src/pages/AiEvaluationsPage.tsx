import { useMemo, useState } from 'react';
import {
  Card,
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
  Form,
  Popconfirm,
  Alert,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import {
  listEvaluationSets,
  createEvaluationSet,
  activateEvaluationSet,
  retireEvaluationSet,
  listEvaluationCases,
  createEvaluationCase,
  approveEvaluationCase,
  rejectEvaluationCase,
  type EvaluationSet,
  type EvaluationSetListParams,
  type EvaluationCase,
  type EvaluationCaseListParams,
} from '../api/aiEvaluations';
import { usePagedList } from '../hooks/usePagedList';
import { formatDateTime } from '../utils/datetime';
import { useAuth } from '../auth/useAuth';
import { ADMIN_ROLES, canAccessAdminRoute } from '../constants/roles';

// ===== AD-11 AI 평가셋/평가케이스 관리 =====
// 평가셋(목록/생성/활성·폐기) → 셋 선택 시 케이스(목록/생성/승인·반려)를 드로어로 관리한다.
// 권한: 평가셋·케이스 관리 = REVIEWER/CONTENT_CREATOR(SUPER_ADMIN 포함). 케이스 승인/반려 = REVIEWER만.
// AI Q&A·해설 회귀 평가의 기준 메타데이터만 다룬다(원문/검증 참조 자료는 노출하지 않음, CLAUDE.md §7).

const EVAL_TYPE_OPTIONS = [
  { label: '해설(EXPLANATION)', value: 'EXPLANATION' },
  { label: '시뮬레이터(SIMULATOR)', value: 'SIMULATOR' },
  { label: 'Q&A(QA)', value: 'QA' },
];

const TARGET_TYPE_OPTIONS = [
  { label: '성경 절(BIBLE_VERSE)', value: 'BIBLE_VERSE' },
  { label: 'QT 본문(QT_PASSAGE)', value: 'QT_PASSAGE' },
  { label: 'Q&A 요청(QA_REQUEST)', value: 'QA_REQUEST' },
];

const SET_STATUS_OPTIONS = [
  { label: '초안(DRAFT)', value: 'DRAFT' },
  { label: '활성(ACTIVE)', value: 'ACTIVE' },
  { label: '폐기(RETIRED)', value: 'RETIRED' },
];

const CASE_STATUS_OPTIONS = [
  { label: '후보(CANDIDATE)', value: 'CANDIDATE' },
  { label: '승인(APPROVED)', value: 'APPROVED' },
  { label: '반려(REJECTED)', value: 'REJECTED' },
];

function setStatusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    DRAFT: { color: 'gold', text: '초안' },
    ACTIVE: { color: 'green', text: '활성' },
    RETIRED: { color: 'default', text: '폐기' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

function caseStatusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    CANDIDATE: { color: 'gold', text: '후보' },
    APPROVED: { color: 'green', text: '승인' },
    REJECTED: { color: 'red', text: '반려' },
  };
  const m = map[status] ?? { color: 'default', text: status };
  return <Tag color={m.color}>{m.text}</Tag>;
}

// 응답의 JSON 값을 보기 좋게 표시. 자연어로 저장된 기대 판정(JSON 문자열)은 따옴표를 벗겨 그대로 보여준다.
function prettyJson(value: string | null): string {
  if (!value) return '-';
  try {
    const parsed = JSON.parse(value);
    if (typeof parsed === 'string') return parsed; // 자연어 판정 문자열
    return JSON.stringify(parsed, null, 2);
  } catch {
    return value;
  }
}

interface CreateSetFormValues {
  name: string;
  evalType: string;
  version: string;
  targetType: string;
  status?: string;
  description?: string;
  expectedPolicyJson?: string;
}

export default function AiEvaluationsPage() {
  const { rows, page, size, total, loading, error, applyFilters, changePage, reload } =
    usePagedList<EvaluationSet, EvaluationSetListParams>(listEvaluationSets, {
      page: 0,
      size: 20,
    });

  // 케이스 승인/반려 권한(REVIEWER/SUPER_ADMIN). CONTENT_CREATOR는 셋·케이스 생성까지만.
  const { adminInfo } = useAuth();
  const canReview = canAccessAdminRoute(adminInfo?.adminRole, [ADMIN_ROLES.REVIEWER]);

  const [evalType, setEvalType] = useState<string | undefined>(undefined);
  const [status, setStatus] = useState<string | undefined>(undefined);

  // 평가셋 생성 모달
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<CreateSetFormValues>();

  // 활성화/폐기 진행 중 행
  const [busyId, setBusyId] = useState<number | null>(null);

  // 케이스 드로어
  const [selectedSet, setSelectedSet] = useState<EvaluationSet | null>(null);

  const onSearch = () =>
    applyFilters({ evalType: evalType || undefined, status: status || undefined });

  const onReset = () => {
    setEvalType(undefined);
    setStatus(undefined);
    applyFilters({ evalType: undefined, status: undefined });
  };

  const submitCreate = async () => {
    let values: CreateSetFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return; // 폼 검증 실패 → 모달 유지
    }
    setCreating(true);
    try {
      await createEvaluationSet({
        name: values.name.trim(),
        evalType: values.evalType,
        version: values.version.trim(),
        targetType: values.targetType,
        description: values.description?.trim() || undefined,
        status: values.status,
        // 기대 판정은 자연어. 서버는 JsonNode라 문자열 값으로 저장된다(판단값, 원문 아님).
        expectedPolicyJson: values.expectedPolicyJson?.trim() || undefined,
      });
      message.success('평가 세트를 만들었습니다.');
      setCreateOpen(false);
      form.resetFields();
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '생성에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const doActivate = async (row: EvaluationSet) => {
    setBusyId(row.id);
    try {
      await activateEvaluationSet(row.id);
      message.success(`#${row.id} 평가 세트을 활성화했습니다.`);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '활성화에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const doRetire = async (row: EvaluationSet) => {
    setBusyId(row.id);
    try {
      await retireEvaluationSet(row.id);
      message.success(`#${row.id} 평가 세트을 폐기했습니다.`);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '폐기에 실패했습니다.');
    } finally {
      setBusyId(null);
    }
  };

  const columns: ColumnsType<EvaluationSet> = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    { title: '이름', dataIndex: 'name', width: 200, ellipsis: true },
    {
      title: '유형',
      dataIndex: 'evalType',
      width: 130,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    { title: '버전', dataIndex: 'version', width: 120 },
    {
      title: '대상',
      dataIndex: 'targetType',
      width: 130,
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (v: string) => setStatusTag(v),
    },
    {
      title: '생성일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '액션',
      width: 230,
      fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button size="small" onClick={() => setSelectedSet(r)}>
            항목 보기
          </Button>
          {r.status === 'DRAFT' && (
            <Popconfirm
              title="이 평가 세트을 활성화할까요?"
              okText="활성화"
              cancelText="취소"
              onConfirm={() => doActivate(r)}
            >
              <Button size="small" type="primary" loading={busyId === r.id}>
                활성화
              </Button>
            </Popconfirm>
          )}
          {r.status !== 'RETIRED' && (
            <Popconfirm
              title="이 평가 세트을 폐기할까요?"
              okText="폐기"
              okButtonProps={{ danger: true }}
              cancelText="취소"
              onConfirm={() => doRetire(r)}
            >
              <Button size="small" danger loading={busyId === r.id}>
                폐기
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
        <Space align="center" wrap>
          <Tag color="blue">AD-11</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            AI 평가 세트
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          AI Q&A·해설이 제대로 동작하는지 검사하는 평가 세트와 그 안의 평가 항목을
          관리합니다. 권한: REVIEWER / CONTENT_CREATOR / SUPER_ADMIN. (항목 승인·반려는
          REVIEWER / SUPER_ADMIN만 가능합니다. 검증 참조 원문은 다루지 않습니다.)
        </Typography.Paragraph>

        {error && (
          <Alert
            type="error"
            showIcon
            message="평가 세트 목록을 불러오지 못했습니다"
            description={error}
            action={
              <Button size="small" onClick={reload}>
                재시도
              </Button>
            }
          />
        )}

        <Space wrap>
          <Select
            placeholder="유형"
            allowClear
            style={{ width: 200 }}
            value={evalType}
            onChange={(v) => setEvalType(v)}
            options={EVAL_TYPE_OPTIONS}
          />
          <Select
            placeholder="상태"
            allowClear
            style={{ width: 160 }}
            value={status}
            onChange={(v) => setStatus(v)}
            options={SET_STATUS_OPTIONS}
          />
          <Button type="primary" onClick={onSearch}>
            조회
          </Button>
          <Button onClick={onReset}>초기화</Button>
          <Tooltip title="새로고침">
            <Button icon={<ReloadOutlined />} onClick={reload} />
          </Tooltip>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              setCreateOpen(true);
            }}
          >
            평가 세트 만들기
          </Button>
        </Space>

        <Table<EvaluationSet>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (r) => (
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                <Typography.Text type="secondary">
                  설명: {r.description || '-'}
                </Typography.Text>
                <Typography.Text type="secondary">기대 판정</Typography.Text>
                <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {prettyJson(r.expectedPolicyJson)}
                </pre>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  활성 {formatDateTime(r.activatedAt)} · 폐기{' '}
                  {formatDateTime(r.retiredAt)}
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

      {/* 평가 셋 생성 모달 */}
      <Modal
        open={createOpen}
        title="평가 세트 만들기"
        okText="만들기"
        cancelText="취소"
        confirmLoading={creating}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form<CreateSetFormValues>
          form={form}
          layout="vertical"
          initialValues={{ status: 'DRAFT' }}
          preserve={false}
        >
          <Form.Item
            name="name"
            label="이름"
            rules={[{ required: true, message: '이름을 입력하세요.' }]}
          >
            <Input placeholder="예: AI Q&A 정책 회귀 평가" maxLength={200} />
          </Form.Item>
          <Form.Item
            name="evalType"
            label="유형"
            rules={[{ required: true, message: '유형을 선택하세요.' }]}
          >
            <Select placeholder="유형 선택" options={EVAL_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="version"
            label="버전"
            rules={[{ required: true, message: '버전을 입력하세요.' }]}
            extra="예: 2026.06.1 같은 식별 가능한 버전 문자열"
          >
            <Input placeholder="버전 문자열" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="targetType"
            label="대상 유형"
            rules={[{ required: true, message: '대상 유형을 선택하세요.' }]}
          >
            <Select placeholder="대상 유형 선택" options={TARGET_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="status" label="초기 상태">
            <Select options={SET_STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="description" label="설명 (선택)">
            <Input.TextArea rows={2} maxLength={500} placeholder="평가 세트 설명" />
          </Form.Item>
          <Form.Item
            name="expectedPolicyJson"
            label="기대 판정 (선택)"
            extra="자연어로 적습니다. 예: 가치 판단 질문은 차단되어야 함"
          >
            <Input.TextArea
              rows={3}
              placeholder="예: 가치 판단·신앙 평가 질문은 차단(BLOCKED)되어야 함"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 케이스 관리 드로어 */}
      <Drawer
        open={selectedSet !== null}
        title={
          selectedSet
            ? `평가 항목 — #${selectedSet.id} ${selectedSet.name}`
            : '평가 항목'
        }
        width={760}
        onClose={() => setSelectedSet(null)}
        destroyOnHidden
      >
        {selectedSet && (
          <EvaluationCasesPanel setId={selectedSet.id} canReview={canReview} />
        )}
      </Drawer>
    </Card>
  );
}

// ----- 케이스 패널 (드로어 내부, 셋별 독립 목록) -----

// 수동 추가 폼 — 식별자·기대판정만(자유 텍스트 없음). inputJson은 서버가 메타로 조립한다.
interface CreateCaseFormValues {
  targetType: string;
  targetId: number;
  expectedPolicyJson?: string;
}

function EvaluationCasesPanel({
  setId,
  canReview,
}: {
  setId: number;
  canReview: boolean;
}) {
  // setId 가 바뀔 때만 새 fetcher → usePagedList 무한 호출 방지(안정적 identity).
  const fetcher = useMemo(
    () => (params: EvaluationCaseListParams) => listEvaluationCases(setId, params),
    [setId],
  );
  const { rows, page, size, total, loading, error, applyFilters, changePage, reload } =
    usePagedList<EvaluationCase, EvaluationCaseListParams>(fetcher, {
      page: 0,
      size: 20,
    });

  const [status, setStatus] = useState<string | undefined>(undefined);

  // 수동 추가 모달(식별자·기대판정만)
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<CreateCaseFormValues>();

  // 승인/반려 모달
  const [action, setAction] = useState<{
    mode: 'approve' | 'reject';
    target: EvaluationCase;
  } | null>(null);
  const [reviewReason, setReviewReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSearch = () => applyFilters({ status: status || undefined });
  const onReset = () => {
    setStatus(undefined);
    applyFilters({ status: undefined });
  };

  const submitCreate = async () => {
    let values: CreateCaseFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setCreating(true);
    try {
      // 자유 텍스트 없이 식별자·기대판정(자연어)만. sourceType은 수동=ADMIN_CREATED.
      await createEvaluationCase(setId, {
        targetType: values.targetType,
        targetId: values.targetId,
        sourceType: 'ADMIN_CREATED',
        expectedPolicyJson: values.expectedPolicyJson?.trim() || undefined,
      });
      message.success('평가 항목을 추가했습니다.');
      setCreateOpen(false);
      form.resetFields();
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '생성에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const submitAction = async () => {
    if (!action) return;
    const reason = reviewReason.trim();
    if (!reason) {
      message.error('검토 사유를 입력하세요.');
      return;
    }
    setSubmitting(true);
    try {
      if (action.mode === 'approve') {
        await approveEvaluationCase(action.target.id, reason);
        message.success('승인했습니다.');
      } else {
        await rejectEvaluationCase(action.target.id, reason);
        message.success('반려했습니다.');
      }
      setAction(null);
      reload();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const openAction = (mode: 'approve' | 'reject', target: EvaluationCase) => {
    setAction({ mode, target });
    setReviewReason('');
  };

  const columns: ColumnsType<EvaluationCase> = [
    { title: 'ID', dataIndex: 'id', width: 64 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (v: string) => caseStatusTag(v),
    },
    {
      title: '대상',
      width: 150,
      render: (_, r) => (
        <span>
          <Tag color="blue">{r.targetType}</Tag>
          {r.targetId != null ? `#${r.targetId}` : ''}
        </span>
      ),
    },
    {
      title: '출처',
      width: 170,
      render: (_, r) => (
        <span>
          <Tag>{r.sourceType}</Tag>
          {r.sourceId != null ? `#${r.sourceId}` : ''}
        </span>
      ),
    },
    {
      title: '생성일',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '액션',
      width: 150,
      fixed: 'right',
      render: (_, r) => {
        if (!canReview) {
          return <Typography.Text type="secondary">-</Typography.Text>;
        }
        if (r.status !== 'CANDIDATE') {
          return <Typography.Text type="secondary">-</Typography.Text>;
        }
        return (
          <Space>
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
          </Space>
        );
      },
    },
  ];

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      {error && (
        <Alert
          type="error"
          showIcon
          message="평가 항목 목록을 불러오지 못했습니다"
          description={error}
          action={
            <Button size="small" onClick={reload}>
              재시도
            </Button>
          }
        />
      )}

      <Space wrap>
        <Select
          placeholder="상태"
          allowClear
          style={{ width: 160 }}
          value={status}
          onChange={(v) => setStatus(v)}
          options={CASE_STATUS_OPTIONS}
        />
        <Button type="primary" onClick={onSearch}>
          조회
        </Button>
        <Button onClick={onReset}>초기화</Button>
        <Tooltip title="새로고침">
          <Button icon={<ReloadOutlined />} onClick={reload} />
        </Tooltip>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            form.resetFields();
            setCreateOpen(true);
          }}
        >
          평가 항목 추가
        </Button>
      </Space>

      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        수동 추가는 대상 식별자(유형·ID)만 입력합니다. 원문/프롬프트는 저장하지 않습니다.
        ‘AI 산출물 검증’·‘신고 처리’ 화면에서도 평가 항목으로 등록할 수 있습니다.
      </Typography.Text>

      <Table<EvaluationCase>
        rowKey="id"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 'max-content' }}
        expandable={{
          expandedRowRender: (r) => (
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Typography.Text type="secondary">입력(식별자·메타)</Typography.Text>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                {prettyJson(r.inputJson)}
              </pre>
              <Typography.Text type="secondary">기대 판정</Typography.Text>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                {prettyJson(r.expectedPolicyJson)}
              </pre>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                검토 {formatDateTime(r.reviewedAt)}
                {r.reviewedByAdminId != null
                  ? ` · 검토자 #${r.reviewedByAdminId}`
                  : ''}
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

      {/* 평가 항목 추가 모달 — 식별자·기대판정만(자유 텍스트 없음) */}
      <Modal
        open={createOpen}
        title="평가 항목 추가"
        okText="추가"
        cancelText="취소"
        confirmLoading={creating}
        onOk={submitCreate}
        onCancel={() => setCreateOpen(false)}
        destroyOnHidden
      >
        <Form<CreateCaseFormValues>
          form={form}
          layout="vertical"
          preserve={false}
        >
          <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
            대상 식별자만 저장합니다(원문/프롬프트 미저장). 출처는 ADMIN_CREATED로
            기록됩니다.
          </Typography.Paragraph>
          <Form.Item
            name="targetType"
            label="대상 유형"
            rules={[{ required: true, message: '대상 유형을 선택하세요.' }]}
          >
            <Select placeholder="대상 유형 선택" options={TARGET_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item
            name="targetId"
            label="대상 ID"
            rules={[{ required: true, message: '대상 ID를 입력하세요.' }]}
            extra="평가 대상의 식별자 (예: QA 요청 ID 700, 성경 절 ID 1001)"
          >
            <InputNumber style={{ width: '100%' }} min={1} placeholder="예: 700" />
          </Form.Item>
          <Form.Item
            name="expectedPolicyJson"
            label="기대 판정 (선택)"
            extra="자연어로 적습니다. 예: 이 답변은 차단되어야 함"
          >
            <Input.TextArea
              rows={2}
              placeholder="예: 가치 판단 질문이므로 차단(BLOCKED)되어야 함"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 승인/반려 모달 */}
      <Modal
        open={action !== null}
        title={action?.mode === 'approve' ? '평가 항목 승인' : '평가 항목 반려'}
        okText={action?.mode === 'approve' ? '승인' : '반려'}
        okButtonProps={{ danger: action?.mode === 'reject' }}
        cancelText="취소"
        confirmLoading={submitting}
        onOk={submitAction}
        onCancel={() => setAction(null)}
        destroyOnHidden
      >
        {action && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              항목 #{action.target.id} · {action.target.targetType}
              {action.target.targetId != null ? ` #${action.target.targetId}` : ''}
            </Typography.Text>
            <div>
              <Typography.Text>
                검토 사유 <Typography.Text type="danger">*</Typography.Text>
              </Typography.Text>
              <Input.TextArea
                rows={3}
                value={reviewReason}
                onChange={(e) => setReviewReason(e.target.value)}
                placeholder="검토 사유를 입력하세요(필수)."
                style={{ marginTop: 4 }}
              />
            </div>
          </Space>
        )}
      </Modal>
    </Space>
  );
}
