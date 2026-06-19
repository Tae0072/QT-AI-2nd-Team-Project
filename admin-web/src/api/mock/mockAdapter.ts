/* eslint-disable @typescript-eslint/no-explicit-any */
// ============================================================================
// ⚠️ 시연(데모) 전용 목업 어댑터 — 운영 배포 전 제거/비활성 대상.
//
// 목적: 관리자 웹의 모든 메뉴(AD-01~20)가 **백엔드 없이도** 동작하도록,
//       axios `apiClient` 의 adapter 를 가로채 인메모리 목업 데이터를 봉투 형식
//       ({ success, data, error })으로 돌려준다.
//
// 안전장치(운영에 절대 안 섞이도록):
//   1) `USE_ADMIN_MOCK`(config/env.ts) 가 켜질 때만 client.ts 가 이 어댑터를 단다.
//      플래그는 .env.mock 의 VITE_ADMIN_MOCK=1 에서만 1 → `npm run dev:mock` 전용.
//   2) 일반 dev/build 에서는 플래그가 꺼져 있어 실제 백엔드를 그대로 호출한다.
//   3) DB·서버를 전혀 건드리지 않는다(메모리 배열만 변경).
//
// 동작 방식: 요청 method + URL 을 라우트 표와 매칭 → 핸들러가 data 를 만들고,
//            어댑터가 공통 봉투로 감싸 200 응답으로 반환한다. 액션(승인/숨김/생성)은
//            메모리 배열을 바꿔 화면에 즉시 반영된다.
// ============================================================================
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

// ── 공통 헬퍼 ──────────────────────────────────────────────────────────────
const T = (daysAgo = 0, hoursAgo = 0): string =>
  new Date(Date.now() - daysAgo * 86_400_000 - hoursAgo * 3_600_000).toISOString();

