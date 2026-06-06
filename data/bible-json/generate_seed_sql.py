"""
KorRV.json + KJV.json → V21__seed_bible_verses.sql 변환 스크립트.

bible_books의 book_id(1~66)와 JSON의 books[] 인덱스가 1:1 대응.
korean_text = KorRV, english_text = KJV.
"""
import json
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(SCRIPT_DIR, '..', '..', 'qtai-server', 'src', 'main', 'resources', 'db', 'migration')

def escape_sql(text):
    if text is None:
        return 'NULL'
    return "'" + text.replace("\\", "\\\\").replace("'", "''").strip() + "'"

def main():
    with open(os.path.join(SCRIPT_DIR, 'KorRV.json'), 'r', encoding='utf-8') as f:
        kor_data = json.load(f)

    with open(os.path.join(SCRIPT_DIR, 'KJV.json'), 'r', encoding='utf-8') as f:
        kjv_data = json.load(f)

    kor_books = kor_data['books']
    kjv_books = kjv_data['books']

    assert len(kor_books) == 66, f"KorRV books count: {len(kor_books)}"
    assert len(kjv_books) == 66, f"KJV books count: {len(kjv_books)}"

    output_path = os.path.join(OUTPUT_DIR, 'V21__seed_bible_verses.sql')
    total_verses = 0

    with open(output_path, 'w', encoding='utf-8') as out:
        out.write("-- V21__seed_bible_verses.sql\n")
        out.write("-- 성경 66권 전체 본문 (KorRV 한글 + KJV 영어)\n")
        out.write("-- 자동 생성: generate_seed_sql.py\n")
        out.write("-- 저작권: KorRV(개역한글, 퍼블릭 도메인), KJV(King James Version, 퍼블릭 도메인)\n\n")

        for book_idx in range(66):
            book_id = book_idx + 1
            kor_book = kor_books[book_idx]
            kjv_book = kjv_books[book_idx]

            out.write(f"-- book_id={book_id}: {kor_book['name']}\n")

            kor_chapters = kor_book['chapters']
            kjv_chapters = kjv_book['chapters']

            for ch_idx in range(len(kor_chapters)):
                kor_ch = kor_chapters[ch_idx]
                kjv_ch = kjv_chapters[ch_idx] if ch_idx < len(kjv_chapters) else None
                chapter_no = kor_ch['chapter']

                kor_verses = kor_ch['verses']
                kjv_verses = kjv_ch['verses'] if kjv_ch else []

                # KJV verse를 dict로 변환
                kjv_map = {v['verse']: v['text'] for v in kjv_verses}

                values = []
                for v in kor_verses:
                    verse_no = v['verse']
                    korean_text = escape_sql(v['text'])
                    english_text = escape_sql(kjv_map.get(verse_no))
                    values.append(f"({book_id}, {chapter_no}, {verse_no}, {korean_text}, {english_text})")
                    total_verses += 1

                if values:
                    out.write(f"INSERT INTO bible_verses (book_id, chapter_no, verse_no, korean_text, english_text) VALUES\n")
                    out.write(",\n".join(values))
                    out.write(";\n\n")

    print(f"Generated: {output_path}")
    print(f"Total verses: {total_verses}")

if __name__ == '__main__':
    main()
