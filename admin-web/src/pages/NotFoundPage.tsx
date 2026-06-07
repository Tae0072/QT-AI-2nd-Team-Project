import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

// 정의되지 않은 주소(URL)로 들어왔을 때 보여 주는 404 화면.
export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Result
        status="404"
        title="404"
        subTitle="요청하신 페이지를 찾을 수 없습니다."
        extra={
          <Button type="primary" onClick={() => navigate('/dashboard')}>
            대시보드로 가기
          </Button>
        }
      />
    </div>
  );
}
