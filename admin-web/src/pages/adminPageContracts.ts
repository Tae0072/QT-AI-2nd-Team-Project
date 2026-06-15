import type { EvaluationSetListParams } from '../api/aiEvaluations';
import type { AiPromptStatus, AiPromptVersionListParams } from '../api/aiPromptVersions';
import type { MusicTrackStatus } from '../api/musicTracks';
import type {
  CreatePraiseSongRequest,
  PraiseSongStatus,
  UpdatePraiseSongRequest,
} from '../api/praiseSongs';
import type { ProcessReportPayload, Report } from '../api/reports';
import type { QtPassageStatus } from '../api/qtPassages';

export const AI_ASSET_FILTERABLE_STATUSES = [
  'VALIDATING',
  'APPROVED',
  'REJECTED',
  'HIDDEN',
] as const;

export type AiAssetFilterableStatus = (typeof AI_ASSET_FILTERABLE_STATUSES)[number];

export const AI_ASSET_DEFAULT_STATUS: AiAssetFilterableStatus = 'VALIDATING';

export type RegenerationJobNotice = {
  generationJobId?: number;
  status: string;
};

type GenerationJobLike =
  | {
      id?: number;
      generationJobId?: number;
      status: string;
    }
  | null
  | undefined;

export function isAiAssetReviewable(status: string) {
  return status === 'VALIDATING';
}

export function isAiAssetApprovable(
  status: string,
  autoValidationResult: string | null | undefined,
  advisorValidationResult: string | null | undefined,
) {
  return (
    isAiAssetReviewable(status) &&
    autoValidationResult === 'PASSED' &&
    advisorValidationResult === 'PASSED'
  );
}

export function shouldShowAiAssetApproveButton(
  status: string,
  advisorValidationResult: string | null | undefined,
) {
  return isAiAssetReviewable(status) && advisorValidationResult !== 'REJECTED';
}

export function isAiAssetRegeneratable(status: string) {
  return status === 'REJECTED' || status === 'HIDDEN';
}

export function isActiveGenerationJobStatus(status: string) {
  return status === 'QUEUED' || status === 'RUNNING';
}

function toRegenerationJobNotice(job: GenerationJobLike): RegenerationJobNotice | undefined {
  if (!job || !isActiveGenerationJobStatus(job.status)) {
    return undefined;
  }
  return {
    generationJobId: job.generationJobId ?? job.id,
    status: job.status,
  };
}

export function resolveActiveRegenerationJob(
  cachedJob: RegenerationJobNotice | undefined,
  activeGenerationJob: GenerationJobLike,
  generationJob: GenerationJobLike,
) {
  return (
    cachedJob ??
    toRegenerationJobNotice(activeGenerationJob) ??
    toRegenerationJobNotice(generationJob)
  );
}

export function aiAssetEvaluationSetListParams(
  targetType: string | null,
): EvaluationSetListParams {
  return {
    targetType: targetType ?? undefined,
    size: 100,
  };
}

export const AI_EVALUATION_RUN_STATUS_TAGS = {
  RUNNING: { color: 'processing', text: '실행 중' },
  SUCCEEDED: { color: 'green', text: '성공' },
  FAILED: { color: 'red', text: '실패' },
} as const;

export const AI_EVALUATION_RUN_RESULT_TAGS = {
  PASSED: { color: 'green', text: '통과' },
  FAILED: { color: 'red', text: '실패' },
  NEEDS_REVIEW: { color: 'gold', text: '검토 필요' },
} as const;

export const AI_PROMPT_MANAGED_TYPE = 'EXPLANATION' as const;
export const AI_PROMPT_DEFAULT_STATUS = 'DRAFT' as const;

export const AI_PROMPT_VERSION_STATUS_TAGS = {
  DRAFT: { color: 'gold', text: '초안' },
  ACTIVE: { color: 'green', text: '활성' },
  RETIRED: { color: 'default', text: '폐기' },
} as const;

export function aiPromptVersionListParams(
  status: AiPromptStatus | undefined,
): Pick<AiPromptVersionListParams, 'promptType' | 'status'> {
  return {
    promptType: AI_PROMPT_MANAGED_TYPE,
    status,
  };
}

export function aiDraftPromptOptionsParams(): AiPromptVersionListParams {
  return {
    promptType: AI_PROMPT_MANAGED_TYPE,
    status: AI_PROMPT_DEFAULT_STATUS,
    page: 0,
    size: 100,
  };
}

export function canRunAiEvaluation(adminRole: string | null | undefined) {
  return adminRole === 'REVIEWER' || adminRole === 'SUPER_ADMIN';
}

export const PRAISE_SONG_FILTERABLE_STATUSES: PraiseSongStatus[] = ['ACTIVE', 'HIDDEN'];

export function buildPraiseSongCreatePayload(
  values: CreatePraiseSongRequest,
): CreatePraiseSongRequest {
  return {
    title: values.title,
    artist: values.artist,
    licenseNote: values.licenseNote,
    status: values.status,
  };
}

export function buildPraiseSongUpdatePayload(
  values: UpdatePraiseSongRequest,
): UpdatePraiseSongRequest {
  return {
    title: values.title,
    artist: values.artist,
    licenseNote: values.licenseNote,
    status: values.status,
  };
}

export const MUSIC_TRACK_FILTERABLE_STATUSES: MusicTrackStatus[] = ['ACTIVE', 'HIDDEN'];

export function musicTrackActionsForStatus(status: MusicTrackStatus) {
  return {
    canPublish: status === 'HIDDEN',
    canHide: status === 'ACTIVE',
  };
}

export const QT_PASSAGE_FILTERABLE_STATUSES: QtPassageStatus[] = [
  'pending_review',
  'active',
  'hidden',
];

export function qtPassageActionsForStatus(status: QtPassageStatus) {
  return {
    canEdit: ['pending_review', 'active', 'hidden'].includes(status),
    canPublish: ['pending_review', 'hidden'].includes(status),
    canHide: status === 'active',
  };
}

export type ReportActionMode = 'resolve' | 'reject';

export function isOpenReportStatus(status: string) {
  return status === 'RECEIVED' || status === 'REVIEWING';
}

export function isAiReport(targetType: string) {
  return targetType === 'AI_QA_REQUEST' || targetType === 'AI_ASSET';
}

export function buildReportProcessPayload(
  mode: ReportActionMode,
  report: Pick<Report, 'targetType'>,
  reason: string,
  notifyReporter: boolean,
): ProcessReportPayload {
  const payload: ProcessReportPayload = {
    reason: reason.trim() || undefined,
    notifyReporter,
  };
  if (mode === 'resolve' && report.targetType === 'POST') {
    payload.action = 'HIDE_TARGET';
  }
  return payload;
}

export function reportEvaluationSetListParams(
  report: Pick<Report, 'targetType'>,
): EvaluationSetListParams {
  return report.targetType === 'AI_QA_REQUEST'
    ? { targetType: 'QA_REQUEST', size: 100 }
    : { size: 100 };
}
