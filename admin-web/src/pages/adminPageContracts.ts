import type { EvaluationSetListParams } from '../api/aiEvaluations';
import type { AiPromptStatus, AiPromptVersionListParams } from '../api/aiPromptVersions';
import type { MusicTrackStatus } from '../api/musicTracks';
import type { ProcessReportPayload, Report } from '../api/reports';
import type { QtPassageStatus, QtPassageRequest } from '../api/qtPassages';

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

// ===== AD-18 기능 테스트: 오늘 QT 미리보기·테스트 등록 헬퍼 =====
// SelfTestPage의 'QT 미리보기' 패널이 쓰는 순수 함수.
// (이 파일은 node 테스트 러너가 직접 변환해 읽으므로, 런타임 모듈 로드 구문 없이 타입 전용만 둔다.)

export const QT_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

// 테스트 등록임을 식별하기 위한 제목 접두사. 나중에 숨김/정리할 때 기준이 된다.
export const QT_TEST_TITLE_PREFIX = '[테스트]';

// 로컬 기준 오늘 날짜를 YYYY-MM-DD로 만든다. now를 주입하면 테스트가 쉽다.
// 참고: QT 공개 기준 시각은 KST(00:00). 관리자 브라우저 로컬 날짜를 기본값으로 쓰되
// 사용자가 임의 날짜를 직접 고를 수 있다.
export function todayQtDate(now: Date = new Date()): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())}`;
}

// YYYY-MM-DD 형식 + 실재하는 날짜(2026-02-30 같은 값 차단)인지 검사한다.
export function isValidQtDate(value: string): boolean {
  if (!QT_DATE_PATTERN.test(value)) return false;
  const d = new Date(`${value}T00:00:00`);
  if (Number.isNaN(d.getTime())) return false;
  // Date 롤오버(2026-02-30 → 2026-03-02)를 다시 포맷해 원본과 비교한다.
  return todayQtDate(d) === value;
}

// 특정 날짜 '하루'만 조회하는 목록 파라미터(from=to=해당 날짜).
export function qtDateRangeParams(qtDate: string) {
  return { from: qtDate, to: qtDate, page: 0, size: 50 };
}

// 테스트용 샘플 등록 요청. 본문 텍스트는 저장하지 않고 참조(권/장/절)만 둔다.
// 제목에 [테스트] 접두사를 붙여 운영 데이터와 구분하고 사후 정리를 쉽게 한다.
export function buildSampleQtPassageRequest(qtDate: string): QtPassageRequest {
  return {
    qtDate,
    bookId: 19, // 시편(Psalms)
    chapter: 23,
    startVerse: 1,
    endVerse: 6,
    title: `${QT_TEST_TITLE_PREFIX} 오늘 QT 미리보기 (${qtDate})`,
    mainVerseRef: '시편 23:1-6',
  };
}

export function isTestQtPassageTitle(title: string): boolean {
  return title.startsWith(QT_TEST_TITLE_PREFIX);
}
