import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import ts from 'typescript';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sourcePath = path.join(rootDir, 'src', 'pages', 'adminPageContracts.ts');
const source = fs.readFileSync(sourcePath, 'utf8');
const pagesDir = path.join(rootDir, 'src', 'pages');

const transpiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ES2022,
    target: ts.ScriptTarget.ES2022,
  },
  fileName: sourcePath,
});

if (/\bimport\b/.test(transpiled.outputText)) {
  throw new Error(
    'adminPageContracts.ts must stay free of runtime imports for this test runner.',
  );
}

const contracts = await import(
  `data:text/javascript;charset=utf-8,${encodeURIComponent(transpiled.outputText)}`
);

const hasOwn = (value, key) => Object.prototype.hasOwnProperty.call(value, key);

function test(name, fn) {
  try {
    fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    console.error(`FAIL ${name}`);
    throw error;
  }
}

test('AI asset status filters use asset statuses only', () => {
  assert.deepEqual(contracts.AI_ASSET_FILTERABLE_STATUSES, [
    'VALIDATING',
    'APPROVED',
    'REJECTED',
    'HIDDEN',
  ]);
  assert.equal(contracts.AI_ASSET_FILTERABLE_STATUSES.includes('NEEDS_REVIEW'), false);
  assert.equal(contracts.AI_ASSET_DEFAULT_STATUS, 'VALIDATING');
  assert.equal(
    contracts.AI_ASSET_FILTERABLE_STATUSES.includes(contracts.AI_ASSET_DEFAULT_STATUS),
    true,
  );
});

test('AI assets page starts and resets with the validating status filter', () => {
  const page = fs.readFileSync(path.join(pagesDir, 'AiAssetsPage.tsx'), 'utf8');

  assert.match(page, /status:\s*AI_ASSET_DEFAULT_STATUS/);
  assert.match(
    page,
    /useState<string \| undefined>\(AI_ASSET_DEFAULT_STATUS\)/,
  );
  assert.match(page, /setStatus\(AI_ASSET_DEFAULT_STATUS\)/);
  assert.match(page, /applyFilters\(\{\s*status:\s*AI_ASSET_DEFAULT_STATUS\s*\}\)/);
});

test('admin page modals use destroyOnHidden instead of deprecated destroyOnClose', () => {
  const modalPages = [
    ['AiAssetsPage.tsx', 4],
    ['AiChecklistsPage.tsx', 1],
    ['AiEvaluationsPage.tsx', 4],
    ['PraiseSongsPage.tsx', 2],
    ['ReportsPage.tsx', 2],
  ];

  for (const [fileName, expectedCount] of modalPages) {
    const page = fs.readFileSync(path.join(pagesDir, fileName), 'utf8');
    assert.equal(page.includes('destroyOnClose'), false, fileName);
    assert.equal((page.match(/\bdestroyOnHidden\b/g) ?? []).length, expectedCount, fileName);
  }
});

test('audit log actor filter exposes only admin and system batch actors', () => {
  const page = fs.readFileSync(path.join(pagesDir, 'AuditLogsPage.tsx'), 'utf8');
  const optionsMatch = page.match(/const ACTOR_TYPE_OPTIONS = \[([\s\S]*?)\];/);

  assert.ok(optionsMatch, 'ACTOR_TYPE_OPTIONS must be declared in AuditLogsPage.tsx');

  const optionsSource = optionsMatch[1];
  assert.match(optionsSource, /label:\s*'ADMIN',\s*value:\s*'ADMIN'/);
  assert.match(
    optionsSource,
    /label:\s*'SYSTEM_BATCH',\s*value:\s*'SYSTEM_BATCH'/,
  );
  assert.doesNotMatch(optionsSource, /label:\s*'USER'|value:\s*'USER'/);
  assert.match(page, /options=\{ACTOR_TYPE_OPTIONS\}/);
});

test('AI asset action visibility follows review and regeneration contracts', () => {
  assert.equal(contracts.isAiAssetReviewable('VALIDATING'), true);
  assert.equal(contracts.isAiAssetReviewable('APPROVED'), false);
  assert.equal(contracts.isAiAssetRegeneratable('REJECTED'), true);
  assert.equal(contracts.isAiAssetRegeneratable('HIDDEN'), true);
  assert.equal(contracts.isAiAssetRegeneratable('APPROVED'), false);
});

