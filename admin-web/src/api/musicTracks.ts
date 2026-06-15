import { apiClient, unwrap } from './client';
import type { ApiResponse, Page, PageParams } from './types';

export type MusicTrackStatus = 'ACTIVE' | 'HIDDEN';
export type MusicTrackCategory = 'BGM' | 'HYMN';

export interface MusicTrack {
  id: number;
  title: string;
  category: MusicTrackCategory;
  mimeType: string;
  byteSize: number;
  durationSec: number | null;
  sortOrder: number;
  licenseNote: string | null;
  status: MusicTrackStatus;
  streamUrl: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface MusicTrackListParams extends PageParams {
  status?: MusicTrackStatus;
}

export interface MusicTrackFormValues {
  title: string;
  category: MusicTrackCategory;
  mimeType?: string;
  durationSec?: number;
  sortOrder?: number;
  licenseNote?: string;
  file?: File;
}

interface SpringPage<T> {
  content?: T[];
  number?: number;
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

function normalizePage<T>(
  page: SpringPage<T>,
  fallback: PageParams,
): Page<T> {
  return {
    content: Array.isArray(page.content) ? page.content : [],
    page: page.page ?? page.number ?? fallback.page ?? 0,
    size: page.size ?? fallback.size ?? 20,
    totalElements: page.totalElements ?? 0,
    totalPages: page.totalPages ?? 0,
  };
}

function appendIfPresent(formData: FormData, key: string, value: unknown) {
  if (value === undefined || value === null || value === '') {
    return;
  }
  formData.append(key, String(value));
}

function toFormData(values: MusicTrackFormValues, requireFile: boolean) {
  const formData = new FormData();
  formData.append('title', values.title);
  formData.append('category', values.category);
  appendIfPresent(formData, 'mimeType', values.mimeType);
  appendIfPresent(formData, 'durationSec', values.durationSec);
  appendIfPresent(formData, 'sortOrder', values.sortOrder);
  appendIfPresent(formData, 'licenseNote', values.licenseNote);
  if (values.file) {
    formData.append('file', values.file);
  } else if (requireFile) {
    throw new Error('음원 파일을 선택하세요.');
  }
  return formData;
}

export async function listMusicTracks(params: MusicTrackListParams = {}) {
  const page = await unwrap<SpringPage<MusicTrack>>(
    apiClient.get<ApiResponse<SpringPage<MusicTrack>>>('/admin/music-tracks', {
      params,
    }),
  );
  return normalizePage(page, params);
}

export function createMusicTrack(values: MusicTrackFormValues) {
  return unwrap<MusicTrack>(
    apiClient.post<ApiResponse<MusicTrack>>(
      '/admin/music-tracks',
      toFormData(values, true),
    ),
  );
}

export function updateMusicTrack(id: number, values: MusicTrackFormValues) {
  return unwrap<MusicTrack>(
    apiClient.patch<ApiResponse<MusicTrack>>(
      `/admin/music-tracks/${id}`,
      toFormData(values, false),
    ),
  );
}

export function publishMusicTrack(id: number) {
  return unwrap<MusicTrack>(
    apiClient.post<ApiResponse<MusicTrack>>(`/admin/music-tracks/${id}/publish`),
  );
}

export function hideMusicTrack(id: number) {
  return unwrap<MusicTrack>(
    apiClient.post<ApiResponse<MusicTrack>>(`/admin/music-tracks/${id}/hide`),
  );
}
