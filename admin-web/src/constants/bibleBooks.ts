// 성경 66권 — book_id(1~66) ↔ 한글 권 이름.
// 백엔드 bible_books seed(V7)와 동일. 관리자 QT 등록/목록에서 ID 대신 한글로 보여주기 위함.

export interface BibleBook {
  id: number;
  korean: string;
}

export const BIBLE_BOOKS: BibleBook[] = [
  { id: 1, korean: '창세기' },
  { id: 2, korean: '출애굽기' },
  { id: 3, korean: '레위기' },
  { id: 4, korean: '민수기' },
  { id: 5, korean: '신명기' },
  { id: 6, korean: '여호수아' },
  { id: 7, korean: '사사기' },
  { id: 8, korean: '룻기' },
  { id: 9, korean: '사무엘상' },
  { id: 10, korean: '사무엘하' },
  { id: 11, korean: '열왕기상' },
  { id: 12, korean: '열왕기하' },
  { id: 13, korean: '역대상' },
  { id: 14, korean: '역대하' },
  { id: 15, korean: '에스라' },
  { id: 16, korean: '느헤미야' },
  { id: 17, korean: '에스더' },
  { id: 18, korean: '욥기' },
  { id: 19, korean: '시편' },
  { id: 20, korean: '잠언' },
  { id: 21, korean: '전도서' },
  { id: 22, korean: '아가' },
  { id: 23, korean: '이사야' },
  { id: 24, korean: '예레미야' },
  { id: 25, korean: '예레미야애가' },
  { id: 26, korean: '에스겔' },
  { id: 27, korean: '다니엘' },
  { id: 28, korean: '호세아' },
  { id: 29, korean: '요엘' },
  { id: 30, korean: '아모스' },
  { id: 31, korean: '오바댜' },
  { id: 32, korean: '요나' },
  { id: 33, korean: '미가' },
  { id: 34, korean: '나훔' },
  { id: 35, korean: '하박국' },
  { id: 36, korean: '스바냐' },
  { id: 37, korean: '학개' },
  { id: 38, korean: '스가랴' },
  { id: 39, korean: '말라기' },
  { id: 40, korean: '마태복음' },
  { id: 41, korean: '마가복음' },
  { id: 42, korean: '누가복음' },
  { id: 43, korean: '요한복음' },
  { id: 44, korean: '사도행전' },
  { id: 45, korean: '로마서' },
  { id: 46, korean: '고린도전서' },
  { id: 47, korean: '고린도후서' },
  { id: 48, korean: '갈라디아서' },
  { id: 49, korean: '에베소서' },
  { id: 50, korean: '빌립보서' },
  { id: 51, korean: '골로새서' },
  { id: 52, korean: '데살로니가전서' },
  { id: 53, korean: '데살로니가후서' },
  { id: 54, korean: '디모데전서' },
  { id: 55, korean: '디모데후서' },
  { id: 56, korean: '디도서' },
  { id: 57, korean: '빌레몬서' },
  { id: 58, korean: '히브리서' },
  { id: 59, korean: '야고보서' },
  { id: 60, korean: '베드로전서' },
  { id: 61, korean: '베드로후서' },
  { id: 62, korean: '요한일서' },
  { id: 63, korean: '요한이서' },
  { id: 64, korean: '요한삼서' },
  { id: 65, korean: '유다서' },
  { id: 66, korean: '요한계시록' },
];

const BIBLE_BOOK_NAME: Record<number, string> = Object.fromEntries(
  BIBLE_BOOKS.map((b) => [b.id, b.korean]),
);

/** book_id를 한글 권 이름으로. 미상이면 `#id`로 폴백. */
export function bibleBookName(id: number | null | undefined): string {
  if (id == null) return '';
  return BIBLE_BOOK_NAME[id] ?? `#${id}`;
}
