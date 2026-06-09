import { useCallback, useEffect, useState } from 'react';
import { App } from 'antd';
import type { Page, PageParams } from '../api/types';

// ===== 목록 화면 공통 상태 훅 =====
// 모든 관리자 목록 화면(AD-04/07 등)이 재사용한다.
// - fetcher(params)를 호출해 Page<T>를 받아 상태로 관리한다.
// - params(페이지/필터)가 바뀌면 자동으로 다시 조회한다.
// - 실패하면 antd 토스트(message.error)로 안내한다.
//
// 주의: fetcher 는 모듈 레벨 api 함수(예: listAuditLogs)를 그대로 넘긴다.
//   인라인 화살표 함수를 넘기면 매 렌더마다 참조가 바뀌어 재조회 루프가 생긴다.

export interface UseListResult<T, P> {
  data: Page<T> | null;
  rows: T[];
  loading: boolean;
  params: P;
  setParams: (patch: Partial<P>) => void; // 일부 필드만 바꿔도 나머지는 유지
  reload: () => void;
}

export function useList<T, P extends PageParams = PageParams>(
  fetcher: (params: P) => Promise<Page<T>>,
  initialParams: P,
): UseListResult<T, P> {
  const { message } = App.useApp();
  const [data, setData] = useState<Page<T> | null>(null);
  const [loading, setLoading] = useState(false);
  const [params, setParamsState] = useState<P>(initialParams);

  const load = useCallback(
    async (p: P) => {
      setLoading(true);
      try {
        setData(await fetcher(p));
      } catch (e) {
        message.error(e instanceof Error ? e.message : '목록을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    },
    [fetcher, message],
  );

  // params 가 바뀔 때마다 재조회(최초 1회 포함).
  useEffect(() => {
    load(params);
  }, [load, params]);

  const setParams = useCallback((patch: Partial<P>) => {
    setParamsState((prev) => ({ ...prev, ...patch }));
  }, []);

  const reload = useCallback(() => {
    load(params);
  }, [load, params]);

  return {
    data,
    rows: data?.content ?? [],
    loading,
    params,
    setParams,
    reload,
  };
}
