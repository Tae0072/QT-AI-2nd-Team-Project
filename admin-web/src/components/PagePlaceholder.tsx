import { Card, Typography, Tag, Space } from 'antd';

// ===== 화면 골격용 공통 컴포넌트 =====
// 아직 내용이 없는 화면을, 화면코드·설명·필요권한·연결 예정 API 와 함께 보여 준다.
// 다음 단계에서 각 화면을 실제 표/폼으로 구현할 때 이 컴포넌트를 교체하면 된다.
interface Props {
  code: string; // 화면 코드 (예: AD-01)
  title: string; // 화면 이름
  description: string; // 한 줄 설명
  endpoints: string[]; // 연결 예정 API 목록
  roles?: string; // 필요 권한 (선택)
}

export default function PagePlaceholder({
  code,
  title,
  description,
  endpoints,
  roles,
}: Props) {
  return (
    <Card>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space align="center">
          <Tag color="blue">{code}</Tag>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
        </Space>

        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          {description}
        </Typography.Paragraph>

        {roles && (
          <Typography.Text>
            필요 권한: <Tag>{roles}</Tag>
          </Typography.Text>
        )}

        <div>
          <Typography.Text strong>연결 예정 API</Typography.Text>
          <ul style={{ marginTop: 8 }}>
            {endpoints.map((e) => (
              <li key={e}>
                <Typography.Text code>{e}</Typography.Text>
              </li>
            ))}
          </ul>
        </div>

        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          이 화면은 아직 골격(빈 페이지)입니다. 다음 단계에서 위 API를 연결해
          표·폼을 구현합니다.
        </Typography.Paragraph>
      </Space>
    </Card>
  );
}
