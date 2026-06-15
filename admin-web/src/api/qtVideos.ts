import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

export interface BibleBook {
  id: number;
  testament: string;
  code: string;
  koreanName: string;
  englishName: string;
  displayOrder: number;
}

export interface SourceVideo {
  id: number;
  bibleBookId: number;
  title: string;
  videoUrl: string;
  durationSec: number;
  status: string;
  createdAt: string | null;
}

export interface SourceVideoParams extends PageParams {
  bibleBookId?: number;
  status?: string;
}

export interface SourceVideoPayload {
  bibleBookId: number;
  title: string;
  videoUrl: string;
  durationSec: number;
}

export interface SourceVideoUpdatePayload extends SourceVideoPayload {
  status: string;
}

export interface VideoSegment {
  id: number;
  bibleVerseId: number;
  startTimeSec: number;
  endTimeSec: number;
}

export interface SegmentPayload {
  bibleVerseId?: number;
  chapter?: number;
  verse?: number;
  startTimeSec: number;
  endTimeSec: number;
}

export interface QtVideoClip {
  id: number;
  qtPassageId: number;
  title: string;
  sourceVideoId: number;
  videoUrl: string;
  startTimeSec: number;
  endTimeSec: number;
  compositionType: string;
  status: string;
  approvedAt: string | null;
}

export interface QtVideoClipParams extends PageParams {
  qtPassageId?: number;
  status?: string;
}

export interface PrepareClipResult {
  qtPassageId: number;
  prepared: boolean;
  clipId: number | null;
}

export function listSourceVideos(params: SourceVideoParams = {}) {
  return unwrap<Page<SourceVideo>>(
    apiClient.get<ApiResponse<Page<SourceVideo>>>('/admin/qt-videos/source-videos', {
      params,
    }),
  );
}

export function listBibleBooks() {
  return unwrap<BibleBook[]>(
    apiClient.get<ApiResponse<BibleBook[]>>('/admin/qt-videos/bible-books'),
  );
}

export function createSourceVideo(payload: SourceVideoPayload) {
  return unwrap<SourceVideo>(
    apiClient.post<ApiResponse<SourceVideo>>('/admin/qt-videos/source-videos', payload),
  );
}

export function updateSourceVideo(id: number, payload: SourceVideoUpdatePayload) {
  return unwrap<SourceVideo>(
    apiClient.patch<ApiResponse<SourceVideo>>(
      `/admin/qt-videos/source-videos/${id}`,
      payload,
    ),
  );
}

export function listSegments(sourceVideoId: number) {
  return unwrap<VideoSegment[]>(
    apiClient.get<ApiResponse<VideoSegment[]>>(
      `/admin/qt-videos/source-videos/${sourceVideoId}/segments`,
    ),
  );
}

export function replaceSegments(sourceVideoId: number, segments: SegmentPayload[]) {
  return unwrap<VideoSegment[]>(
    apiClient.put<ApiResponse<VideoSegment[]>>(
      `/admin/qt-videos/source-videos/${sourceVideoId}/segments`,
      { segments },
    ),
  );
}

export function listQtVideoClips(params: QtVideoClipParams = {}) {
  return unwrap<Page<QtVideoClip>>(
    apiClient.get<ApiResponse<Page<QtVideoClip>>>('/admin/qt-videos/clips', {
      params,
    }),
  );
}

export function prepareQtVideoClip(qtPassageId: number) {
  return unwrap<PrepareClipResult>(
    apiClient.post<ApiResponse<PrepareClipResult>>(
      `/admin/qt-videos/qt-passages/${qtPassageId}/clips/prepare`,
      {},
    ),
  );
}

export function changeQtVideoClipStatus(clipId: number, status: string) {
  return unwrap<QtVideoClip>(
    apiClient.patch<ApiResponse<QtVideoClip>>(
      `/admin/qt-videos/clips/${clipId}/status`,
      { status },
    ),
  );
}
