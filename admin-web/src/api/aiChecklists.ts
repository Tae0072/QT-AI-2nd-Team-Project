import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-09 AI 검증 체크리스트 관리 =====
// 연결 API (권한: REVIEWER / SUPER_ADMIN, AdminAiAuthentication.requireReviewer)
//   GET  /api/v1/admin/ai/validation-checklists            목록
//   POST /api/v1/admin/ai/validation-checklists            등록 (기본 status=DRAFT)
//   POST /api/v1/admin/ai/validation-checklists/{id}/activate 활성화 (운영 적용본)
//   POST /api/v1/admin/ai/validation-checklists/{id}/retire   폐기
// 참고: 검증 체크리스트 '버전'은 AI 산출물 검증·승인 게이트의 기준이 된다(CLAUDE.md §7).
//       원문(체크 항목 본문)이 아니라 버전/해시/상태 같은 메타데이터만 다룬다.

// 체크리스트 유형 (백엔드 AiValidationChecklistType)
export type ChecklistType = 'EXPLANATION' | 'SIMULATOR' | 'QA';
// 체크리스트 상태 (백엔드 AiValidationChecklistStatus)
export type ChecklistStatus = 'DRAFT' | 'ACTIVE' | 'RETIRED';

// 백엔드 AdminAiValidationChecklistResponse 와 1:1 대응
export interface AiChecklist {
  id: number;
  checklistType: string; // EXPLANATION / SIMULATOR / QA
  version: string;
  contentHash: string | null;
  status: string; // DRAFT / ACTIVE / RETIRED
  createdByAdminId: number | null;
  createdAt: string; // ISO
  activatedAt: string | null; // ISO
  retiredAt: string | null; // ISO
}

export interface AiChecklistListParams extends PageParams {
  checklistType?: string;
  status?: string;
}

// 등록 요청 바디 (백엔드 AdminAiValidationChecklistRequest)
// status 를 비우면 서버가 DRAFT 로 처리한다.
export interface CreateChecklistRequest {
  checklistType: ChecklistType;
  version: string;
  contentHash?: string;
  status?: ChecklistStatus;
}

export function listAiChecklists(params: AiChecklistListParams = {}) {
  return unwrap<Page<AiChecklist>>(
    apiClient.get<ApiResponse<Page<AiChecklist>>>(
      '/admin/ai/validation-checklists',
      { params },
    ),
  );
}

export function createAiChecklist(payload: CreateChecklistRequest) {
  return unwrap<AiChecklist>(
    apiClient.post<ApiResponse<AiChecklist>>(
      '/admin/ai/validation-checklists',
      payload,
    ),
  );
}

export function activateAiChecklist(id: number) {
  return unwrap<AiChecklist>(
    apiClient.post<ApiResponse<AiChecklist>>(
      `/admin/ai/validation-checklists/${id}/activate`,
    ),
  );
}

export function retireAiChecklist(id: number) {
  return unwrap<AiChecklist>(
    apiClient.post<ApiResponse<AiChecklist>>(
      `/admin/ai/validation-checklists/${id}/retire`,
    ),
  );
}
