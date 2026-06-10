import { useCallback, useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Space,
  Alert,
  Button,
  Tooltip,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined } from '@ant-design/icons';
import { listQtPassages, type QtPassage } from '../api/qtPassages';

// ===== AD-02 오늘 QT 관리 =====
// GET /api/v1/admin/qt-passages — 목록. 백엔드 미구현 시 '준비 중' 안내(자동 활성화).
// 컬럼은 응답 필드로 자동 구성(DTO 확정 전까지 generic).

export default function QtPassagesPage() {
  const [rows, setRows] = useState<QtPassage[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [ready, setReady] = useState(false);

  const load = useCallback((p: number, s: number) => {
    setLoading(true);
    listQtPassages({ page: p, size: s })
      .then((res) => {
        setRows(res.content);
        setTotal(res.totalElements);
        setPage(res.page);
        setSize(res.size);
        setReady(true);
      })
      .catch(() => setReady(false))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(0, 20);
  }, [load]);

  const columns: ColumnsType<QtPassage> =
    rows.length > 0
      ? Object.keys(rows[0]).map((k) => ({
          title: k,
          dataIndex: k,
          render: (v: unknown) =>
            v == null ? '-' : typeof v === 'object' ? JSON.stringify(v) : String(v),
        }))
      : [{ title: 'ID', dataIndex: 'id', width: 80 }];

  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">AD-02</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            오늘 QT 관리
          </Typography.Title>
          <Tooltip title="새로고침">
            <Button
              size="small"
              icon={<ReloadOutlined />}
              onClick={() => load(page, size)}
            />
          </Tooltip>
        </Space>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          QT 본문을 등록·수정·게시·숨김 처리합니다. (공개 00:00 KST / 사용자 노출
          04:00 KST)
        </Typography.Paragraph>

        {!ready && !loading && (
          <Alert
            type="info"
            showIcon
            message="백엔드 준비 중 (E단계)"
            description="오늘 QT 관리 API(GET /api/v1/admin/qt-passages)가 아직 구현되지 않았습니다. 백엔드가 준비되면 목록·게시/숨김이 자동 활성화됩니다."
          />
        )}

        <Table<QtPassage>
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
            showTotal: (t) => `총 ${t}건`,
            onChange: (p, ps) => load(p - 1, ps),
          }}
        />
      </Space>
    </Card>
  );
}
