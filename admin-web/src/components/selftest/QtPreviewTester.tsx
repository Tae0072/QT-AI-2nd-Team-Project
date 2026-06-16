import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ExperimentOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import {
  listQtPassages,
  createQtPassage,
  publishQtPassage,
  hideQtPassage,
  type QtPassage,
  type QtPassageStatus,
  type QtPassageRequest,
} from '../../api/qtPassages';
import { ApiClientError } from '../../api/client';
import { formatDateTime } from '../../utils/datetime';
import {
  buildSampleQtPassageRequest,
  isTestQtPassageTitle,
  isValidQtDate,
  qtDateRangeParams,
  qtPassageActionsForStatus,
  todayQtDate,
} from '../../pages/adminPageContracts';

// ===== AD-18 기능 테스트: 오늘 QT 미리보기 · 테스트 등록 =====
// "특정 날짜를 지정해 오늘의 QT를 미리 만들어 보고, 그 날짜에 무엇이 등록돼 있는지
//  관리자 등록 상태로 확인"하는 패널.
//
// - 조회(GET): 부작용 없음. from=to=선택 날짜로 그 하루만 가볍게 불러온다.
// - 테스트 등록/게시/숨김(POST): 실제 DB에 쓰는 작업이라 반드시 확인(Popconfirm)을 거친다.
//   제목에 [테스트] 접두사가 붙어 운영 데이터와 구분되고 나중에 숨김으로 정리할 수 있다.
//
// 표시 수준은 "관리자 등록 상태"(본문/제목/상태/게시 시각)다. (CLAUDE.md §6: 공개 00:00 KST,
// 사용자 노출·캐시 갱신 04:00 KST)

const STATUS_META: Record<QtPassageStatus, { label: string; color: string }> = {
  pending_review: { label: '검토 대기', color: 'gold' },
  active: { label: '게시됨', color: 'green' },
  hidden: { label: '숨김', color: 'default' },
  deletion_notified: { label: '삭제 예정', color: 'orange' },
  removed: { label: '제거됨', color: 'red' },
};

function errMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiClientError) return e.code ? `[${e.code}] ${e.message}` : e.message;
  return e instanceof Error ? e.message : fallback;
}

