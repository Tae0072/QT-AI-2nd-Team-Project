import { useContext } from 'react';
import { AuthContext, type AuthContextValue } from './AuthContext';

// AuthContext 를 편하게 꺼내 쓰는 훅.
// 반드시 <AuthProvider> 안쪽 컴포넌트에서만 호출해야 한다.
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth 는 AuthProvider 안에서만 사용할 수 있습니다.');
  }
  return ctx;
}
