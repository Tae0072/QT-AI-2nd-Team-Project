import { KAKAO_JS_KEY } from '../config/env';

// ===== 카카오 JS SDK 헬퍼 =====
// 카카오 SDK 는 index.html 에서 CDN(window.Kakao)으로 로드된다(2026-06-10 결정 ①: JS SDK 방식, 서버 /oauth2 미사용).
// 전체 SDK 타입 패키지는 도입하지 않고, 우리가 쓰는 부분만 최소 타입으로 선언한다.

interface KakaoAuth {
  login(opts: {
    success: (res: { access_token: string }) => void;
    fail: (err: unknown) => void;
  }): void;
}

interface KakaoSDK {
  isInitialized(): boolean;
  init(jsKey: string): void;
  Auth: KakaoAuth;
}

declare global {
  interface Window {
    Kakao?: KakaoSDK;
  }
}

// 로드·키 확인 후 초기화된 Kakao SDK 를 돌려준다.
function getKakao(): KakaoSDK {
  const Kakao = window.Kakao;
  if (!Kakao) {
    throw new Error('카카오 SDK가 로드되지 않았습니다. 네트워크 연결을 확인하세요.');
  }
  if (!KAKAO_JS_KEY) {
    throw new Error('VITE_KAKAO_JS_KEY 가 설정되지 않았습니다. (.env 확인)');
  }
  if (!Kakao.isInitialized()) {
    Kakao.init(KAKAO_JS_KEY);
  }
  return Kakao;
}

// 카카오 로그인 → 카카오 access token 획득.
// 이 토큰을 서버(POST /api/v1/admin/auth/kakao)로 전달해 ADMIN 토큰을 발급받는다.
export function loginWithKakao(): Promise<string> {
  const Kakao = getKakao();
  return new Promise<string>((resolve, reject) => {
    Kakao.Auth.login({
      success: (res) => resolve(res.access_token),
      fail: (err) => reject(err),
    });
  });
}
