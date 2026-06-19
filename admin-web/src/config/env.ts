// .env 파일의 값을 읽어오는 곳.
// import.meta.env 는 Vite가 제공하는 환경변수 모음이고,
// 'VITE_' 로 시작하는 변수만 프론트엔드에서 읽을 수 있다.
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

// ⚠️ 시연(데모) 전용 — 운영/일반 dev/build 에서는 꺼져 있어야 한다.
// `npm run dev:mock`(vite --mode mock) 으로 켜진다. 두 경로로 판별한다:
//   - MODE === 'mock'           : --mode mock 실행 시 자동(권장, env 파일 불필요)
//   - VITE_ADMIN_MOCK === '1'   : .env.mock 등에서 명시적으로 켤 때(보조)
// 켜지면 client.ts 가 모든 API 호출을 인메모리 목업으로 대체한다(백엔드 불필요).
// 일반 `vite`/`vite build` 는 MODE 가 development/production 이라 항상 꺼진다.
export const USE_ADMIN_MOCK: boolean =
  import.meta.env.MODE === 'mock' || import.meta.env.VITE_ADMIN_MOCK === '1';