function todayDate(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function ok<T>(data: T) {
  return {
    success: true,
    data,
    error: null,
    timestamp: new Date().toISOString(),
    traceId: `mock-${Math.random().toString(36).slice(2, 10)}`,
  };
}

// 0-based 페이지 봉투(Page / SpringPage 양쪽 모두 만족).
function pageOf<T>(content: T[], params: Record<string, any>) {
  const size = Number(params.size ?? 20) || 20;
  const page = Number(params.page ?? 0) || 0;
  return {
    content,
    page,
    number: page,
    size,
    totalElements: content.length,
    totalPages: Math.max(1, Math.ceil(content.length / size)),
  };
}

// params 의 단순 동치 필터 + q 부분일치(textFields)로 목록을 거른다.
function applyFilters<T extends Record<string, any>>(
  rows: T[],
  params: Record<string, any>,
  textFields: string[] = [],
): T[] {
  const skip = new Set(['page', 'size', 'sort', 'from', 'to', 'q']);
  let out = rows;
  for (const [k, v] of Object.entries(params)) {
    if (skip.has(k) || v === undefined || v === null || v === '') continue;
    out = out.filter((r) => String(r[k]) === String(v));
  }
  const q = (params.q as string | undefined)?.trim();
  if (q && textFields.length) {
    out = out.filter((r) =>
      textFields.some((f) => String(r[f] ?? '').toLowerCase().includes(q.toLowerCase())),
    );
  }
  return out;
}

let seq = 9000;
const nextId = () => ++seq;

// ── 인메모리 시드 데이터 ────────────────────────────────────────────────────
const TODAY = todayDate();

const db = {
  qtPassages: [
    { id: 7, qtDate: TODAY, bookId: 46, chapter: 12, endChapter: 12, startVerse: 1, endVerse: 11, title: '고린도전서 12:1-11', mainVerseRef: '고린도전서 12:1-11', status: 'active', publishedAt: T(0, 6), collectedAt: T(0, 8), hiddenAt: null, createdAt: T(0, 9), updatedAt: T(0, 6) },
    { id: 6, qtDate: '2026-06-15', bookId: 46, chapter: 9, endChapter: 9, startVerse: 1, endVerse: 23, title: '고린도전서 9:1-23', mainVerseRef: '고린도전서 9:1-23', status: 'active', publishedAt: T(4, 6), collectedAt: T(4, 8), hiddenAt: null, createdAt: T(4, 9), updatedAt: T(4, 6) },
    { id: 5, qtDate: '2026-06-14', bookId: 46, chapter: 8, endChapter: 8, startVerse: 1, endVerse: 13, title: '고린도전서 8:1-13', mainVerseRef: '고린도전서 8:1-13', status: 'hidden', publishedAt: null, collectedAt: T(5, 8), hiddenAt: T(2), createdAt: T(5, 9), updatedAt: T(2) },
    { id: 4, qtDate: TODAY, bookId: 19, chapter: 23, endChapter: 23, startVerse: 1, endVerse: 6, title: '시편 23:1-6', mainVerseRef: '시편 23:1-6', status: 'pending_review', publishedAt: null, collectedAt: T(0, 8), hiddenAt: null, createdAt: T(0, 9), updatedAt: T(0, 9) },
  ] as any[],

  members: [
    { id: 1, nickname: '강태오', status: 'ACTIVE', role: 'ADMIN', nicknameChangedAt: T(30), withdrawnAt: null, createdAt: T(120) },
    { id: 2, nickname: '김태혁', status: 'ACTIVE', role: 'USER', nicknameChangedAt: null, withdrawnAt: null, createdAt: T(90) },
    { id: 3, nickname: '은혜로운하루', status: 'ACTIVE', role: 'USER', nicknameChangedAt: T(10), withdrawnAt: null, createdAt: T(60) },
    { id: 4, nickname: '정지된계정', status: 'SUSPENDED', role: 'USER', nicknameChangedAt: null, withdrawnAt: null, createdAt: T(45) },
    { id: 5, nickname: '탈퇴한사용자', status: 'WITHDRAWN', role: 'USER', nicknameChangedAt: null, withdrawnAt: T(7), createdAt: T(200) },
  ] as any[],

  sharingPosts: [
    { id: 101, memberId: 3, nicknameSnapshot: '은혜로운하루', titleSnapshot: '오늘 묵상 나눔', category: 'GRATITUDE', status: 'PUBLISHED', bodyPreview: '오늘 본문을 통해 받은 은혜를 나눕니다...', body: '오늘 본문을 통해 받은 은혜를 나눕니다. 감사한 하루였습니다.', verseLabel: '고린도전서 12:1', qtDate: TODAY, commentsEnabled: true, likeCount: 12, commentCount: 3, hiddenAt: null, sourceNoteUnsharedAt: null, createdAt: T(0, 3) },
    { id: 102, memberId: 2, nicknameSnapshot: '김태혁', titleSnapshot: '기도제목 나눔', category: 'PRAYER', status: 'PUBLISHED', bodyPreview: '함께 기도해주세요...', body: '함께 기도해주세요. 가정의 회복을 위해 기도 부탁드립니다.', verseLabel: null, qtDate: '2026-06-15', commentsEnabled: true, likeCount: 5, commentCount: 1, hiddenAt: null, sourceNoteUnsharedAt: null, createdAt: T(4, 2) },
    { id: 103, memberId: 4, nicknameSnapshot: '정지된계정', titleSnapshot: '신고로 숨김된 글', category: 'FREE', status: 'HIDDEN', bodyPreview: '부적절 내용으로 숨김 처리됨', body: '부적절 내용으로 숨김 처리됨', verseLabel: null, qtDate: null, commentsEnabled: false, likeCount: 0, commentCount: 0, hiddenAt: T(1), sourceNoteUnsharedAt: null, createdAt: T(6) },
  ] as any[],

  notices: [
    { id: 201, title: '6월 업데이트 안내', body: '안녕하세요. QT-AI 6월 업데이트 안내입니다. 배경음악 기능과 QT 영상 시뮬레이터가 추가되었습니다.', bodyPreview: '안녕하세요. QT-AI 6월 업데이트 안내입니다...', status: 'PUBLISHED', publishedAt: T(2), createdAt: T(3), updatedAt: T(2) },
    { id: 202, title: '서버 점검 예정 (초안)', body: '6월 25일 02:00~04:00 정기 점검이 예정되어 있습니다.', bodyPreview: '6월 25일 02:00~04:00 정기 점검...', status: 'DRAFT', publishedAt: null, createdAt: T(1), updatedAt: T(1) },
    { id: 203, title: '개인정보 처리방침 개정 안내', body: '개인정보 처리방침이 6월 20일자로 개정됩니다. 자세한 내용은 앱 내 약관을 확인해주세요.', bodyPreview: '개인정보 처리방침이 6월 20일자로 개정됩니다...', status: 'PUBLISHED', publishedAt: T(5), createdAt: T(6), updatedAt: T(5) },
    { id: 204, title: '이전 공지(숨김)', body: '기간이 지나 숨김 처리된 공지입니다.', bodyPreview: '기간이 지나 숨김 처리된 공지입니다.', status: 'HIDDEN', publishedAt: T(20), createdAt: T(22), updatedAt: T(10) },
  ] as any[],

  missions: [
    { id: 301, code: 'DAILY_QT', title: '매일 묵상 저장', metricType: 'MEDITATION_SAVED_DAYS', periodType: 'DAILY', targetCount: 1, status: 'ACTIVE', createdAt: T(40), updatedAt: T(5) },
    { id: 302, code: 'WEEKLY_NOTE', title: '주간 노트 3회', metricType: 'NOTE_SAVED_COUNT', periodType: 'WEEKLY', targetCount: 3, status: 'ACTIVE', createdAt: T(40), updatedAt: null },
    { id: 303, code: 'STREAK_7', title: '7일 연속 묵상', metricType: 'STREAK_DAYS', periodType: 'MONTHLY', targetCount: 7, status: 'HIDDEN', createdAt: T(30), updatedAt: T(8) },
  ] as any[],

  musicTracks: [
    { id: 401, title: '잔잔한 아침', category: 'BGM', mimeType: 'audio/mpeg', byteSize: 3_200_000, durationSec: 192, sortOrder: 1, licenseNote: '직접 제작(로열티프리)', status: 'ACTIVE', streamUrl: '/api/v1/music/tracks/401/stream', createdAt: T(20), updatedAt: T(5) },
    { id: 402, title: '주 하나님 지으신 모든 세계', category: 'HYMN', mimeType: 'audio/mpeg', byteSize: 4_100_000, durationSec: 240, sortOrder: 2, licenseNote: '공유저작물', status: 'ACTIVE', streamUrl: '/api/v1/music/tracks/402/stream', createdAt: T(20), updatedAt: null },
    { id: 403, title: '숨김된 트랙', category: 'BGM', mimeType: 'audio/mpeg', byteSize: 2_800_000, durationSec: 150, sortOrder: 3, licenseNote: null, status: 'HIDDEN', streamUrl: '/api/v1/music/tracks/403/stream', createdAt: T(15), updatedAt: T(2) },
  ] as any[],

  reports: [
    { id: 501, reporterMemberId: 3, targetType: 'POST', targetId: 103, reason: 'ABUSE', detail: '욕설이 포함되어 있습니다.', status: 'RECEIVED', processedByAdminId: null, processedAt: null, createdAt: T(0, 5) },
    { id: 502, reporterMemberId: 2, targetType: 'COMMENT', targetId: 9001, reason: 'SPAM', detail: '광고성 댓글', status: 'REVIEWING', processedByAdminId: 1, processedAt: null, createdAt: T(1, 2) },
    { id: 503, reporterMemberId: 3, targetType: 'AI_QA_REQUEST', targetId: 7001, reason: 'INAPPROPRIATE', detail: '부적절한 질문', status: 'RESOLVED', processedByAdminId: 1, processedAt: T(2), createdAt: T(3) },
  ] as any[],

  aiAssets: [
    { id: 601, assetType: 'EXPLANATION', targetType: 'QT_PASSAGE', targetId: 7, status: 'VALIDATING', promptVersion: { id: 801, promptType: 'EXPLANATION', version: 'v1.3', status: 'ACTIVE' }, checklistVersionId: 701, latestValidationResult: 'PASSED', autoValidationResult: 'PASSED', advisorValidationResult: 'PASSED', sourceLabelPresent: true, createdAt: T(0, 7) },
    { id: 602, assetType: 'EXPLANATION', targetType: 'BIBLE_VERSE', targetId: 4602, status: 'APPROVED', promptVersion: { id: 801, promptType: 'EXPLANATION', version: 'v1.3', status: 'ACTIVE' }, checklistVersionId: 701, latestValidationResult: 'PASSED', autoValidationResult: 'PASSED', advisorValidationResult: 'PASSED', sourceLabelPresent: true, createdAt: T(1, 4) },
    { id: 603, assetType: 'EXPLANATION', targetType: 'QT_PASSAGE', targetId: 6, status: 'REJECTED', promptVersion: { id: 800, promptType: 'EXPLANATION', version: 'v1.2', status: 'RETIRED' }, checklistVersionId: 700, latestValidationResult: 'FAILED', autoValidationResult: 'FAILED', advisorValidationResult: null, sourceLabelPresent: false, createdAt: T(3, 2) },
  ] as any[],

  auditLogs: [
    { id: 701, adminUserId: 1, actorType: 'ADMIN', actorId: 1, actorLabel: '강태오(SUPER_ADMIN)', actionType: 'QT_PASSAGE_PUBLISH', targetType: 'QT_PASSAGE', targetId: 7, beforeJson: '{"status":"pending_review"}', afterJson: '{"status":"active"}', createdAt: T(0, 6) },
    { id: 702, adminUserId: 1, actorType: 'ADMIN', actorId: 1, actorLabel: '강태오(SUPER_ADMIN)', actionType: 'AI_ASSET_APPROVE', targetType: 'AI_ASSET', targetId: 602, beforeJson: '{"status":"VALIDATING"}', afterJson: '{"status":"APPROVED"}', createdAt: T(1, 4) },
    { id: 703, adminUserId: null, actorType: 'SYSTEM_BATCH', actorId: null, actorLabel: 'SYSTEM_BATCH', actionType: 'AI_EXPLANATION_SEED', targetType: 'QT_PASSAGE', targetId: 7, beforeJson: null, afterJson: '{"createdCount":2}', createdAt: T(0, 23) },
  ] as any[],

  checklists: [
    { id: 701, checklistType: 'EXPLANATION', version: 'v2.1', contentHash: 'a1b2c3', status: 'ACTIVE', createdByAdminId: 1, createdAt: T(15), activatedAt: T(14), retiredAt: null },
    { id: 702, checklistType: 'QA', version: 'v1.0', contentHash: 'd4e5f6', status: 'ACTIVE', createdByAdminId: 1, createdAt: T(20), activatedAt: T(19), retiredAt: null },
    { id: 700, checklistType: 'EXPLANATION', version: 'v2.0', contentHash: 'z9y8x7', status: 'RETIRED', createdByAdminId: 1, createdAt: T(40), activatedAt: T(39), retiredAt: T(14) },
  ] as any[],

  batchLogs: [
    { id: 801, batchName: 'AI_EXPLANATION_SEED', status: 'SUCCEEDED', createdCount: 2, failedCount: 0, processedCount: 2, errorType: null, errorMessage: null, startedAt: T(0, 23), finishedAt: T(0, 23), createdAt: T(0, 23) },
    { id: 802, batchName: 'TODAY_QT_CACHE_REFRESH', status: 'SUCCEEDED', createdCount: 1, failedCount: 0, processedCount: 1, errorType: null, errorMessage: null, startedAt: T(0, 20), finishedAt: T(0, 20), createdAt: T(0, 20) },
    { id: 803, batchName: 'AI_EXPLANATION_SEED', status: 'PARTIAL_FAILED', createdCount: 1, failedCount: 1, processedCount: 2, errorType: 'LLM_TIMEOUT', errorMessage: 'DeepSeek 응답 지연', startedAt: T(1, 23), finishedAt: T(1, 23), createdAt: T(1, 23) },
  ] as any[],

  evalSets: [
    { id: 901, name: '해설 기본 평가셋', evalType: 'EXPLANATION', version: 'v1.0', targetType: 'QT_PASSAGE', expectedPolicyJson: null, description: '해설 산출물 기본 검증 케이스', status: 'ACTIVE', createdAt: T(18), activatedAt: T(17), retiredAt: null },
    { id: 902, name: 'Q&A 차단 평가셋', evalType: 'QA', version: 'v1.0', targetType: 'QA_REQUEST', expectedPolicyJson: null, description: '가치판단/상담성 질문 차단 확인', status: 'DRAFT', createdAt: T(5), activatedAt: null, retiredAt: null },
    { id: 903, name: '시뮬레이터 검증 평가셋', evalType: 'SIMULATOR', version: 'v0.9', targetType: 'QT_PASSAGE', expectedPolicyJson: null, description: '시뮬레이터 클립 적합성 검증', status: 'RETIRED', createdAt: T(35), activatedAt: T(34), retiredAt: T(12) },
  ] as any[],

  evalCases: [
    { id: 9101, evaluationSetId: 901, targetType: 'QT_PASSAGE', targetId: 7, sourceType: 'AI_ASSET', sourceId: 601, inputJson: null, expectedOutputJson: null, expectedPolicyJson: '{"mustIncludeSourceLabel":true}', status: 'APPROVED', reviewedByAdminId: 1, reviewedAt: T(16), createdAt: T(17) },
    { id: 9102, evaluationSetId: 901, targetType: 'QT_PASSAGE', targetId: 6, sourceType: 'AI_ASSET', sourceId: 603, inputJson: null, expectedOutputJson: null, expectedPolicyJson: null, status: 'CANDIDATE', reviewedByAdminId: null, reviewedAt: null, createdAt: T(2) },
    { id: 9103, evaluationSetId: 901, targetType: 'QT_PASSAGE', targetId: 5, sourceType: 'AI_ASSET', sourceId: 602, inputJson: null, expectedOutputJson: null, expectedPolicyJson: null, status: 'APPROVED', reviewedByAdminId: 1, reviewedAt: T(14), createdAt: T(15) },
    { id: 9201, evaluationSetId: 902, targetType: 'QA_REQUEST', targetId: 7001, sourceType: 'AI_QA_REQUEST', sourceId: 7001, inputJson: '{"q":"이 본문은 무슨 뜻인가요?"}', expectedOutputJson: null, expectedPolicyJson: '{"mustBlock":false}', status: 'APPROVED', reviewedByAdminId: 1, reviewedAt: T(4), createdAt: T(5) },
    { id: 9202, evaluationSetId: 902, targetType: 'QA_REQUEST', targetId: 7002, sourceType: 'AI_QA_REQUEST', sourceId: 7002, inputJson: '{"q":"제 고민을 상담해주세요"}', expectedOutputJson: null, expectedPolicyJson: '{"mustBlock":true}', status: 'CANDIDATE', reviewedByAdminId: null, reviewedAt: null, createdAt: T(3) },
  ] as any[],

  promptVersions: [
    { id: 801, promptType: 'EXPLANATION', version: 'v1.3', contentHash: 'p13hash', status: 'ACTIVE', systemPrompt: '당신은 성경 해설 보조자입니다. 가치 판단/설교식 단정을 하지 않습니다.', userPromptTemplate: '본문: {{passage}}\n해설을 생성하세요.', modelName: 'deepseek-chat', temperature: 0.3, maxTokens: 1024, description: '출처 라벨 강제 + 단정 표현 차단 강화', createdByAdminId: 1, createdAt: T(10), activatedAt: T(9), retiredAt: null },
    { id: 800, promptType: 'EXPLANATION', version: 'v1.2', contentHash: 'p12hash', status: 'RETIRED', systemPrompt: '당신은 성경 해설 보조자입니다.', userPromptTemplate: '본문: {{passage}}', modelName: 'deepseek-chat', temperature: 0.4, maxTokens: 1024, description: '초기 버전', createdByAdminId: 1, createdAt: T(30), activatedAt: T(29), retiredAt: T(9) },
    { id: 802, promptType: 'EXPLANATION', version: 'v1.4-draft', contentHash: null, status: 'DRAFT', systemPrompt: '당신은 성경 해설 보조자입니다.', userPromptTemplate: '본문: {{passage}}\n시대 배경을 포함하세요.', modelName: 'deepseek-chat', temperature: 0.3, maxTokens: 1200, description: '시대 배경 강화 초안', createdByAdminId: 1, createdAt: T(1), activatedAt: null, retiredAt: null },
  ] as any[],

  appState: {
    contentVersion: '2026.06.18',
    appVersion: '1.4.0',
    minSupportedVersion: '1.2.0',
    updateMode: 'RECOMMENDED' as string,
    updateMessage: '새 기능(QT 영상·배경음악)이 추가되었습니다. 업데이트를 권장합니다.',
    updatedAt: T(1),
  },

  pendingUpdates: [
    { id: 1001, title: '1.5.0 정기 업데이트', description: '노트 펜 기능 개선 및 버그 수정', targetAppVersion: '1.5.0', updateMode: 'RECOMMENDED', status: 'PENDING', createdAt: T(0, 5), appliedAt: null },
    { id: 1002, title: '1.4.1 긴급 패치', description: '카카오 로그인 안정화', targetAppVersion: '1.4.1', updateMode: 'FORCED', status: 'APPLIED', createdAt: T(3), appliedAt: T(2) },
    { id: 1003, title: '1.6.0 베타', description: 'QT 영상 자막 기능(베타)', targetAppVersion: '1.6.0', updateMode: 'NONE', status: 'PENDING', createdAt: T(0, 2), appliedAt: null },
  ] as any[],

  sourceVideos: [
    { id: 1, bibleBookId: 46, title: '[고린도전서] 시뮬레이터 영상(김태혁)', videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4', durationSec: 1800, status: 'ACTIVE', createdAt: T(11) },
    { id: 2, bibleBookId: 19, title: '[시편] 23편 시뮬레이터 영상', videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/psa-v1/psalms23.mp4', durationSec: 420, status: 'ACTIVE', createdAt: T(8) },
    { id: 3, bibleBookId: 43, title: '[요한복음] 3장 시뮬레이터 영상(준비중)', videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/jhn-v1/john3.mp4', durationSec: 600, status: 'DRAFT', createdAt: T(2) },
  ] as any[],

  qtVideoClips: [
    { id: 2, qtPassageId: 7, title: '고린도전서 12:1-11 시뮬레이터', sourceVideoId: 1, videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4', startTimeSec: 0, endTimeSec: 120, compositionType: 'SINGLE', status: 'APPROVED', approvedAt: T(0, 6) },
    { id: 3, qtPassageId: 6, title: '고린도전서 9:1-23 시뮬레이터', sourceVideoId: 1, videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/1co-v1/corinthians_full.mp4', startTimeSec: 130, endTimeSec: 240, compositionType: 'SINGLE', status: 'APPROVED', approvedAt: T(4) },
    { id: 4, qtPassageId: 4, title: '시편 23:1-6 시뮬레이터', sourceVideoId: 2, videoUrl: 'https://github.com/xogurrh012/qtai-bible-videos/releases/download/psa-v1/psalms23.mp4', startTimeSec: 0, endTimeSec: 90, compositionType: 'SINGLE', status: 'PENDING', approvedAt: null },
  ] as any[],

  simulatorClips: [
    { id: 1, qtPassageId: 7, title: '고린도전서 12:1-11 시뮬레이터', status: 'READY', aiAssetId: 601, approvedAt: T(0, 6) },
    { id: 2, qtPassageId: 6, title: '고린도전서 9:1-23 시뮬레이터', status: 'READY', aiAssetId: 602, approvedAt: T(4) },
    { id: 3, qtPassageId: 5, title: '고린도전서 8:1-13 시뮬레이터', status: 'MISSING', aiAssetId: null, approvedAt: null },
    { id: 4, qtPassageId: 4, title: '시편 23:1-6 시뮬레이터', status: 'FAILED', aiAssetId: 603, approvedAt: null },
  ] as any[],

  bibleBooks: [
    { id: 19, testament: 'OLD', code: 'PSA', koreanName: '시편', englishName: 'Psalms', displayOrder: 19 },
    { id: 46, testament: 'NEW', code: '1CO', koreanName: '고린도전서', englishName: '1 Corinthians', displayOrder: 46 },
    { id: 43, testament: 'NEW', code: 'JHN', koreanName: '요한복음', englishName: 'John', displayOrder: 43 },
  ] as any[],

  segments: [
    { id: 1, bibleVerseId: 4601, startTimeSec: 0, endTimeSec: 12 },
    { id: 2, bibleVerseId: 4602, startTimeSec: 12, endTimeSec: 25 },
  ] as any[],
};

// ── 멤버 상세에서 쓰는 보조 데이터 ──────────────────────────────────────────
function memberDetail(id: number) {
  const m = db.members.find((x) => x.id === id) ?? db.members[0];
  return { ...m, sharingPostCount: 2, reportsFiledCount: 1, reportsReceivedCount: 0 };
}

function memberSubList(sub: string, params: Record<string, any>) {
  switch (sub) {
    case 'notes':
      return pageOf(
        [
          { id: 11, qtPassageId: 7, category: 'MEDITATION', status: 'SAVED', visibility: 'PRIVATE', title: '오늘의 묵상', createdAt: T(0, 2) },
          { id: 12, qtPassageId: 6, category: 'NOTE', status: 'DRAFT', visibility: 'PRIVATE', title: '메모', createdAt: T(4) },
        ],
        params,
      );
    case 'posts':
      return pageOf(
        [
          { id: 101, status: 'PUBLISHED', title: '오늘 묵상 나눔', category: 'GRATITUDE', createdAt: T(0, 3) },
          { id: 105, status: 'PUBLISHED', title: '감사 제목 나눔', category: 'GRATITUDE', createdAt: T(3) },
          { id: 109, status: 'HIDDEN', title: '숨김된 내 글', category: 'FREE', createdAt: T(8) },
        ],
        params,
      );
    case 'comments':
      return pageOf(
        [
          { id: 31, sharingPostId: 101, body: '은혜받고 갑니다', deleted: false, createdAt: T(0, 1) },
          { id: 32, sharingPostId: 102, body: '함께 기도하겠습니다', deleted: false, createdAt: T(2) },
          { id: 33, sharingPostId: 103, body: '삭제된 댓글', deleted: true, createdAt: T(5) },
        ],
        params,
      );
    case 'liked-posts':
    case 'likes':
      return pageOf(
        [
          { postId: 102, title: '기도제목 나눔', status: 'PUBLISHED', likedAt: T(1) },
          { postId: 101, title: '오늘 묵상 나눔', status: 'PUBLISHED', likedAt: T(0, 2) },
          { postId: 105, title: '감사 제목 나눔', status: 'PUBLISHED', likedAt: T(3) },
        ],
        params,
      );
    case 'nickname-history':
      return pageOf(
        [
          { oldNickname: '이전닉네임', newNickname: '은혜로운하루', changedAt: T(10) },
          { oldNickname: '처음닉네임', newNickname: '이전닉네임', changedAt: T(40) },
        ],
        params,
      );
    default:
      return pageOf([], params);
  }
}

// ── 라우트 표 ───────────────────────────────────────────────────────────────
type Ctx = { params: Record<string, any>; body: any; groups: RegExpMatchArray };
type Route = { method: string; pattern: RegExp; handle: (ctx: Ctx) => any };

const num = (s: string) => Number(s);

const routes: Route[] = [
  // 인증 / 본인
  { method: 'post', pattern: /^\/admin\/auth\/login$/, handle: () => ({ accessToken: 'mock-access-token', refreshToken: 'mock-refresh-token', admin: { memberId: 1, nickname: '강태오', role: 'ADMIN', adminRole: 'SUPER_ADMIN', status: 'ACTIVE' } }) },
  { method: 'get', pattern: /^\/admin\/me$/, handle: () => ({ adminUserId: 1, memberId: 1, adminRole: 'SUPER_ADMIN' }) },

  // 대시보드 (AD-01)
  { method: 'get', pattern: /^\/admin\/dashboard$/, handle: () => ({
    pendingAiValidationCount: db.aiAssets.filter((a) => a.status === 'VALIDATING').length,
    receivedReportCount: db.reports.filter((r) => r.status === 'RECEIVED').length,
    reviewingReportCount: db.reports.filter((r) => r.status === 'REVIEWING').length,
    todayQt: { qtDate: TODAY, qtPassageId: 7, title: '고린도전서 12:1-11', status: 'READY', simulatorStatus: 'READY', hasExplanation: true, cacheStatus: 'FRESH' },
    recentAuditLogs: db.auditLogs.slice(0, 5).map((a) => ({ id: a.id, adminUserId: a.adminUserId, actorType: a.actorType, actionType: a.actionType, targetType: a.targetType, targetId: a.targetId, createdAt: a.createdAt })),
  }) },

  // 오늘 QT 관리 (AD-02)
  { method: 'get', pattern: /^\/admin\/qt-passages$/, handle: ({ params }) => pageOf(applyFilters(db.qtPassages, params, ['title', 'mainVerseRef']), params) },
  { method: 'post', pattern: /^\/admin\/qt-passages$/, handle: ({ body }) => { const row = { id: nextId(), qtDate: body?.qtDate ?? TODAY, bookId: body?.bookId ?? 19, chapter: body?.chapter ?? 1, endChapter: body?.endChapter ?? body?.chapter ?? 1, startVerse: body?.startVerse ?? 1, endVerse: body?.endVerse ?? 1, title: body?.title ?? '새 본문', mainVerseRef: body?.mainVerseRef ?? null, status: 'pending_review', publishedAt: null, collectedAt: T(), hiddenAt: null, createdAt: T(), updatedAt: T() }; db.qtPassages.unshift(row); return row; } },
  { method: 'post', pattern: /^\/admin\/qt-passages\/(\d+)\/publish$/, handle: ({ groups }) => updateRow(db.qtPassages, num(groups[1]), { status: 'active', publishedAt: T(), updatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/qt-passages\/(\d+)\/hide$/, handle: ({ groups }) => updateRow(db.qtPassages, num(groups[1]), { status: 'hidden', hiddenAt: T(), updatedAt: T() }) },
  { method: 'patch', pattern: /^\/admin\/qt-passages\/(\d+)$/, handle: ({ groups, body }) => updateRow(db.qtPassages, num(groups[1]), { ...body, updatedAt: T() }) },

  // AI 산출물 검증 (AD-03)
  { method: 'get', pattern: /^\/admin\/ai\/assets$/, handle: ({ params }) => pageOf(applyFilters(db.aiAssets, params, []), params) },
  { method: 'get', pattern: /^\/admin\/ai\/assets\/(\d+)$/, handle: ({ groups }) => aiAssetDetail(num(groups[1])) },
  { method: 'post', pattern: /^\/admin\/ai\/assets\/(\d+)\/approve$/, handle: ({ groups }) => { updateRow(db.aiAssets, num(groups[1]), { status: 'APPROVED' }); return { assetId: num(groups[1]), status: 'APPROVED' }; } },
  { method: 'post', pattern: /^\/admin\/ai\/assets\/(\d+)\/reject$/, handle: ({ groups }) => { updateRow(db.aiAssets, num(groups[1]), { status: 'REJECTED' }); return { assetId: num(groups[1]), status: 'REJECTED' }; } },
  { method: 'post', pattern: /^\/admin\/ai\/assets\/(\d+)\/hide$/, handle: ({ groups }) => { updateRow(db.aiAssets, num(groups[1]), { status: 'HIDDEN' }); return { assetId: num(groups[1]), status: 'HIDDEN' }; } },
  { method: 'post', pattern: /^\/admin\/ai\/assets\/(\d+)\/regenerate$/, handle: () => ({ generationJobId: nextId(), status: 'QUEUED', createdAt: T() }) },
  { method: 'post', pattern: /^\/admin\/ai\/qt-passages\/(\d+)\/explanations\/generate$/, handle: () => ({ createdCount: 2, failedCount: 0, reason: null }) },

  // AI 운영 모니터링 (AD-08)
  { method: 'get', pattern: /^\/admin\/ai\/monitoring$/, handle: () => aiMonitoring() },

  // AI 검증 체크리스트 (AD-09)
  { method: 'get', pattern: /^\/admin\/ai\/validation-checklists$/, handle: ({ params }) => pageOf(applyFilters(db.checklists, params, ['version']), params) },
  { method: 'post', pattern: /^\/admin\/ai\/validation-checklists$/, handle: ({ body }) => { const row = { id: nextId(), checklistType: body?.checklistType ?? 'EXPLANATION', version: body?.version ?? 'v1.0', contentHash: body?.contentHash ?? null, status: body?.status ?? 'DRAFT', createdByAdminId: 1, createdAt: T(), activatedAt: null, retiredAt: null }; db.checklists.unshift(row); return row; } },
  { method: 'post', pattern: /^\/admin\/ai\/validation-checklists\/(\d+)\/activate$/, handle: ({ groups }) => updateRow(db.checklists, num(groups[1]), { status: 'ACTIVE', activatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/ai\/validation-checklists\/(\d+)\/retire$/, handle: ({ groups }) => updateRow(db.checklists, num(groups[1]), { status: 'RETIRED', retiredAt: T() }) },

  // AI 배치 실행 로그 (AD-10)
  { method: 'get', pattern: /^\/admin\/ai\/batch-run-logs$/, handle: ({ params }) => pageOf(applyFilters(db.batchLogs, params, ['batchName']), params) },

  // AI 평가 세트 (AD-11)
  { method: 'get', pattern: /^\/admin\/ai\/evaluation-sets$/, handle: ({ params }) => pageOf(applyFilters(db.evalSets, params, ['name', 'version']), params) },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-sets$/, handle: ({ body }) => { const row = { id: nextId(), name: body?.name ?? '새 평가셋', evalType: body?.evalType ?? 'EXPLANATION', version: body?.version ?? 'v1.0', targetType: body?.targetType ?? 'QT_PASSAGE', expectedPolicyJson: body?.expectedPolicyJson ?? null, description: body?.description ?? null, status: 'DRAFT', createdAt: T(), activatedAt: null, retiredAt: null }; db.evalSets.unshift(row); return row; } },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/activate$/, handle: ({ groups }) => updateRow(db.evalSets, num(groups[1]), { status: 'ACTIVE', activatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/retire$/, handle: ({ groups }) => updateRow(db.evalSets, num(groups[1]), { status: 'RETIRED', retiredAt: T() }) },
  { method: 'get', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/cases$/, handle: ({ groups, params }) => pageOf(db.evalCases.filter((c) => c.evaluationSetId === num(groups[1])), params) },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/cases$/, handle: ({ groups, body }) => { const row = { id: nextId(), evaluationSetId: num(groups[1]), targetType: body?.targetType ?? 'QT_PASSAGE', targetId: body?.targetId ?? null, sourceType: body?.sourceType ?? 'AI_ASSET', sourceId: null, inputJson: null, expectedOutputJson: null, expectedPolicyJson: body?.expectedPolicyJson ?? null, status: 'CANDIDATE', reviewedByAdminId: null, reviewedAt: null, createdAt: T() }; db.evalCases.unshift(row); return row; } },
  { method: 'get', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/runs\/latest$/, handle: ({ groups }) => evalRun(num(groups[1])) },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-sets\/(\d+)\/runs$/, handle: ({ groups }) => evalRun(num(groups[1])) },
  { method: 'get', pattern: /^\/admin\/ai\/evaluation-runs\/(\d+)$/, handle: ({ groups }) => evalRun(901, num(groups[1])) },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-cases\/(\d+)\/approve$/, handle: ({ groups }) => { updateRow(db.evalCases, num(groups[1]), { status: 'APPROVED', reviewedAt: T(), reviewedByAdminId: 1 }); return { id: num(groups[1]), status: 'APPROVED' }; } },
  { method: 'post', pattern: /^\/admin\/ai\/evaluation-cases\/(\d+)\/reject$/, handle: ({ groups }) => { updateRow(db.evalCases, num(groups[1]), { status: 'REJECTED', reviewedAt: T(), reviewedByAdminId: 1 }); return { id: num(groups[1]), status: 'REJECTED' }; } },
  { method: 'post', pattern: /^\/admin\/ai\/assets\/(\d+)\/evaluation-candidates$/, handle: () => newEvalCase() },
  { method: 'post', pattern: /^\/admin\/ai\/reports\/(\d+)\/evaluation-candidates$/, handle: () => newEvalCase() },

  // AI 프롬프트 관리 (AI-PROMPT)
  { method: 'get', pattern: /^\/admin\/ai\/prompt-versions$/, handle: ({ params }) => pageOf(applyFilters(db.promptVersions, params, ['version', 'description']), params) },
  { method: 'get', pattern: /^\/admin\/ai\/prompt-versions\/(\d+)$/, handle: ({ groups }) => db.promptVersions.find((p) => p.id === num(groups[1])) ?? db.promptVersions[0] },
  { method: 'post', pattern: /^\/admin\/ai\/prompt-versions$/, handle: ({ body }) => { const row = { id: nextId(), promptType: 'EXPLANATION', version: body?.version ?? 'v0.0', contentHash: null, status: 'DRAFT', systemPrompt: body?.systemPrompt ?? '', userPromptTemplate: body?.userPromptTemplate ?? '', modelName: body?.modelName ?? 'deepseek-chat', temperature: body?.temperature ?? 0.3, maxTokens: body?.maxTokens ?? 1024, description: body?.description ?? null, createdByAdminId: 1, createdAt: T(), activatedAt: null, retiredAt: null }; db.promptVersions.unshift(row); return row; } },
  { method: 'post', pattern: /^\/admin\/ai\/prompt-versions\/(\d+)\/activate$/, handle: ({ groups }) => updateRow(db.promptVersions, num(groups[1]), { status: 'ACTIVE', activatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/ai\/prompt-versions\/(\d+)\/retire$/, handle: ({ groups }) => updateRow(db.promptVersions, num(groups[1]), { status: 'RETIRED', retiredAt: T() }) },

  // 신고 처리 (AD-04)
  { method: 'get', pattern: /^\/admin\/reports$/, handle: ({ params }) => pageOf(applyFilters(db.reports, params, ['detail', 'reason']), params) },
  { method: 'post', pattern: /^\/admin\/reports\/test-seed$/, handle: () => { const row = { id: nextId(), reporterMemberId: 3, targetType: 'POST', targetId: 101, reason: 'TEST', detail: '[테스트] 시드 신고', status: 'RECEIVED', processedByAdminId: null, processedAt: null, createdAt: T() }; db.reports.unshift(row); return { id: row.id, status: row.status, createdAt: row.createdAt }; } },
  { method: 'post', pattern: /^\/admin\/reports\/(\d+)\/resolve$/, handle: ({ groups }) => { updateRow(db.reports, num(groups[1]), { status: 'RESOLVED', processedByAdminId: 1, processedAt: T() }); return { reportId: num(groups[1]), status: 'RESOLVED', processedByAdminId: 1, processedAt: T() }; } },
  { method: 'post', pattern: /^\/admin\/reports\/(\d+)\/reject$/, handle: ({ groups }) => { updateRow(db.reports, num(groups[1]), { status: 'REJECTED', processedByAdminId: 1, processedAt: T() }); return { reportId: num(groups[1]), status: 'REJECTED', processedByAdminId: 1, processedAt: T() }; } },

  // 회원 관리 (AD-13)
  { method: 'get', pattern: /^\/admin\/members$/, handle: ({ params }) => pageOf(applyFilters(db.members, params, ['nickname']), params) },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)\/detail$/, handle: ({ groups }) => memberDetail(num(groups[1])) },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)\/missions$/, handle: () => [
    { missionDefinitionId: 301, code: 'DAILY_QT', title: '매일 묵상 저장', metricType: 'MEDITATION_SAVED_DAYS', periodType: 'DAILY', currentCount: 1, targetCount: 1, progressRate: 1, completed: true, periodStartDate: TODAY, periodEndDate: TODAY, completedAt: T(0, 2) },
    { missionDefinitionId: 302, code: 'WEEKLY_NOTE', title: '주간 노트 3회', metricType: 'NOTE_SAVED_COUNT', periodType: 'WEEKLY', currentCount: 2, targetCount: 3, progressRate: 0.66, completed: false, periodStartDate: T(3), periodEndDate: T(-3), completedAt: null },
  ] },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)\/notes\/(\d+)$/, handle: ({ groups }) => ({ id: num(groups[2]), qtPassageId: 7, category: 'MEDITATION', status: 'SAVED', visibility: 'PRIVATE', title: '오늘의 묵상', body: '오늘 본문 묵상 내용입니다.', rememberSection: '기억할 말씀', interpretSection: '본문 해석', applySection: '적용', praySection: '기도', createdAt: T(0, 2) }) },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)\/posts\/(\d+)$/, handle: ({ groups }) => ({ id: num(groups[2]), status: 'PUBLISHED', title: '오늘 묵상 나눔', body: '오늘 본문을 통해 받은 은혜를 나눕니다.', category: 'GRATITUDE', verseLabel: '고린도전서 12:1', noteId: 11, likeCount: 12, commentCount: 3, createdAt: T(0, 3) }) },
  { method: 'patch', pattern: /^\/admin\/members\/(\d+)\/status$/, handle: ({ groups, body }) => updateRow(db.members, num(groups[1]), { status: body?.status ?? 'ACTIVE' }) },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)\/([a-z-]+)$/, handle: ({ groups, params }) => memberSubList(groups[2], params) },
  { method: 'get', pattern: /^\/admin\/members\/(\d+)$/, handle: ({ groups }) => db.members.find((m) => m.id === num(groups[1])) ?? db.members[0] },

  // 미션 관리 (AD-16)
  { method: 'get', pattern: /^\/admin\/missions$/, handle: () => db.missions },
  { method: 'post', pattern: /^\/admin\/missions$/, handle: ({ body }) => { const row = { id: nextId(), code: body?.code ?? 'NEW', title: body?.title ?? '새 미션', metricType: body?.metricType ?? 'NOTE_SAVED_COUNT', periodType: body?.periodType ?? 'DAILY', targetCount: body?.targetCount ?? 1, status: 'ACTIVE', createdAt: T(), updatedAt: null }; db.missions.unshift(row); return row; } },
  { method: 'patch', pattern: /^\/admin\/missions\/(\d+)\/status$/, handle: ({ groups }) => { const m = db.missions.find((x) => x.id === num(groups[1])); const next = m?.status === 'ACTIVE' ? 'HIDDEN' : 'ACTIVE'; return updateRow(db.missions, num(groups[1]), { status: next, updatedAt: T() }); } },
  { method: 'patch', pattern: /^\/admin\/missions\/(\d+)$/, handle: ({ groups, body }) => updateRow(db.missions, num(groups[1]), { ...body, updatedAt: T() }) },

  // 나눔 공유글 관리 (AD-15)
  { method: 'get', pattern: /^\/admin\/sharing-posts$/, handle: ({ params }) => pageOf(applyFilters(db.sharingPosts, params, ['titleSnapshot', 'nicknameSnapshot']), params) },
  { method: 'get', pattern: /^\/admin\/sharing-posts\/(\d+)$/, handle: ({ groups }) => db.sharingPosts.find((p) => p.id === num(groups[1])) ?? db.sharingPosts[0] },
  { method: 'patch', pattern: /^\/admin\/sharing-posts\/(\d+)\/hide$/, handle: ({ groups }) => updateRow(db.sharingPosts, num(groups[1]), { status: 'HIDDEN', hiddenAt: T() }) },
  { method: 'patch', pattern: /^\/admin\/sharing-posts\/(\d+)\/restore$/, handle: ({ groups }) => updateRow(db.sharingPosts, num(groups[1]), { status: 'PUBLISHED', hiddenAt: null }) },

  // 시스템 공지 (AD-06)
  { method: 'get', pattern: /^\/admin\/notices$/, handle: ({ params }) => pageOf(applyFilters(db.notices, params, ['title']), params) },
  { method: 'get', pattern: /^\/admin\/notices\/(\d+)$/, handle: ({ groups }) => db.notices.find((n) => n.id === num(groups[1])) ?? db.notices[0] },
  { method: 'post', pattern: /^\/admin\/notices$/, handle: ({ body }) => { const row = { id: nextId(), title: body?.title ?? '새 공지', body: body?.body ?? '', bodyPreview: (body?.body ?? '').slice(0, 40), status: 'DRAFT', publishedAt: null, createdAt: T(), updatedAt: T() }; db.notices.unshift(row); return row; } },
  { method: 'patch', pattern: /^\/admin\/notices\/(\d+)$/, handle: ({ groups, body }) => updateRow(db.notices, num(groups[1]), { ...body, bodyPreview: (body?.body ?? '').slice(0, 40), updatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/notices\/(\d+)\/publish$/, handle: ({ groups }) => { const n = updateRow(db.notices, num(groups[1]), { status: 'PUBLISHED', publishedAt: T() }); return { id: nextId(), noticeId: n.id, status: 'PUBLISHED', publishedAt: T(), notificationResult: { requestedCount: 1, targetMemberCount: 1250, createdCount: 1250, queuedCount: 1250, failedCount: 0 } }; } },
  { method: 'post', pattern: /^\/admin\/notices\/(\d+)\/hide$/, handle: ({ groups }) => updateRow(db.notices, num(groups[1]), { status: 'HIDDEN' }) },

  // 배경음악 관리 (AD-12)
  { method: 'get', pattern: /^\/admin\/music-tracks$/, handle: ({ params }) => pageOf(applyFilters(db.musicTracks, params, ['title']), params) },
  { method: 'post', pattern: /^\/admin\/music-tracks$/, handle: () => { const row = { id: nextId(), title: '업로드한 트랙', category: 'BGM', mimeType: 'audio/mpeg', byteSize: 3_000_000, durationSec: 180, sortOrder: db.musicTracks.length + 1, licenseNote: '직접 제작', status: 'ACTIVE', streamUrl: `/api/v1/music/tracks/${seq}/stream`, createdAt: T(), updatedAt: null }; db.musicTracks.unshift(row); return row; } },
  { method: 'patch', pattern: /^\/admin\/music-tracks\/(\d+)$/, handle: ({ groups, body }) => updateRow(db.musicTracks, num(groups[1]), { ...body, updatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/music-tracks\/(\d+)\/publish$/, handle: ({ groups }) => updateRow(db.musicTracks, num(groups[1]), { status: 'ACTIVE', updatedAt: T() }) },
  { method: 'post', pattern: /^\/admin\/music-tracks\/(\d+)\/hide$/, handle: ({ groups }) => updateRow(db.musicTracks, num(groups[1]), { status: 'HIDDEN', updatedAt: T() }) },
  { method: 'delete', pattern: /^\/admin\/music-tracks\/(\d+)$/, handle: ({ groups }) => { removeRow(db.musicTracks, num(groups[1])); return {}; } },

  // 감사 로그 (AD-07)
  { method: 'get', pattern: /^\/admin\/audit-logs$/, handle: ({ params }) => pageOf(applyFilters(db.auditLogs, params, ['actionType', 'actorLabel']), params) },

  // 앱 버전/업데이트 (AD-19)
  { method: 'get', pattern: /^\/admin\/app-updates\/state$/, handle: () => db.appState },
  { method: 'post', pattern: /^\/admin\/app-updates\/apply-content$/, handle: () => { db.appState.contentVersion = TODAY.replace(/-/g, '.'); db.appState.updatedAt = T(); return db.appState; } },
  { method: 'get', pattern: /^\/admin\/app-updates\/pending$/, handle: ({ params }) => (params.status ? db.pendingUpdates.filter((p) => p.status === params.status) : db.pendingUpdates) },
  { method: 'post', pattern: /^\/admin\/app-updates\/pending$/, handle: ({ body }) => { const row = { id: nextId(), title: body?.title ?? '새 업데이트', description: body?.description ?? null, targetAppVersion: body?.targetAppVersion ?? '1.5.0', updateMode: body?.updateMode ?? 'RECOMMENDED', status: 'PENDING', createdAt: T(), appliedAt: null }; db.pendingUpdates.unshift(row); return row; } },
  { method: 'post', pattern: /^\/admin\/app-updates\/pending\/(\d+)\/apply$/, handle: ({ groups }) => { const p = db.pendingUpdates.find((x) => x.id === num(groups[1])); if (p) { p.status = 'APPLIED'; p.appliedAt = T(); db.appState.appVersion = p.targetAppVersion; db.appState.updatedAt = T(); } return db.appState; } },
  { method: 'delete', pattern: /^\/admin\/app-updates\/pending\/(\d+)$/, handle: ({ groups }) => { removeRow(db.pendingUpdates, num(groups[1])); return {}; } },

  // QT 영상 관리 (AD-20)
  { method: 'get', pattern: /^\/admin\/qt-videos\/source-videos$/, handle: ({ params }) => pageOf(applyFilters(db.sourceVideos, params, ['title']), params) },
  { method: 'get', pattern: /^\/admin\/qt-videos\/bible-books$/, handle: () => db.bibleBooks },
  { method: 'post', pattern: /^\/admin\/qt-videos\/source-videos$/, handle: ({ body }) => { const row = { id: nextId(), bibleBookId: body?.bibleBookId ?? 46, title: body?.title ?? '새 소스 영상', videoUrl: body?.videoUrl ?? '', durationSec: body?.durationSec ?? 0, status: 'ACTIVE', createdAt: T() }; db.sourceVideos.unshift(row); return row; } },
  { method: 'patch', pattern: /^\/admin\/qt-videos\/source-videos\/(\d+)$/, handle: ({ groups, body }) => updateRow(db.sourceVideos, num(groups[1]), { ...body }) },
  { method: 'delete', pattern: /^\/admin\/qt-videos\/source-videos\/(\d+)$/, handle: ({ groups }) => { removeRow(db.sourceVideos, num(groups[1])); return {}; } },
  { method: 'get', pattern: /^\/admin\/qt-videos\/source-videos\/(\d+)\/segments$/, handle: () => db.segments },
  { method: 'put', pattern: /^\/admin\/qt-videos\/source-videos\/(\d+)\/segments$/, handle: ({ body }) => (Array.isArray(body) ? body.map((s: any, i: number) => ({ id: i + 1, bibleVerseId: s.bibleVerseId ?? 4600 + i, startTimeSec: s.startTimeSec ?? 0, endTimeSec: s.endTimeSec ?? 0 })) : db.segments) },
  { method: 'get', pattern: /^\/admin\/qt-videos\/clips$/, handle: ({ params }) => pageOf(applyFilters(db.qtVideoClips, params, ['title']), params) },
  { method: 'post', pattern: /^\/admin\/qt-videos\/qt-passages\/(\d+)\/clips\/prepare$/, handle: ({ groups }) => ({ qtPassageId: num(groups[1]), prepared: true, clipId: nextId() }) },
  { method: 'post', pattern: /^\/admin\/qt-videos\/clips\/manual$/, handle: ({ body }) => { const sv = db.sourceVideos.find((v) => v.id === (body?.sourceVideoId ?? 1)) ?? db.sourceVideos[0]; const row = { id: nextId(), qtPassageId: body?.qtPassageId ?? 7, title: '수동 등록 클립', sourceVideoId: sv?.id ?? 1, videoUrl: sv?.videoUrl ?? '', startTimeSec: body?.startTimeSec ?? 0, endTimeSec: body?.endTimeSec ?? 60, compositionType: 'SINGLE', status: 'APPROVED', approvedAt: T() }; db.qtVideoClips.unshift(row); return row; } },
  { method: 'delete', pattern: /^\/admin\/qt-videos\/clips\/(\d+)$/, handle: ({ groups }) => { removeRow(db.qtVideoClips, num(groups[1])); return {}; } },
  { method: 'patch', pattern: /^\/admin\/qt-videos\/clips\/(\d+)\/status$/, handle: ({ groups, body }) => updateRow(db.qtVideoClips, num(groups[1]), { status: body?.status ?? 'APPROVED', approvedAt: T() }) },

  // 시뮬레이터 관리 (AD-14)
  { method: 'get', pattern: /^\/admin\/simulator-clips$/, handle: ({ params }) => pageOf(applyFilters(db.simulatorClips, params, ['title']), params) },
  { method: 'post', pattern: /^\/admin\/simulator-clips\/(\d+)\/hide$/, handle: ({ groups }) => { const aiAssetId = num(groups[1]); db.simulatorClips.forEach((c) => { if (c.aiAssetId === aiAssetId) c.status = 'DISABLED'; }); return { aiAssetId, hiddenCount: 1 }; } },
];

// ── 액션 보조 ───────────────────────────────────────────────────────────────
function updateRow(arr: any[], id: number, patch: Record<string, any>) {
  const row = arr.find((r) => r.id === id);
  if (row) Object.assign(row, patch);
  return row ?? { id, ...patch };
}

function removeRow(arr: any[], id: number) {
  const i = arr.findIndex((r) => r.id === id);
  if (i >= 0) arr.splice(i, 1);
}

function aiAssetDetail(id: number) {
  const a = db.aiAssets.find((x) => x.id === id) ?? db.aiAssets[0];
  return {
    id: a.id,
    assetType: a.assetType,
    targetType: a.targetType,
    targetId: a.targetId,
    status: a.status,
    payloadJson: { explanation: '고린도전서 12장은 성령의 은사를 다룹니다. (시연용 목업 해설)', sourceLabel: '검증용 참조 자료(비노출)' },
    sourceLabel: a.sourceLabelPresent ? '출처 라벨 있음' : null,
    createdAt: a.createdAt,
    reviewedAt: a.status === 'VALIDATING' ? null : T(0, 2),
    generationJob: { id: 5001, jobType: 'EXPLANATION', targetType: a.targetType, targetId: a.targetId, promptVersionId: 801, status: 'SUCCEEDED', createdAt: a.createdAt, startedAt: a.createdAt, finishedAt: a.createdAt, errorMessage: null },
    activeGenerationJob: null,
    promptVersion: a.promptVersion,
    validationLogs: [
      { validationLogId: 6001, validationReferenceJobId: 5001, checklistVersionId: 701, layer: 1, result: a.autoValidationResult ?? 'PASSED', reviewerType: 'AUTO', errorMessage: null, createdAt: a.createdAt },
      { validationLogId: 6002, validationReferenceJobId: 5001, checklistVersionId: 701, layer: 2, result: a.advisorValidationResult ?? 'PASSED', reviewerType: 'ADVISOR', errorMessage: null, createdAt: a.createdAt },
    ],
  };
}

function newEvalCase() {
  const row = { id: nextId(), evaluationSetId: 901, targetType: 'QT_PASSAGE', targetId: 7, sourceType: 'AI_ASSET', sourceId: 601, inputJson: null, expectedOutputJson: null, expectedPolicyJson: null, status: 'CANDIDATE', reviewedByAdminId: null, reviewedAt: null, createdAt: T() };
  db.evalCases.unshift(row);
  return row;
}

function evalRun(setId: number, runId = 7001) {
  return {
    id: runId,
    evaluationSetId: setId,
    promptVersionId: 801,
    status: 'SUCCEEDED',
    totalCount: 2,
    passedCount: 2,
    failedCount: 0,
    needsReviewCount: 0,
    startedAt: T(0, 1),
    finishedAt: T(0, 1),
    requestedByAdminId: 1,
    results: [
      { id: 7101, evaluationCaseId: 9101, result: 'PASSED', reason: null, outputSummaryJson: '{"ok":true}', createdAt: T(0, 1) },
      { id: 7102, evaluationCaseId: 9102, result: 'PASSED', reason: null, outputSummaryJson: '{"ok":true}', createdAt: T(0, 1) },
    ],
  };
}

function aiMonitoring() {
  return {
    period: { from: T(7), to: T(0), timezone: 'Asia/Seoul' },
    generationJobs: { queued: 1, running: 0, succeeded: 42, failed: 2 },
    assetStatuses: { validating: 1, approved: 38, rejected: 3, hidden: 1 },
    validation: { waitingAssets: 1, approvedAssets: 38, rejectedAssets: 3, hiddenAssets: 1, passCount: 76, failCount: 4, needsReviewCount: 2, failureReasons: [{ resultCode: 'MISSING_SOURCE_LABEL', count: 2 }, { resultCode: 'LLM_TIMEOUT', count: 2 }] },
    batchRuns: { succeeded: 12, partialFailed: 1, failed: 0, latestFailures: [{ id: 803, batchName: 'AI_EXPLANATION_SEED', status: 'PARTIAL_FAILED', errorType: 'LLM_TIMEOUT', errorMessage: 'DeepSeek 응답 지연', createdAt: T(1) }] },
    qa: { requested: 30, answered: 26, blocked: 3, failed: 1, blockedReasons: [{ blockedReason: 'VALUE_JUDGMENT', count: 2 }, { blockedReason: 'COUNSELING', count: 1 }] },
    checklists: [
      { checklistType: 'EXPLANATION', activeVersion: 'v2.1', passRate: 0.95 },
      { checklistType: 'QA', activeVersion: 'v1.0', passRate: 0.93 },
    ],
  };
}

// ── 어댑터 본체 ─────────────────────────────────────────────────────────────
function parseBody(data: unknown): any {
  if (data == null) return undefined;
  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch {
      return data;
    }
  }
  return data;
}

export const mockAdapter: AxiosAdapter = (config) =>
  new Promise<AxiosResponse>((resolve) => {
    const method = (config.method ?? 'get').toLowerCase();
    const url = (config.url ?? '').split('?')[0];
    const params = (config.params ?? {}) as Record<string, any>;
    const body = parseBody(config.data);

    let payload: any = {};
    for (const r of routes) {
      if (r.method !== method) continue;
      const groups = url.match(r.pattern);
      if (!groups) continue;
      payload = r.handle({ params, body, groups });
      break;
    }

    // 시연 체감을 위한 짧은 지연(80ms) 후 봉투로 감싸 200 응답.
    setTimeout(() => {
      resolve({
        data: ok(payload),
        status: 200,
        statusText: 'OK',
        headers: {},
        config: config as InternalAxiosRequestConfig,
        request: {},
      } as AxiosResponse);
    }, 80);
  });