test('AI asset active regeneration job prefers cached active jobs', () => {
  assert.deepEqual(
    contracts.resolveActiveRegenerationJob(
      { generationJobId: 7, status: 'QUEUED' },
      { id: 8, status: 'RUNNING' },
      { id: 9, status: 'SUCCEEDED' },
    ),
    { generationJobId: 7, status: 'QUEUED' },
  );

  assert.deepEqual(
    contracts.resolveActiveRegenerationJob(
      undefined,
      { id: 8, status: 'RUNNING' },
      { id: 9, status: 'SUCCEEDED' },
    ),
    { generationJobId: 8, status: 'RUNNING' },
  );

  assert.equal(
    contracts.resolveActiveRegenerationJob(
      undefined,
      { id: 8, status: 'SUCCEEDED' },
      { id: 9, status: 'FAILED' },
    ),
    undefined,
  );
});

test('AI asset evaluation set params preserve targetType contract', () => {
  assert.deepEqual(contracts.aiAssetEvaluationSetListParams('QT_PASSAGE'), {
    targetType: 'QT_PASSAGE',
    size: 100,
  });
  assert.deepEqual(contracts.aiAssetEvaluationSetListParams(null), {
    targetType: undefined,
    size: 100,
  });
});

test('praise song payload remains metadata-only', () => {
  assert.deepEqual(contracts.PRAISE_SONG_FILTERABLE_STATUSES, ['ACTIVE', 'HIDDEN']);

  const payload = contracts.buildPraiseSongCreatePayload({
    title: 'title',
    artist: 'artist',
    licenseNote: 'license',
    status: 'ACTIVE',
    sourceType: 'DEVICE',
    lyrics: 'blocked',
    youtubeUrl: 'blocked',
  });

  assert.deepEqual(payload, {
    title: 'title',
    artist: 'artist',
    licenseNote: 'license',
    status: 'ACTIVE',
  });
  assert.equal(hasOwn(payload, 'sourceType'), false);
  assert.equal(hasOwn(payload, 'lyrics'), false);
  assert.equal(hasOwn(payload, 'youtubeUrl'), false);
});

test('QT passage filters and row actions use operational statuses only', () => {
  assert.deepEqual(contracts.QT_PASSAGE_FILTERABLE_STATUSES, [
    'pending_review',
    'active',
    'hidden',
  ]);

  assert.deepEqual(contracts.qtPassageActionsForStatus('pending_review'), {
    canEdit: true,
    canPublish: true,
    canHide: false,
  });
  assert.deepEqual(contracts.qtPassageActionsForStatus('active'), {
    canEdit: true,
    canPublish: false,
    canHide: true,
  });
  assert.deepEqual(contracts.qtPassageActionsForStatus('deletion_notified'), {
    canEdit: false,
    canPublish: false,
    canHide: false,
  });
});

test('report processing payload hides only resolved POST targets', () => {
  assert.deepEqual(
    contracts.buildReportProcessPayload('resolve', { targetType: 'POST' }, '  spam  ', true),
    {
      reason: 'spam',
      notifyReporter: true,
      action: 'HIDE_TARGET',
    },
  );

  assert.deepEqual(
    contracts.buildReportProcessPayload('reject', { targetType: 'POST' }, '', false),
    {
      reason: undefined,
      notifyReporter: false,
    },
  );

  assert.deepEqual(
    contracts.buildReportProcessPayload('resolve', { targetType: 'COMMENT' }, '', true),
    {
      reason: undefined,
      notifyReporter: true,
    },
  );
});

test('report evaluation candidate visibility and filters stay aligned', () => {
  assert.equal(contracts.isOpenReportStatus('RECEIVED'), true);
  assert.equal(contracts.isOpenReportStatus('RESOLVED'), false);
  assert.equal(contracts.isAiReport('AI_QA_REQUEST'), true);
  assert.equal(contracts.isAiReport('AI_ASSET'), true);
  assert.equal(contracts.isAiReport('POST'), false);

  assert.deepEqual(contracts.reportEvaluationSetListParams({ targetType: 'AI_QA_REQUEST' }), {
    targetType: 'QA_REQUEST',
    size: 100,
  });
  assert.deepEqual(contracts.reportEvaluationSetListParams({ targetType: 'AI_ASSET' }), {
    size: 100,
  });
});
