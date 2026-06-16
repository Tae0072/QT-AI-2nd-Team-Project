import { ApiClientError, apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-11 AI 평가셋/평가케이스 관리 =====
// 연결 API (base /api/v1/admin/ai, AdminAiEvaluationController)
//   GET  /evaluation-sets                          평가셋 목록 (권한: REVIEWER/CONTENT_CREATOR)
//   POST /evaluation-sets                          평가셋 생성
//   POST /evaluation-sets/{setId}/activate|retire  평가셋 상태 전이
//   GET  /evaluation-sets/{setId}/cases            평가케이스 목록
//   POST /evaluation-sets/{setId}/cases            평가케이스 생성
//   POST /evaluation-cases/{caseId}/approve|reject 케이스 승인/반려 (권한: REVIEWER, reviewReason 필수)
//   POST /evaluation-sets/{setId}/runs             프롬프트 평가 실행
//   GET  /evaluation-sets/{setId}/runs/latest      최근 평가 실행
//   GET  /evaluation-runs/{runId}                  평가 실행 상세
//   POST /assets/{assetId}/evaluation-candidates   asset → 평가 케이스 후보 등록
// AI Q&A·해설 회귀 평가의 기준 메타데이터를 다룬다(04 §7.3). JSON 필드는 응답에서 문자열로 온다.

export type EvalType = 'EXPLANATION' | 'SIMULATOR' | 'QA';
export type EvalSetStatus = 'DRAFT' | 'ACTIVE' | 'RETIRED';
export type EvalCaseStatus = 'CANDIDATE' | 'APPROVED' | 'REJECTED';
export type EvaluationRunStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED';
export type EvaluationRunResultStatus = 'PASSED' | 'FAILED' | 'NEEDS_REVIEW';

// 백엔드 AiEvaluationSetResponse 와 1:1
export interface EvaluationSet {
  id: number;
  name: string;
  evalType: string; // EXPLANATION / SIMULATOR / QA
  version: string;
  targetType: string; // BIBLE_VERSE / QT_PASSAGE / QA_REQUEST
  expectedPolicyJson: string | null;
  description: string | null;
  status: string; // DRAFT / ACTIVE / RETIRED
  createdAt: string;
  activatedAt: string | null;
  retiredAt: string | null;
}

export interface EvaluationSetListParams extends PageParams {
  evalType?: string;
  targetType?: string;
  status?: string;
}

// 백엔드 AiEvaluationSetRequest (expectedPolicyJson 은 JSON 값으로 전송)
export interface CreateEvaluationSetPayload {
  name: string;
  evalType: string;
  version: string;
  targetType: string;
  expectedPolicyJson?: unknown;
  description?: string;
  status?: string;
}

// 백엔드 AiEvaluationCaseResponse 와 1:1
export interface EvaluationCase {
  id: number;
  evaluationSetId: number;
  targetType: string;
  targetId: number | null;
  sourceType: string; // VALIDATION_FAILURE / USER_REPORT / ADMIN_CREATED
  sourceId: number | null;
  inputJson: string | null;
  expectedOutputJson: string | null;
  expectedPolicyJson: string | null;
  status: string; // CANDIDATE / APPROVED / REJECTED
  reviewedByAdminId: number | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface EvaluationCaseListParams extends PageParams {
  status?: string;
}

// 백엔드 AiEvaluationCaseRequest — 식별자·기대판정만. 자유 텍스트(원문/프롬프트)는 보내지 않는다.
// inputJson은 서버가 {targetType,targetId,sourceType} 메타로 조립한다(§7).
export interface CreateEvaluationCasePayload {
  targetType: string;
  targetId: number;
  sourceType: string;
  expectedPolicyJson?: unknown;
  status?: string;
}

// 백엔드 AiEvaluationCaseStatusResponse
export interface EvaluationCaseStatus {
  id: number;
  status: string;
}

export interface EvaluationRunResult {
  id: number;
  evaluationCaseId: number;
  result: EvaluationRunResultStatus;
  reason: string | null;
  outputSummaryJson: string | null;
  createdAt: string;
}

export interface EvaluationRun {
  id: number;
  evaluationSetId: number;
  promptVersionId: number;
  status: EvaluationRunStatus;
  totalCount: number;
  passedCount: number;
  failedCount: number;
  needsReviewCount: number;
  startedAt: string | null;
  finishedAt: string | null;
  requestedByAdminId: number | null;
  results: EvaluationRunResult[];
}

export interface CreateEvaluationRunPayload {
  promptVersionId: number;
}

// ----- 평가 셋 -----
export function listEvaluationSets(params: EvaluationSetListParams = {}) {
  return unwrap<Page<EvaluationSet>>(
    apiClient.get<ApiResponse<Page<EvaluationSet>>>('/admin/ai/evaluation-sets', {
      params,
    }),
  );
}

export function createEvaluationSet(payload: CreateEvaluationSetPayload) {
  return unwrap<EvaluationSet>(
    apiClient.post<ApiResponse<EvaluationSet>>('/admin/ai/evaluation-sets', payload),
  );
}

export function activateEvaluationSet(setId: number) {
  return unwrap<EvaluationSet>(
    apiClient.post<ApiResponse<EvaluationSet>>(
      `/admin/ai/evaluation-sets/${setId}/activate`,
    ),
  );
}

export function retireEvaluationSet(setId: number) {
  return unwrap<EvaluationSet>(
    apiClient.post<ApiResponse<EvaluationSet>>(
      `/admin/ai/evaluation-sets/${setId}/retire`,
    ),
  );
}

// ----- 평가 케이스 -----
export function listEvaluationCases(
  setId: number,
  params: EvaluationCaseListParams = {},
) {
  return unwrap<Page<EvaluationCase>>(
    apiClient.get<ApiResponse<Page<EvaluationCase>>>(
      `/admin/ai/evaluation-sets/${setId}/cases`,
      { params },
    ),
  );
}

export function createEvaluationCase(
  setId: number,
  payload: CreateEvaluationCasePayload,
) {
  return unwrap<EvaluationCase>(
    apiClient.post<ApiResponse<EvaluationCase>>(
      `/admin/ai/evaluation-sets/${setId}/cases`,
      payload,
    ),
  );
}

export function approveEvaluationCase(caseId: number, reviewReason: string) {
  return unwrap<EvaluationCaseStatus>(
    apiClient.post<ApiResponse<EvaluationCaseStatus>>(
      `/admin/ai/evaluation-cases/${caseId}/approve`,
      { reviewReason },
    ),
  );
}

export function rejectEvaluationCase(caseId: number, reviewReason: string) {
  return unwrap<EvaluationCaseStatus>(
    apiClient.post<ApiResponse<EvaluationCaseStatus>>(
      `/admin/ai/evaluation-cases/${caseId}/reject`,
      { reviewReason },
    ),
  );
}

// ----- 평가 실행 -----
export function createEvaluationRun(
  setId: number,
  payload: CreateEvaluationRunPayload,
) {
  return unwrap<EvaluationRun>(
    apiClient.post<ApiResponse<EvaluationRun>>(
      `/admin/ai/evaluation-sets/${setId}/runs`,
      payload,
    ),
  );
}

export async function getLatestEvaluationRun(
  setId: number,
): Promise<EvaluationRun | null> {
  try {
    const res = await apiClient.get<ApiResponse<EvaluationRun>>(
      `/admin/ai/evaluation-sets/${setId}/runs/latest`,
    );
    if (!res.data.success) {
      throw new Error(
        res.data.error?.message ?? '최근 평가 실행을 불러오지 못했습니다.',
      );
    }
    return res.data.data;
  } catch (e) {
    if (e instanceof ApiClientError && e.status === 404) {
      return null;
    }
    throw e;
  }
}

export function getEvaluationRun(runId: number) {
  return unwrap<EvaluationRun>(
    apiClient.get<ApiResponse<EvaluationRun>>(
      `/admin/ai/evaluation-runs/${runId}`,
    ),
  );
}

// ----- asset → 평가 후보 등록 (이 모듈이 소유) -----
// 팀원의 AI Asset 상세 화면(AiAssetsPage)이 추후 이 함수를 import 해서 버튼에 연결한다.
// (평가 후보 등록을 두 곳에서 중복 구현하지 않기 위한 단일 소유 지점.)
export interface CreateEvaluationCandidatePayload {
  evaluationSetId: number;
  expectedPolicyJson?: unknown;
}

export function createEvaluationCandidate(
  assetId: number,
  payload: CreateEvaluationCandidatePayload,
) {
  return unwrap<EvaluationCase>(
    apiClient.post<ApiResponse<EvaluationCase>>(
      `/admin/ai/assets/${assetId}/evaluation-candidates`,
      payload,
    ),
  );
}

// ----- 신고 → 평가 항목 후보 등록 (USER_REPORT) -----
// FE는 판단값(평가 세트·기대 정책)만 보내고, 백엔드가 신고 메타데이터로 inputJson을 조립한다
// (원문/프롬프트/민감정보 미저장). AI 신고(AI_QA_REQUEST/AI_ASSET)만 등록 가능.
export function createReportEvaluationCandidate(
  reportId: number,
  payload: CreateEvaluationCandidatePayload,
) {
  return unwrap<EvaluationCase>(
    apiClient.post<ApiResponse<EvaluationCase>>(
      `/admin/ai/reports/${reportId}/evaluation-candidates`,
      payload,
    ),
  );
}
