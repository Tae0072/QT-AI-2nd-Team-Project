// .env 파일의 값을 읽어오는 곳.
// import.meta.env 는 Vite가 제공하는 환경변수 모음이고,
// 'VITE_' 로 시작하는 변수만 프론트엔드에서 읽을 수 있다.
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
