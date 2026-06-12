import type { EvaluationSetListParams } from '../api/aiEvaluations';
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

export function isAiAssetReviewable(status: string) {
  return status === 'VALIDATING';
}

export function isAiAssetRegeneratable(status: string) {
  return status === 'REJECTED' || status === 'HIDDEN';
}

export function aiAssetEvaluationSetListParams(
  targetType: string | null,
): EvaluationSetListParams {
  return {
    targetType: targetType ?? undefined,
    size: 100,
  };
}

export const PRAISE_SONG_FILTERABLE_STATUSES: PraiseSongStatus[] = ['ACTIVE', 'HIDDEN'];

export function buildPraiseSongCreatePayload(
  values: CreatePraiseSongRequest,
): CreatePraiseSongRequest {
  return {
    title: values.title,
    artist: values.artist,
    licenseNote: values.licenseNote,
  };
}

export function buildPraiseSongUpdatePayload(
  values: UpdatePraiseSongRequest,
): UpdatePraiseSongRequest {
  return {
    title: values.title,
    artist: values.artist,
    licenseNote: values.licenseNote,
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
  // AdminReportController accepts action, and current moderation hiding is wired
  // only for resolved POST reports.
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
