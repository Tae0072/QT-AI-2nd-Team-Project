#!/usr/bin/env python3
"""Generate Aquifer Open Study Notes commentary seed SQL."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import tempfile
import urllib.request
import zipfile
from collections import Counter, defaultdict
from dataclasses import dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import TypeVar


RELEASE_TAG = "v2026-06-09"
RELEASE_URL = (
    "https://github.com/BibleAquifer/AquiferOpenStudyNotes/releases/download/"
    f"{RELEASE_TAG}/English.zip"
)
EXPECTED_SHA256 = "14b5df7b81e8f2cdeb7202430beb9d629771d0ac520badea06cacfa9383201b3"
SOURCE_KEY = "AQUIFER_OPEN_STUDY_NOTES"
SOURCE_NAME = "Aquifer Open Study Notes"
SOURCE_LABEL = "Aquifer Open Study Notes (Tyndale)"
PRODUCT = "Tyndale Open Study Notes"
LICENSE_LABEL = "CC BY-SA 4.0"
COPYRIGHT_NOTICE = "Tyndale Open Study Notes \u00a9 2019 Tyndale House Publishers"
ATTRIBUTION = (
    "Aquifer Open Study Notes is adapted by Mission Mutual from "
    "Tyndale Open Study Notes and is licensed under CC BY-SA 4.0."
)

FORBIDDEN_PATTERNS = [
    re.compile(r"\bESV\b", re.IGNORECASE),
    re.compile(r"\bNIV\b", re.IGNORECASE),
    re.compile(r"English Standard Version", re.IGNORECASE),
    re.compile(r"New International Version", re.IGNORECASE),
    re.compile(r"개역개정"),
    re.compile(r"성서유니온"),
    re.compile(r"두란노"),
]

REF_PATTERN = re.compile(r"^([1-3]?[A-Z]{2,3}) (\d+):(\d+)$")
BOOK_LINE_PATTERN = re.compile(r"^\((\d+),\s*'[^']+',\s*'([^']+)'")
VERSE_LINE_PATTERN = re.compile(r"^\((\d+),\s*(\d+),\s*(\d+),")

MATERIAL_BATCH_SIZE = 100
MAPPING_BATCH_SIZE = 500
T = TypeVar("T")


@dataclass(frozen=True)
class Ref:
    book_code: str
    chapter: int
    verse: int


@dataclass(frozen=True)
class Passage:
    start: Ref
    end: Ref
    label: str


@dataclass
class MaterialSeed:
    external_id: str
    material_type: str
    refs: str
    book_code: str
    chapter_start: int
    verse_start: int
    chapter_end: int
    verse_end: int
    title: str
    keywords_json: str
    content_text: str
    content_html: str
    content_hash: str
    mapping_refs: list[Ref]


class TextExtractor(HTMLParser):
    BLOCK_TAGS = {
        "blockquote",
        "br",
        "dd",
        "div",
        "dt",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "li",
        "ol",
        "p",
        "section",
        "table",
        "td",
        "th",
        "tr",
        "ul",
    }

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() in self.BLOCK_TAGS:
            self.parts.append(" ")

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() in self.BLOCK_TAGS:
            self.parts.append(" ")

    def handle_data(self, data: str) -> None:
        self.parts.append(data)

    def text(self) -> str:
        return re.sub(r"\s+", " ", "".join(self.parts)).strip()


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def migration_path() -> Path:
    return repo_root() / "qtai-server/admin-server/src/main/resources/db/migration/V34__seed_aquifer_open_study_notes.sql"


def cache_path() -> Path:
    return Path(tempfile.gettempdir()) / "aquifer-open-study-notes-v2026-06-09" / "English.zip"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download_release() -> Path:
    path = cache_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and sha256_file(path) == EXPECTED_SHA256:
        return path
    if path.exists():
        path.unlink()
    with urllib.request.urlopen(RELEASE_URL, timeout=60) as response:
        path.write_bytes(response.read())
    actual_hash = sha256_file(path)
    if actual_hash != EXPECTED_SHA256:
        raise ValueError(f"Unexpected SHA-256: {actual_hash}")
    return path


def sql_string(value: str | None) -> str:
    if value is None:
        return "NULL"
    escaped = (
        value.replace("\\", "\\\\")
        .replace("'", "''")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
    )
    return f"'{escaped}'"


def sql_json(value: str) -> str:
    return f"CAST({sql_string(value)} AS JSON)"


def parse_ref(raw: str) -> Ref:
    match = REF_PATTERN.match(raw)
    if not match:
        raise ValueError(f"Invalid USFM reference: {raw}")
    return Ref(match.group(1), int(match.group(2)), int(match.group(3)))


def parse_books() -> dict[int, str]:
    path = repo_root() / "qtai-server/admin-server/src/main/resources/db/migration/V7__seed_bible_books.sql"
    books: dict[int, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        match = BOOK_LINE_PATTERN.match(line.strip())
        if match:
            books[int(match.group(1))] = match.group(2)
    if len(books) != 66:
        raise ValueError(f"Expected 66 bible_books rows, found {len(books)}")
    return books


def parse_verse_index(books: dict[int, str]) -> dict[str, set[tuple[int, int]]]:
    path = repo_root() / "qtai-server/admin-server/src/main/resources/db/migration/V23__seed_bible_verses.sql"
    verses: dict[str, set[tuple[int, int]]] = defaultdict(set)
    for line in path.read_text(encoding="utf-8").splitlines():
        match = VERSE_LINE_PATTERN.match(line.strip())
        if not match:
            continue
        book_id = int(match.group(1))
        book_code = books.get(book_id)
        if book_code:
            verses[book_code].add((int(match.group(2)), int(match.group(3))))
    if len(verses) != 66:
        raise ValueError(f"Expected verse rows for 66 books, found {len(verses)}")
    return verses


def refs_for_range(passage: Passage, verse_index: dict[str, set[tuple[int, int]]]) -> list[Ref]:
    if passage.start.book_code != passage.end.book_code:
        raise ValueError(f"Cross-book range is not supported: {passage.label}")
    book_verses = verse_index.get(passage.start.book_code)
    if not book_verses:
        raise ValueError(f"Unknown book code: {passage.start.book_code}")
    start_key = (passage.start.chapter, passage.start.verse)
    end_key = (passage.end.chapter, passage.end.verse)
    if start_key not in book_verses:
        raise ValueError(f"Unknown start verse: {passage.label}")
    if end_key not in book_verses:
        raise ValueError(f"Unknown end verse: {passage.label}")
    expanded = [
        Ref(passage.start.book_code, chapter, verse)
        for chapter, verse in sorted(book_verses)
        if start_key <= (chapter, verse) <= end_key
    ]
    if not expanded:
        raise ValueError(f"Empty verse range: {passage.label}")
    return expanded


def passage_label(start: Ref, end: Ref) -> str:
    start_label = f"{start.book_code} {start.chapter}:{start.verse}"
    end_label = f"{end.book_code} {end.chapter}:{end.verse}"
    return start_label if start == end else f"{start_label}-{end_label}"


def html_to_text(content_html: str) -> str:
    parser = TextExtractor()
    parser.feed(content_html)
    parser.close()
    return parser.text()


def validate_metadata(metadata: dict) -> None:
    resource = metadata.get("resource_metadata") or {}
    license_info = resource.get("license_info") or {}
    licenses = license_info.get("licenses") or []
    license_name = ""
    if licenses:
        license_name = ((licenses[0] or {}).get("eng") or {}).get("name", "")

    expected = {
        "title": SOURCE_NAME,
        "version": "1.1.2",
        "date_created": "2026-06-09",
        "language": "eng",
    }
    for key, value in expected.items():
        if resource.get(key) != value:
            raise ValueError(f"Unexpected metadata {key}: {resource.get(key)!r}")
    if license_info.get("title") != PRODUCT:
        raise ValueError(f"Unexpected license title: {license_info.get('title')!r}")
    if not license_name.startswith("CC BY-SA 4.0"):
        raise ValueError(f"Unexpected license name: {license_name!r}")


def scan_forbidden(value: str, counter: Counter[str]) -> None:
    for pattern in FORBIDDEN_PATTERNS:
        if pattern.search(value):
            counter[pattern.pattern] += 1


def load_materials(zip_path: Path, verse_index: dict[str, set[tuple[int, int]]]) -> tuple[list[MaterialSeed], Counter[str]]:
    forbidden_matches: Counter[str] = Counter()
    materials: list[MaterialSeed] = []
    seen_external_ids: set[str] = set()

    with zipfile.ZipFile(zip_path) as archive:
        validate_metadata(json.loads(archive.read("metadata.json")))
        json_files = sorted(
            name
            for name in archive.namelist()
            if name.startswith("json/") and name.endswith(".content.json")
        )
        if len(json_files) != 66:
            raise ValueError(f"Expected 66 json content files, found {len(json_files)}")

        for content_file in json_files:
            items = json.loads(archive.read(content_file))
            if not isinstance(items, list):
                raise ValueError(f"Expected list in {content_file}")
            for item in items:
                external_id = f"aquifer-eng-{item['content_id']}"
                if external_id in seen_external_ids:
                    raise ValueError(f"Duplicate external_id: {external_id}")
                seen_external_ids.add(external_id)

                if item.get("language") != "eng":
                    raise ValueError(f"Unexpected language for {external_id}: {item.get('language')}")
                if item.get("media_type") != "Text":
                    raise ValueError(f"Unexpected media_type for {external_id}: {item.get('media_type')}")

                raw_passages = ((item.get("associations") or {}).get("passage") or [])
                if not raw_passages:
                    raise ValueError(f"Missing passage for {external_id}")

                passages: list[Passage] = []
                mapping_refs: list[Ref] = []
                seen_mapping_refs: set[Ref] = set()
                for raw_passage in raw_passages:
                    start = parse_ref(raw_passage["start_ref_usfm"])
                    end = parse_ref(raw_passage["end_ref_usfm"])
                    passage = Passage(start, end, passage_label(start, end))
                    passages.append(passage)
                    for ref in refs_for_range(passage, verse_index):
                        if ref not in seen_mapping_refs:
                            mapping_refs.append(ref)
                            seen_mapping_refs.add(ref)

                content_html = item.get("content") or ""
                content_text = html_to_text(content_html)
                if not content_text:
                    raise ValueError(f"Empty text content for {external_id}")
                scan_forbidden(content_html, forbidden_matches)
                scan_forbidden(content_text, forbidden_matches)

                first_passage = passages[0]
                refs = ", ".join(passage.label for passage in passages)
                if len(refs) > 100:
                    raise ValueError(f"refs too long for {external_id}: {refs}")
                title = item.get("title") or refs
                if len(title) > 200:
                    raise ValueError(f"title too long for {external_id}: {title}")

                keywords = {
                    "releaseTag": RELEASE_TAG,
                    "sourceVersion": "1.1.2",
                    "itemVersion": item.get("version"),
                    "reviewLevel": item.get("review_level"),
                    "referenceId": item.get("reference_id"),
                    "indexReference": item.get("index_reference"),
                    "mediaType": item.get("media_type"),
                    "contentFile": content_file,
                    "passageCount": len(passages),
                }

                materials.append(
                    MaterialSeed(
                        external_id=external_id,
                        material_type="study_note",
                        refs=refs,
                        book_code=first_passage.start.book_code,
                        chapter_start=first_passage.start.chapter,
                        verse_start=first_passage.start.verse,
                        chapter_end=first_passage.end.chapter,
                        verse_end=first_passage.end.verse,
                        title=title,
                        keywords_json=json.dumps(keywords, ensure_ascii=False, separators=(",", ":")),
                        content_text=content_text,
                        content_html=content_html,
                        content_hash=hashlib.sha256(content_html.encode("utf-8")).hexdigest(),
                        mapping_refs=mapping_refs,
                    )
                )

    if forbidden_matches:
        raise ValueError(f"Forbidden source keywords detected: {dict(forbidden_matches)}")
    return materials, forbidden_matches


def source_insert_sql() -> str:
    columns = [
        "source_key",
        "name",
        "source_label",
        "product",
        "language",
        "usage_type",
        "license_label",
        "copyright_notice",
        "attribution",
        "access_level",
        "status",
    ]
    values = [
        SOURCE_KEY,
        SOURCE_NAME,
        SOURCE_LABEL,
        PRODUCT,
        "eng",
        "GENERATION_INPUT",
        LICENSE_LABEL,
        COPYRIGHT_NOTICE,
        ATTRIBUTION,
        "INTERNAL",
        "ACTIVE",
    ]
    return (
        f"INSERT INTO commentary_sources ({', '.join(columns)}) VALUES\n"
        f"({', '.join(sql_string(value) for value in values)});\n"
    )


def material_row_sql(material: MaterialSeed) -> str:
    values = [
        "@aquifer_source_id",
        sql_string(material.external_id),
        sql_string(material.material_type),
        sql_string(material.refs),
        sql_string(material.book_code),
        str(material.chapter_start),
        str(material.verse_start),
        str(material.chapter_end),
        str(material.verse_end),
        sql_string(material.title),
        sql_json(material.keywords_json),
        sql_string(material.content_text),
        sql_string(material.content_html),
        sql_string(material.content_hash),
        sql_string("ACTIVE"),
    ]
    return f"({', '.join(values)})"


def mapping_seed_rows(materials: list[MaterialSeed]) -> list[tuple[str, Ref, int]]:
    rows: list[tuple[str, Ref, int]] = []
    order_by_verse: defaultdict[Ref, int] = defaultdict(int)
    for material in materials:
        for ref in material.mapping_refs:
            order_by_verse[ref] += 1
            rows.append((material.external_id, ref, order_by_verse[ref]))
    return rows


def mapping_select_sql(row: tuple[str, Ref, int], first: bool) -> str:
    external_id, ref, display_order = row
    prefix = "SELECT" if first else "UNION ALL SELECT"
    return (
        f"{prefix} {sql_string(external_id)} AS external_id, "
        f"{sql_string(ref.book_code)} AS book_code, "
        f"{ref.chapter} AS chapter_no, "
        f"{ref.verse} AS verse_no, "
        f"{display_order} AS display_order"
    )


def chunks(items: list[T], size: int) -> list[list[T]]:
    return [items[index : index + size] for index in range(0, len(items), size)]


def generate_sql(materials: list[MaterialSeed]) -> str:
    lines: list[str] = [
        "-- V34__seed_aquifer_open_study_notes.sql",
        f"-- Source: {SOURCE_NAME} {RELEASE_TAG}",
        f"-- Release URL: {RELEASE_URL}",
        f"-- SHA-256: {EXPECTED_SHA256}",
        f"-- License: {LICENSE_LABEL}",
        f"-- Copyright: {COPYRIGHT_NOTICE}",
        "-- Generated by data/aquifer-open-study-notes/generate_aquifer_commentary_seed.py",
        "",
        source_insert_sql().rstrip(),
        "",
        f"SET @aquifer_source_id := (SELECT id FROM commentary_sources WHERE source_key = {sql_string(SOURCE_KEY)});",
        "",
    ]

    material_columns = [
        "source_id",
        "external_id",
        "material_type",
        "refs",
        "book_code",
        "chapter_start",
        "verse_start",
        "chapter_end",
        "verse_end",
        "title",
        "keywords_json",
        "content_text",
        "content_html",
        "content_hash",
        "status",
    ]
    for batch in chunks(materials, MATERIAL_BATCH_SIZE):
        lines.append(f"INSERT INTO commentary_materials ({', '.join(material_columns)}) VALUES")
        lines.append(",\n".join(material_row_sql(material) for material in batch) + ";")
        lines.append("")

    mapping_rows = mapping_seed_rows(materials)
    for batch in chunks(mapping_rows, MAPPING_BATCH_SIZE):
        lines.append(
            "INSERT INTO commentary_material_verses "
            "(commentary_material_id, bible_verse_id, display_order, match_type)"
        )
        lines.append("SELECT cm.id, bv.id, seed.display_order, 'RANGE_EXPANDED'")
        lines.append("FROM (")
        lines.append("\n".join(mapping_select_sql(row, index == 0) for index, row in enumerate(batch)))
        lines.append(") seed")
        lines.append("JOIN commentary_materials cm")
        lines.append("  ON cm.source_id = @aquifer_source_id AND cm.external_id = seed.external_id")
        lines.append("JOIN bible_books bb ON bb.code = seed.book_code")
        lines.append(
            "JOIN bible_verses bv ON bv.book_id = bb.id "
            "AND bv.chapter_no = seed.chapter_no AND bv.verse_no = seed.verse_no;"
        )
        lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def run(check_only: bool) -> int:
    zip_path = download_release()
    books = parse_books()
    verse_index = parse_verse_index(books)
    materials, forbidden_matches = load_materials(zip_path, verse_index)
    sql = generate_sql(materials)
    target = migration_path()
    mapping_count = len(mapping_seed_rows(materials))
    multi_passage_count = sum(1 for material in materials if material.keywords_json.find('"passageCount":1') < 0)

    if check_only:
        if not target.exists():
            print(f"missing migration: {target}", file=sys.stderr)
            return 1
        current = target.read_text(encoding="utf-8")
        if current != sql:
            print(f"migration is not up to date: {target}", file=sys.stderr)
            return 1
        status = "checked"
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(sql, encoding="utf-8", newline="\n")
        status = "written"

    print(f"status={status}")
    print(f"source_count=1")
    print(f"material_count={len(materials)}")
    print(f"mapping_count={mapping_count}")
    print(f"multi_passage_material_count={multi_passage_count}")
    print(f"skipped_count=0")
    print(f"forbidden_keyword_matches={sum(forbidden_matches.values())}")
    print(f"sha256={EXPECTED_SHA256}")
    print(f"output={target}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate that the generated SQL is already current")
    args = parser.parse_args()
    try:
        return run(args.check)
    except Exception as exception:
        print(f"error: {exception}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
