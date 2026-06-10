# 2026-06-10 QT video clips on MSA

## Goal

Add QT video as a separate feature from the existing simulator flow.

## Scope

- Keep the existing simulator APIs and `simulator_clips` behavior unchanged.
- Add QT video tables for source videos, verse timecodes, and approved QT clips.
- Add a user API that returns the approved QT video clip for a QT passage.
- Add `videoStatus` to Today QT responses so the app can show a QT video entry point without overloading `simulatorStatus`.
- Add Flutter playback UI for the QT video response.

## Non-goals

- No AI video generation.
- No 00:05 automatic video cutting batch in this step.
- No public upload/admin API for video source data.

## Design Notes

- Owner service: `service-bible`, because it already owns `/api/v1/qt`, QT passage lookup, study content, and existing simulator read APIs.
- Data is still on the shared DB during the MSA transition. Schema SQL is added to both the legacy root and `admin-server` migration folders to match the current repository pattern.
- First operation path is manual SQL/import into `qt_video_clips` after the source video and verse timecode rows exist.

## Verification Plan

- Run targeted `:service-bible:test`.
- Run Flutter dependency/analyze checks where possible.
- Write a report with changed files, validation result, and remaining operations work.
