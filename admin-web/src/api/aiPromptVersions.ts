import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AI 프롬프트 버전 관리 =====
// 연결 API (권한: REVIEWER / SUPER_ADMIN)
//   GET  /api/v1/admin/ai/prompt-versions
//   GET  /api/v1/admin/ai/prompt-versions/{id}
//   POST /api/v1/admin/ai/prompt-versions
//   POST /api/v1/admin/ai/prompt-versions/{id}/activate
//   POST /api/v1/admin/ai/prompt-versions/{id}/retire
// 1차 범위는 EXPLANATION 프롬프트만 지원한다.

export type AiPromptType = 'EXPLANATION';
export type AiPromptStatus = 'DRAFT' | 'ACTIVE' | 'RETIRED';

export interface AiPromptVersion {
  id: number;
  promptType: string;
  version: string;
  contentHash: string | null;
  status: string;
  systemPrompt: string;
  userPromptTemplate: string;
  modelName: string | null;
  temperature: number;
  maxTokens: number;
  description: string | null;
  createdByAdminId: number | null;
  createdAt: string;
  activatedAt: string | null;
  retiredAt: string | null;
}

export interface AiPromptVersionListParams extends PageParams {
  promptType?: AiPromptType;
  status?: AiPromptStatus;
}

export interface CreateAiPromptVersionPayload {
  promptType: AiPromptType;
  version: string;
  systemPrompt: string;
  userPromptTemplate: string;
  modelName?: string;
  temperature: number;
  maxTokens: number;
  description?: string;
}

export function listAiPromptVersions(params: AiPromptVersionListParams = {}) {
  return unwrap<Page<AiPromptVersion>>(
    apiClient.get<ApiResponse<Page<AiPromptVersion>>>(
      '/admin/ai/prompt-versions',
      { params },
    ),
  );
}

export function getAiPromptVersion(id: number) {
  return unwrap<AiPromptVersion>(
    apiClient.get<ApiResponse<AiPromptVersion>>(
      `/admin/ai/prompt-versions/${id}`,
    ),
  );
}

export function createAiPromptVersion(payload: CreateAiPromptVersionPayload) {
  return unwrap<AiPromptVersion>(
    apiClient.post<ApiResponse<AiPromptVersion>>(
      '/admin/ai/prompt-versions',
      payload,
    ),
  );
}

export function activateAiPromptVersion(id: number) {
  return unwrap<AiPromptVersion>(
    apiClient.post<ApiResponse<AiPromptVersion>>(
      `/admin/ai/prompt-versions/${id}/activate`,
    ),
  );
}

export function retireAiPromptVersion(id: number) {
  return unwrap<AiPromptVersion>(
    apiClient.post<ApiResponse<AiPromptVersion>>(
      `/admin/ai/prompt-versions/${id}/retire`,
    ),
  );
}
