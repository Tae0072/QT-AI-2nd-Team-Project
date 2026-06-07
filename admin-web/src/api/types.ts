// 백엔드 공통 응답 형식(envelope)에 대응하는 타입 모음.
// 기준: 04_API_명세서.md §1.4(성공) / §1.5(에러) / §1.6(페이징)
// 모든 응답은 아래 ApiResponse<T> 모양으로 온다.

// 성공/실패 공통 응답 봉투
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
  timestamp: string;
  traceId: string;
}

// 에러 상세 (success=false 일 때 error 에 들어온다)
export interface ApiError {
  code: string; // 예: VALIDATION_ERROR, FORBIDDEN, TOKEN_EXPIRED
  message: string; // 화면에 보여줄 사용자용 메시지
  fields?: Record<string, string>; // 입력값 오류 시 필드별 메시지
}

// 페이지네이션 응답 (data 안에 들어오는 모양, §1.6)
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 목록 조회 시 공통으로 쓰는 쿼리 파라미터
export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
