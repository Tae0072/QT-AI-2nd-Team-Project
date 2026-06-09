// ISO datetime 문자열을 'YYYY-MM-DD HH:mm:ss'(로컬)로 표시한다. 없으면 '-'.
// 관리자 화면 목록의 시각 컬럼 공통 포맷.
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '-';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
