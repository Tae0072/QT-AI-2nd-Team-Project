# 2026-06-10 AD-02 PR Review Notes

## Status Contract Decision

- AD-02 admin qt-passages uses the 2026-06-09 moderation status decision: `active`, `hidden`, `pending_review`, `deletion_notified`, `removed`.
- This supersedes the earlier draft-style `DRAFT/PUBLISHED/HIDDEN` wording for this admin API.
- Kim Jimin confirmed admin-web can connect to the 5-status contract on 2026-06-10; the remaining SSoT task is to keep `04_API_명세서.md` aligned with this implementation before final combined release.

## User Exposure Policy

- `qt_passages.status = active` is only the admin scheduling/visibility gate for a QT passage.
- It does not change the AI validation policy for generated content.
- Verse explanations, simulator clips, and other AI-generated assets remain exposed only when their own validation status is `APPROVED`.
- Therefore `active` on `qt_passages` does not conflict with the v3.1 "validated content only" policy.

## Transition Scope

- This PR intentionally implements only AD-02 list/create/update/publish/hide.
- Allowed transitions in this PR:
  - `pending_review -> active`
  - `hidden -> active`
  - `active -> hidden`
- `deletion_notified` and `removed` are list/filter states for moderation visibility in this PR. Their workflow transitions are follow-up scope.

## Error Code Note

- `DUPLICATE_RESOURCE` keeps `C0003` for duplicate resource conflicts, including duplicate `qtDate`.
- `INVALID_STATUS_TRANSITION` uses `C0007` to avoid sharing the same code with duplicate resource conflicts.
- The previous duplicate `C0003` mapping existed only inside this in-progress PR branch and was corrected before merge.

## Schema Concurrency Backstop

- `qtai-server/admin-server/src/main/resources/db/migration/V3__create_qt_passages.sql` creates `qt_passages.qt_date` as `DATE NOT NULL UNIQUE`.
- The service-level duplicate check gives a friendly `409 C0003`.
- The V3 DB unique constraint remains the final race-condition backstop for concurrent create/update attempts.
- `V31__add_qt_passage_admin_status.sql` adds only admin moderation columns and the `(status, qt_date)` lookup index because the date uniqueness already exists in the baseline schema.
