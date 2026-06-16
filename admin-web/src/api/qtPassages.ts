import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

// ===== AD-02 오늘 QT 관리 =====
// 연결 API (권한: ADMIN + OPERATOR/SUPER_ADMIN)
//   GET   /api/v1/admin/qt-passages              목록
//   POST  /api/v1/admin/qt-passages              등록
//   PATCH /api/v1/admin/qt-passages/{id}         수정
//   POST  /api/v1/admin/qt-passages/{id}/publish 게시
//   POST  /api/v1/admin/qt-passages/{id}/hide    숨김
// 참고: QT 공개 00:00 KST, 사용자 노출·캐시 갱신 04:00 KST (CLAUDE.md §6)
//
// 타입은 백엔드 AdminQtPassageResponse(admin-server) 기준.
// 근거: 계약서 DevA_이지윤/reports/2026-06-10_admin-qt-passages-contract-to-jimin.md

// 6/9 모더레이션 결정 5종 상태값(소문자).
export type QtPassageStatus =
  | 'pending_review'
  | 'active'
  | 'hidden'
  | 'deletion_notified'
  | 'removed';

export interface QtPassage {
  id: number;
  qtDate: string; // YYYY-MM-DD
  bookId: number; // 성경 권 1~66
  chapter: number; // 시작 장
  endChapter: number; // 종료 장 (같은 장이면 chapter와 동일)
  startVerse: number;
  endVerse: number;
  title: string;
  mainVerseRef: string | null;
  status: QtPassageStatus;
  publishedAt: string | null; // 게시 시각 (자동수집 본문은 QT 날짜 04:00 KST)
  collectedAt: string | null; // 수집 시각 (시스템 배치가 실제로 가져온 시각)
  hiddenAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface QtPassageListParams extends PageParams {
  status?: QtPassageStatus;
  from?: string; // YYYY-MM-DD
  to?: string; // YYYY-MM-DD
  q?: string; // 제목·대표 구절 검색
}

// 등록/수정 공통 요청 (계약서 §3).
// 같은 장이면 startVerse <= endVerse. endChapter 미지정 시 백엔드가 chapter로 보정(단일 장).
export interface QtPassageRequest {
  qtDate: string;
  bookId: number;
  chapter: number;
  endChapter?: number;
  startVerse: number;
  endVerse: number;
  title: string;
  mainVerseRef?: string | null;
}

export function listQtPassages(params: QtPassageListParams = {}) {
  return unwrap<Page<QtPassage>>(
    apiClient.get<ApiResponse<Page<QtPassage>>>('/admin/qt-passages', { params }),
  );
}

export function createQtPassage(body: QtPassageRequest) {
  return unwrap<QtPassage>(
    apiClient.post<ApiResponse<QtPassage>>('/admin/qt-passages', body),
  );
}

export function updateQtPassage(id: number, body: QtPassageRequest) {
  return unwrap<QtPassage>(
    apiClient.patch<ApiResponse<QtPassage>>(`/admin/qt-passages/${id}`, body),
  );
}

export function publishQtPassage(id: number) {
  return unwrap<QtPassage>(
    apiClient.post<ApiResponse<QtPassage>>(`/admin/qt-passages/${id}/publish`),
  );
}

export function hideQtPassage(id: number) {
  return unwrap<QtPassage>(
    apiClient.post<ApiResponse<QtPassage>>(`/admin/qt-passages/${id}/hide`),
  );
}
