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
import { listNotices, type Notice } from '../api/notices';

// ===== AD-06 시스템 공지 =====
// GET /api/v1/admin/notices — 목록. 백엔드 미구현 시 '준비 중' 안내(자동 활성화).
// 컬럼은 응답 필드로 자동 구성(DTO 확정 전까지 generic).

export default function NoticesPage() {
  const [rows, setRows] = useState<Notice[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [ready, setReady] = useState(false);

  const load = useCallback((p: number, s: number) => {
    setLoading(true);
    listNotices({ page: p, size: s })
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

  const columns: ColumnsType<Notice> =
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
          <Tag color="blue">AD-06</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            시스템 공지
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
          앱 공지사항을 등록·수정·발행·숨김 처리합니다.
        </Typography.Paragraph>

        {!ready && !loading && (
          <Alert
            type="info"
            showIcon
            message="백엔드 준비 중 (E단계)"
            description="시스템 공지 API(GET /api/v1/admin/notices)가 아직 구현되지 않았습니다. 백엔드가 준비되면 목록·발행/숨김이 자동 활성화됩니다."
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
