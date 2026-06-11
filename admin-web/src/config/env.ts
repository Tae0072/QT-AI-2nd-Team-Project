// .env 파일의 값을 읽어오는 곳.
// import.meta.env 는 Vite가 제공하는 환경변수 모음이고,
// 'VITE_' 로 시작하는 변수만 프론트엔드에서 읽을 수 있다.
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

// 카카오 JavaScript SDK 키 (로그인 버튼용). .env 의 VITE_KAKAO_JS_KEY.
// 비어 있으면 로그인 시 안내 후 중단한다. (2026-06-10 결정 ①: 카카오 JS SDK 방식)
export const KAKAO_JS_KEY: string = import.meta.env.VITE_KAKAO_JS_KEY ?? '';
