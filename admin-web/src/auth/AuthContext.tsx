import { createContext, useState, useCallback, type ReactNode } from 'react';
import { getToken, setToken as saveToken, clearToken } from './tokenStorage';

// ===== 로그인 상태(토큰)를 앱 전체에서 공유하는 Context =====
// React 의 Context 는 "여러 화면이 함께 쓰는 값"을 전달하는 통로다.
//  - token      : 현재 저장된 ADMIN 토큰 (null 이면 비로그인)
//  - isLoggedIn : 로그인 여부 (편의 값)
//  - login(t)   : 토큰을 저장한다 (로그인 처리)
//  - logout()   : 토큰을 지운다 (로그아웃)
export interface AuthContextValue {
  token: string | null;
  isLoggedIn: boolean;
  login: (token: string) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
);

// 이 Provider 로 앱을 감싸면 내부 어디서든 useAuth() 로 로그인 상태를 쓸 수 있다.
export function AuthProvider({ children }: { children: ReactNode }) {
  // 새로고침해도 유지되도록, 처음 값은 저장소에서 읽어온다.
  const [token, setTokenState] = useState<string | null>(() => getToken());

  const login = useCallback((newToken: string) => {
    saveToken(newToken);
    setTokenState(newToken);
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setTokenState(null);
  }, []);

  const value: AuthContextValue = {
    token,
    isLoggedIn: token !== null && token.length > 0,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
