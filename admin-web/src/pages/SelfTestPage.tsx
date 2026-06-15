import { useState } from 'react';
import {
  Button,
  Card,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  Alert,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import { rejectReport, resolveReport, seedTestReport } from '../api/reports';
import QtPreviewTester from '../components/selftest/QtPreviewTester';

// ===== AD-18 기능 테스트 (자가 진단) =====
// 관리자 웹이 호출하는 각 백엔드 admin API에 "읽기 전용(GET)"으로 핑을 보내,
// 연결·인증·권한가드·직렬화가 정상인지 한눈에 확인한다.
// 쓰기(등록/수정/삭제/발송)는 부작용이 있어 자동 실행하지 않는다(조회만).

type CheckStatus = 'idle' | 'running' | 'pass' | 'guard' | 'fail';

interface CheckDef {
  key: string;
  code: string;
  label: string;
  url: string;
  params?: Record<string, unknown>;
}

interface CheckResult {
  status: CheckStatus;
  httpStatus?: number;
  ms?: number;
  message?: string;
}

// 점검 대상(전부 GET, 읽기 전용). 목록형은 page/size=1로 가볍게 호출한다.
const CHECKS: CheckDef[] = [
  { key: 'me', code: '인증', label: '관리자 인증 (/admin/me)', url: '/admin/me' },
  { key: 'dashboard', code: 'AD-01', label: '대시보드', url: '/admin/dashboard' },
  { key: 'qt', code: 'AD-02', label: '오늘 QT 관리', url: '/admin/qt-passages', params: { page: 0, size: 1 } },
  { key: 'aiAssets', code: 'AD-03', label: 'AI 산출물 검증', url: '/admin/ai/assets', params: { page: 0, size: 1 } },
  { key: 'reports', code: 'AD-04', label: '신고 처리', url: '/admin/reports', params: { page: 0, size: 1 } },
  { key: 'notices', code: 'AD-06', label: '시스템 공지', url: '/admin/notices', params: { page: 0, size: 1 } },
  { key: 'audit', code: 'AD-07', label: '감사 로그', url: '/admin/audit-logs', params: { page: 0, size: 1 } },
  { key: 'aiMon', code: 'AD-08', label: 'AI 운영 모니터링', url: '/admin/ai/monitoring' },
  { key: 'aiChk', code: 'AD-09', label: 'AI 검증 체크리스트', url: '/admin/ai/validation-checklists' },
  { key: 'aiBatch', code: 'AD-10', label: 'AI 배치 실행 로그', url: '/admin/ai/batch-run-logs', params: { page: 0, size: 1 } },
  { key: 'aiEval', code: 'AD-11', label: 'AI 평가 세트', url: '/admin/ai/evaluation-sets', params: { page: 0, size: 1 } },
  { key: 'members', code: 'AD-12', label: '회원 관리', url: '/admin/members', params: { page: 0, size: 1 } },
  { key: 'sharing', code: 'AD-13', label: '나눔 공유글 관리', url: '/admin/sharing-posts', params: { page: 0, size: 1 } },
  { key: 'music', code: 'AD-15', label: '배경음악 관리', url: '/admin/music' },
  { key: 'missions', code: 'AD-16', label: '미션 관리', url: '/admin/missions' },
  { key: 'bible', code: 'AD-17', label: '성경 데이터 조회', url: '/admin/bible/books' },
];

function statusTag(s: CheckStatus) {
  switch (s) {
    case 'pass':
      return <Tag color="green">성공</Tag>;
    case 'guard':
      return <Tag color="gold">권한 가드(정상)</Tag>;
    case 'fail':
      return <Tag color="red">실패</Tag>;
    case 'running':
      return <Tag color="blue">실행 중…</Tag>;
    default:
      return <Tag>대기</Tag>;
  }
}

export default function SelfTestPage() {
  const navigate = useNavigate();
  const [results, setResults] = useState<Record<string, CheckResult>>({});
  const [runningAll, setRunningAll] = useState(false);

  // ── 신고 처리 테스트(쓰기) — 테스트 신고를 만들고 처리/반려까지 시연 ──
  const [seeded, setSeeded] = useState<{ id: number; status: string } | null>(null);
  const [reportBusy, setReportBusy] = useState(false);

  const createTestReport = async () => {
    setReportBusy(true);
    try {
      const r = await seedTestReport();
      setSeeded({ id: r.id, status: r.status });
      message.success(`테스트 신고 #${r.id}을(를) 생성했습니다 (상태: ${r.status}).`);
    } catch (e) {
      const err = e as { message?: string };
      message.error(err.message ?? '테스트 신고 생성에 실패했습니다.');
    } finally {
      setReportBusy(false);
    }
  };

  const processSeeded = async (mode: 'resolve' | 'reject') => {
    if (!seeded) return;
    setReportBusy(true);
    try {
      // notifyReporter=false: 테스트라 신고자(=관리자 본인)에게 실제 알림을 보내지 않는다.
      const payload = { reason: '기능 테스트', notifyReporter: false };
      const res =
        mode === 'resolve'
          ? await resolveReport(seeded.id, payload)
          : await rejectReport(seeded.id, payload);
      setSeeded({ id: res.reportId, status: res.status });
      message.success(mode === 'resolve' ? '신고를 처리(인정)했습니다.' : '신고를 반려했습니다.');
    } catch (e) {
      const err = e as { message?: string };
      message.error(err.message ?? '처리에 실패했습니다.');
    } finally {
      setReportBusy(false);
    }
  };

  const runOne = async (check: CheckDef): Promise<CheckResult> => {
    setResults((prev) => ({ ...prev, [check.key]: { status: 'running' } }));
    const started = performance.now();
    try {
      const res = await apiClient.get(check.url, { params: check.params });
      const ms = Math.round(performance.now() - started);
      const result: CheckResult = { status: 'pass', httpStatus: res.status, ms };
      setResults((prev) => ({ ...prev, [check.key]: result }));
      return result;
    } catch (e) {
      const ms = Math.round(performance.now() - started);
      const err = e as { status?: number; code?: string; message?: string };
      const isGuard = err.status === 403;
      const result: CheckResult = {
        status: isGuard ? 'guard' : 'fail',
        httpStatus: err.status,
        ms,
        message: isGuard
          ? `이 계정 권한으로는 접근 불가(가드 정상): ${err.code ?? ''}`
          : `${err.code ?? ''} ${err.message ?? '요청 실패'}`.trim(),
      };
      setResults((prev) => ({ ...prev, [check.key]: result }));
      return result;
    }
  };

  const runAll = async () => {
    setRunningAll(true);
    setResults({});
    let pass = 0;
    let guard = 0;
    let fail = 0;
    for (const check of CHECKS) {
      // eslint-disable-next-line no-await-in-loop
      const r = await runOne(check);
      if (r.status === 'pass') pass++;
      else if (r.status === 'guard') guard++;
      else fail++;
    }
    setRunningAll(false);
    if (fail === 0) {
      message.success(`전체 점검 완료 — 성공 ${pass}, 권한가드 ${guard}, 실패 0`);
    } else {
      message.error(`전체 점검 완료 — 성공 ${pass}, 권한가드 ${guard}, 실패 ${fail}`);
    }
  };

  const summary = (() => {
    const vals = Object.values(results);
    return {
      pass: vals.filter((v) => v.status === 'pass').length,
      guard: vals.filter((v) => v.status === 'guard').length,
      fail: vals.filter((v) => v.status === 'fail').length,
      done: vals.filter((v) => v.status !== 'running').length,
    };
  })();

  const columns: ColumnsType<CheckDef> = [
    {
      title: '화면',
      dataIndex: 'code',
      width: 90,
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    { title: '기능', dataIndex: 'label' },
    {
      title: '엔드포인트',
      dataIndex: 'url',
      render: (v: string) => <Typography.Text code>GET {v}</Typography.Text>,
    },
    {
      title: '결과',
      key: 'status',
      width: 130,
      render: (_: unknown, r: CheckDef) => statusTag(results[r.key]?.status ?? 'idle'),
    },
    {
      title: 'HTTP',
      key: 'http',
      width: 80,
      render: (_: unknown, r: CheckDef) => results[r.key]?.httpStatus ?? '-',
    },
    {
      title: '응답(ms)',
      key: 'ms',
      width: 90,
      render: (_: unknown, r: CheckDef) => results[r.key]?.ms ?? '-',
    },
    {
      title: '메시지',
      key: 'message',
      render: (_: unknown, r: CheckDef) => {
        const m = results[r.key]?.message;
        return m ? <Typography.Text type="secondary">{m}</Typography.Text> : '-';
      },
    },
    {
      title: '개별 실행',
      key: 'run',
      width: 100,
      render: (_: unknown, r: CheckDef) => (
        <Tooltip title="이 항목만 실행">
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            loading={results[r.key]?.status === 'running'}
            onClick={() => void runOne(r)}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-18</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            기능 테스트 (자가 진단)
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          관리자 웹이 호출하는 각 백엔드 API에 읽기 전용(GET)으로 핑을 보내 연결·인증·권한·직렬화가
          정상인지 확인합니다. 등록·수정·삭제·발송 같은 쓰기 작업은 자동 실행하지 않습니다.
          권한 가드(정상)는 현재 계정 권한으로 접근 불가라는 뜻으로 장애가 아닙니다.
        </Typography.Paragraph>

        {summary.done > 0 && (
          <Alert
            type={summary.fail > 0 ? 'error' : 'success'}
            showIcon
            message={`점검 ${summary.done}/${CHECKS.length} — 성공 ${summary.pass} · 권한가드 ${summary.guard} · 실패 ${summary.fail}`}
          />
        )}

        <Space wrap>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            loading={runningAll}
            onClick={() => void runAll()}
          >
            전체 점검 실행
          </Button>
          <Tooltip title="결과 비우기">
            <Button icon={<ReloadOutlined />} disabled={runningAll} onClick={() => setResults({})} />
          </Tooltip>
        </Space>

        <Table<CheckDef>
          rowKey="key"
          size="middle"
          columns={columns}
          dataSource={CHECKS}
          pagination={false}
          scroll={{ x: 'max-content' }}
        />
      </Space>
      </Card>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space align="center">
            <Tag color="blue">AD-04</Tag>
            <Typography.Title level={4} style={{ margin: 0 }}>
              신고 처리 테스트 (쓰기)
            </Typography.Title>
          </Space>
          <Alert
            type="warning"
            showIcon
            message="이 테스트는 실제(테스트용) 신고 데이터를 만듭니다."
            description="대상은 COMMENT, 사유는 TEST이며 매번 고유 대상 ID로 생성됩니다(중복 방지). 처리/반려 시 신고자 알림은 보내지 않습니다. 신고 목록이 비어 있을 때 처리 흐름을 점검하는 용도입니다."
          />

          <Space wrap>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              loading={reportBusy}
              onClick={() => void createTestReport()}
            >
              ① 테스트 신고 생성
            </Button>
            <Button
              icon={<CheckCircleOutlined />}
              disabled={!seeded || seeded.status !== 'RECEIVED'}
              loading={reportBusy}
              onClick={() => void processSeeded('resolve')}
            >
              ② 처리(인정)
            </Button>
            <Button
              danger
              icon={<CloseCircleOutlined />}
              disabled={!seeded || seeded.status !== 'RECEIVED'}
              loading={reportBusy}
              onClick={() => void processSeeded('reject')}
            >
              ② 반려
            </Button>
            <Tooltip title="신고 처리 화면으로 이동">
              <Button icon={<RightOutlined />} onClick={() => navigate('/reports')}>
                신고 처리 화면에서 보기
              </Button>
            </Tooltip>
          </Space>

          {seeded && (
            <Alert
              type={seeded.status === 'RECEIVED' ? 'info' : 'success'}
              showIcon
              message={
                <span>
                  테스트 신고 <Typography.Text strong>#{seeded.id}</Typography.Text> · 현재 상태{' '}
                  <Tag
                    color={
                      seeded.status === 'RESOLVED'
                        ? 'green'
                        : seeded.status === 'REJECTED'
                          ? 'red'
                          : 'blue'
                    }
                  >
                    {seeded.status}
                  </Tag>
                  {seeded.status === 'RECEIVED'
                    ? ' — ② 처리/반려를 눌러 상태 전이를 확인하세요.'
                    : ' — 처리 완료. 신고 처리 화면에서도 확인할 수 있습니다.'}
                </span>
              }
            />
          )}
        </Space>
      </Card>

      {/* AD-02 연동: 날짜 지정 오늘 QT 미리보기 · 테스트 등록 */}
      <QtPreviewTester />
    </Space>
  );
}
