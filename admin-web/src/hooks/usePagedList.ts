import { useCallback, useEffect, useState } from 'react';
import { message } from 'antd';
import type { Page } from '../api/types';

// ===== 목록(서버 페이지네이션) 공통 훅 =====
// 관리자 화면(AD-07 감사 로그, AD-04 신고 처리 등)이 공통으로 쓰는 목록 상태 관리.
// - fetcher : 파라미터를 받아 Page<T>를 돌려주는 API 함수
//   (모듈 레벨 함수처럼 identity가 안정적인 함수를 넘겨야 한다. 매 렌더 새로 만든 함수면 무한 호출됨)
// - 로딩/에러/데이터/페이지 상태와 필터 변경·페이지 이동·새로고침을 한곳에서 제공한다.
export interface PagedListState<T, P> {
  rows: T[];
  page: number; // 0-based (서버 기준)
  size: number;
  total: number;
  loading: boolean;
  error: string | null;
  params: P;
  /** 필터 변경 — 0페이지로 리셋 후 조회 */
  applyFilters: (next: Partial<P>) => void;
  /** 페이지/페이지크기 변경 */
  changePage: (page: number, size?: number) => void;
  /** 현재 조건으로 다시 조회 */
  reload: () => void;
}

export function usePagedList<T, P extends { page?: number; size?: number }>(
  fetcher: (params: P) => Promise<Page<T>>,
  initialParams: P,
): PagedListState<T, P> {
  const [params, setParams] = useState<P>({ page: 0, size: 20, ...initialParams });
  const [rows, setRows] = useState<T[]>([]);
  const [page, setPage] = useState(initialParams.page ?? 0);
  const [size, setSize] = useState(initialParams.size ?? 20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetcher(params)
      .then((res) => {
        if (cancelled) return;
        setRows(res.content);
        setPage(res.page);
        setSize(res.size);
        setTotal(res.totalElements);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const msg = e instanceof Error ? e.message : '목록을 불러오지 못했습니다.';
        setError(msg);
        message.error(msg);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [fetcher, params, reloadKey]);

  const applyFilters = useCallback((next: Partial<P>) => {
    setParams((prev) => ({ ...prev, ...next, page: 0 }));
  }, []);

  const changePage = useCallback((nextPage: number, nextSize?: number) => {
    setParams((prev) => ({ ...prev, page: nextPage, size: nextSize ?? prev.size }));
  }, []);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  return { rows, page, size, total, loading, error, params, applyFilters, changePage, reload };
}
