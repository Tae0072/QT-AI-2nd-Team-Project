# 2026-06-10 AD-02 Status Decision Note

This file supplements `2026-06-10_admin-qt-passages-api.md` for PR review traceability.

## Decision

- AD-02 uses the 2026-06-09 moderation status set:
  - `active`
  - `hidden`
  - `pending_review`
  - `deletion_notified`
  - `removed`
- This replaces the earlier draft wording `DRAFT/PUBLISHED/HIDDEN` for the admin qt-passages API.
- The decision was reconfirmed during the 2026-06-10 admin-web contract discussion with Kim Jimin: admin-web can bind to the 5 status values.

## SSoT Alignment

- `04_API_명세서.md` is updated in this PR so AD-02 examples and error-code mapping follow the implemented contract.
- Final combined-release review should keep `07_요구사항_정의서.md`, `04_API_명세서.md`, and admin-web tags/buttons aligned with these 5 values.

## Exposure Boundary

- `qt_passages.status = active` means the QT passage itself is visible/usable from the admin scheduling perspective.
- AI-generated explanation and simulator content still require their own validation status, such as `APPROVED`, before user exposure.
- Therefore the 5-value passage moderation status does not weaken the v3.1 validated-content-only policy.

## Out Of Scope For This PR

- Transitions into `deletion_notified` and `removed`.
- Bulk moderation.
- User-facing UI changes.
