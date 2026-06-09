// .env 파일의 값을 읽어오는 곳.
// import.meta.env 는 Vite가 제공하는 환경변수 모음이고,
// 'VITE_' 로 시작하는 변수만 프론트엔드에서 읽을 수 있다.
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

// 개발 모드 여부 (vite dev 서버에서 true, prod 빌드에서 false).
export const IS_DEV: boolean = import.meta.env.DEV;

// [DEV 전용] 백엔드 dev-bypass에 보낼 관리자 회원 id (X-Dev-User-Id).
// 백엔드 qtai.dev.admin-member-id 와 같아야 한다. 기본 1(dev 회원 시드 관례).
export const DEV_ADMIN_MEMBER_ID: string =
  import.meta.env.VITE_DEV_ADMIN_MEMBER_ID ?? '1';