export default function QtPreviewTester() {
  const [date, setDate] = useState<string>(todayQtDate());
  const [rows, setRows] = useState<QtPassage[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [mutating, setMutating] = useState(false);

  // 테스트 등록 모달
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<QtPassageRequest>();

  const dateValid = isValidQtDate(date);

  // 선택한 날짜 하루만 조회한다(읽기 전용).
  const loadDate = async () => {
    if (!dateValid) {
      message.warning('YYYY-MM-DD 형식의 올바른 날짜를 입력해 주세요');
      return;
    }
    setLoading(true);
    try {
      const page = await listQtPassages(qtDateRangeParams(date));
      setRows(page.content);
    } catch (e) {
      message.error(errMessage(e, '조회에 실패했습니다'));
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  // 테스트 등록 모달을 연다(현재 선택 날짜로 샘플 값 채움).
  const openCreate = () => {
    if (!dateValid) {
      message.warning('먼저 올바른 날짜를 입력해 주세요');
      return;
    }
    form.setFieldsValue(buildSampleQtPassageRequest(date));
    setOpen(true);
  };

  const onSubmit = async () => {
    let values: QtPassageRequest;
    try {
      values = await form.validateFields();
    } catch {
      return; // 검증 실패 — 모달 유지
    }
    setSaving(true);
    try {
      await createQtPassage(values);
      message.success('테스트 QT가 등록되었습니다');
      setOpen(false);
      // 등록한 날짜로 맞춰 다시 조회해 결과를 바로 보여준다.
      setDate(values.qtDate);
      const page = await listQtPassages(qtDateRangeParams(values.qtDate));
      setRows(page.content);
    } catch (e) {
      message.error(errMessage(e, '테스트 등록에 실패했습니다'));
    } finally {
      setSaving(false);
    }
  };

  const runAction = async (fn: () => Promise<unknown>, okMsg: string) => {
    setMutating(true);
    try {
      await fn();
      message.success(okMsg);
      await loadDate();
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
      width: 180,
      render: (_, r) => r.mainVerseRef ?? `#${r.bookId} ${r.chapter}:${r.startVerse}-${r.endVerse}`,
    },
    {
      title: '제목',
      dataIndex: 'title',
      ellipsis: true,
      render: (t: string) =>
        isTestQtPassageTitle(t) ? (
          <Space size={4}>
            <Tag color="purple">테스트</Tag>
            <span>{t}</span>
          </Space>
        ) : (
          t
        ),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
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
      width: 170,
      render: (_, r) => {
        const { canPublish, canHide } = qtPassageActionsForStatus(r.status);
        if (!canPublish && !canHide) return '-';
        return (
          <Space size={4}>
            {canPublish && (
              <Popconfirm
                title="이 QT를 게시할까요? (실제 게시 처리됩니다)"
                onConfirm={() => runAction(() => publishQtPassage(r.id), '게시되었습니다')}
              >
                <Button size="small" type="primary" disabled={mutating}>
                  게시
                </Button>
              </Popconfirm>
            )}
            {canHide && (
              <Popconfirm
                title="이 QT를 숨길까요? (테스트 데이터 정리에 사용하세요)"
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
        <Space align="center">
          <Tag color="geekblue">AD-02 연동</Tag>
          <Typography.Title level={4} style={{ margin: 0 }}>
            오늘 QT 미리보기 · 테스트 등록
          </Typography.Title>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          날짜를 지정해 그날의 QT를 미리 만들어 보고, 등록 상태(본문·제목·상태·게시 시각)를 확인합니다.
          조회는 부작용이 없지만, <b>테스트 등록·게시·숨김은 실제 DB에 반영</b>되므로 확인 후 실행됩니다.
          등록된 테스트 데이터는 제목 앞 <Typography.Text code>[테스트]</Typography.Text> 표시로 구분되며,
          정리할 때는 <b>숨김</b>으로 사용자 노출에서 제외하세요. (공개 00:00 KST / 사용자 노출 04:00 KST)
        </Typography.Paragraph>

        <Space wrap align="start">
          <div>
            <Input
              addonBefore="QT 날짜"
              placeholder="2026-06-15"
              style={{ width: 240 }}
              value={date}
              status={date && !dateValid ? 'error' : undefined}
              onChange={(e) => setDate(e.target.value.trim())}
              onPressEnter={() => void loadDate()}
              allowClear
            />
          </div>
          <Button icon={<SearchOutlined />} loading={loading} onClick={() => void loadDate()}>
            이 날짜 조회
          </Button>
          <Tooltip title="오늘 날짜로">
            <Button icon={<ReloadOutlined />} onClick={() => setDate(todayQtDate())} />
          </Tooltip>
          <Popconfirm
            title="테스트 QT를 등록할까요?"
            description="실제 DB에 [테스트] QT가 생성됩니다. 정리는 숨김으로 할 수 있어요."
            okText="등록 진행"
            cancelText="취소"
            onConfirm={openCreate}
          >
            <Button type="primary" icon={<PlusOutlined />} disabled={!dateValid}>
              테스트 QT 등록
            </Button>
          </Popconfirm>
        </Space>

        {date && !dateValid && (
          <Alert
            type="warning"
            showIcon
            message="날짜 형식이 올바르지 않습니다 (YYYY-MM-DD, 실재하는 날짜)"
          />
        )}

        {rows !== null && (
          <Alert
            type={rows.length > 0 ? 'success' : 'info'}
            showIcon
            icon={<ExperimentOutlined />}
            message={
              rows.length > 0
                ? `${date}에 등록된 QT ${rows.length}건`
                : `${date}에 등록된 QT가 없습니다. '테스트 QT 등록'으로 미리 만들어 볼 수 있어요.`
            }
          />
        )}

        {rows !== null && (
          <Table<QtPassage>
            rowKey="id"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={rows}
            pagination={false}
            scroll={{ x: 'max-content' }}
          />
        )}
      </Space>

      <Modal
        title="테스트 QT 등록"
        open={open}
        onOk={() => void onSubmit()}
        onCancel={() => setOpen(false)}
        confirmLoading={saving}
        okText="등록"
        cancelText="취소"
        forceRender
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="실제 DB에 등록됩니다"
          description="아래 샘플 값으로 [테스트] QT가 생성됩니다. 필요하면 수정 후 등록하세요."
        />
        <Form form={form} layout="vertical">
          <Form.Item
            label="QT 날짜"
            name="qtDate"
            rules={[
              { required: true, message: 'QT 날짜를 입력해 주세요' },
              { pattern: /^\d{4}-\d{2}-\d{2}$/, message: 'YYYY-MM-DD 형식으로 입력해 주세요' },
            ]}
          >
            <Input placeholder="2026-06-15" />
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
            <Form.Item label="시작 절" name="startVerse" rules={[{ required: true, message: '시작 절' }]}>
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
            <Input placeholder="[테스트] 오늘 QT 미리보기" maxLength={200} />
          </Form.Item>
          <Form.Item label="대표 구절(선택)" name="mainVerseRef" rules={[{ max: 100 }]}>
            <Input placeholder="예: 시편 23:1-6" maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
