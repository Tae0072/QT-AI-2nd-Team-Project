# -*- coding: utf-8 -*-
"""tts_api.py(Voice Studio)에 [N초] 묵음 태그 지원을 추가하는 패치 스크립트.

사용: python tool/patch_tts_silence.py [tts_api.py 경로]
기본 경로: C:/Users/G/Downloads/bible-tts/tts_api.py
"""
import re
import shutil
import sys

PATH = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\G\Downloads\bible-tts\tts_api.py"
BACKUP = PATH + ".bak_silence"

src = open(PATH, encoding="utf-8").read()
if "_split_silence_tags" in src:
    print("ALREADY_PATCHED")
    raise SystemExit(0)

shutil.copyfile(PATH, BACKUP)

# 1) import re 추가
src = src.replace("import os\nimport sys\n", "import os\nimport re\nimport sys\n", 1)

# 2) 헬퍼 함수 삽입 — /qt/read 엔드포인트 직전
helper = '''
# [N초] 묵음 태그 — 텍스트에 [1초], [2.5초]를 넣으면 해당 길이의 무음을 삽입한다.
SILENCE_TAG_RE = re.compile(r"\\[(\\d+(?:\\.\\d+)?)\\s*초\\]")


def _split_silence_tags(text):
    """[N초] 태그 기준으로 ("text", 본문) / ("silence", 초) 세그먼트로 분해."""
    segments = []
    pos = 0
    for m in SILENCE_TAG_RE.finditer(text):
        before = text[pos:m.start()].strip()
        if before:
            segments.append(("text", before))
        try:
            sec = float(m.group(1))
        except ValueError:
            sec = 0.0
        if sec > 0:
            segments.append(("silence", min(sec, 30.0)))  # 최대 30초 제한
        pos = m.end()
    tail = text[pos:].strip()
    if tail:
        segments.append(("text", tail))
    return segments


@app.post("/qt/read"'''
src = src.replace('\n@app.post("/qt/read"', helper, 1)

# 3) 텍스트 분할부 — 태그 세그먼트 → 문장 chunk + silence 마커
old_split = """    chunks = split_text_for_bark(text)
    if not chunks:"""
new_split = """    segments = _split_silence_tags(text)
    chunks = []  # ("text", 문장) 또는 ("silence", 초)
    for _kind, _val in segments:
        if _kind == "text":
            for _c in split_text_for_bark(_val):
                chunks.append(("text", _c))
        else:
            chunks.append(("silence", _val))
    if not any(_k == "text" for _k, _ in chunks):"""
assert old_split in src, "split anchor not found"
src = src.replace(old_split, new_split, 1)

# 4) 생성 루프 — silence 마커는 생성 없이 통과
old_loop = """    for i, chunk in enumerate(chunks):
        _log("""
new_loop = """    for i, (chunk_kind, chunk) in enumerate(chunks):
        if chunk_kind == "silence":
            all_audio.append(("silence", chunk))
            continue
        _log("""
assert old_loop in src, "loop anchor not found"
src = src.replace(old_loop, new_loop, 1)

# 5) 오디오 append를 ("audio", ...) 마커로 통일
src = src.replace(
    "all_audio.append(wav.squeeze(0).numpy())",
    'all_audio.append(("audio", wav.squeeze(0).numpy()))',
)
src = src.replace(
    "all_audio.append(audio_np)",
    'all_audio.append(("audio", audio_np))',
)

# 6) 합치기 블록 — silence 마커는 무음으로 변환, 텍스트 사이만 0.4초 간격
merge_re = re.compile(
    r"    combined = \[\].*?full_audio = np\.concatenate\(combined\)",
    re.DOTALL,
)
new_merge = """    combined = []
    gap = np.zeros(int(audio_sr * 0.4), dtype=np.float32)
    prev_kind = None
    for kind, item in all_audio:
        if kind == "audio":
            if prev_kind == "audio":
                combined.append(gap)
            combined.append(item.astype(np.float32))
        else:  # [N초] 묵음 태그
            combined.append(np.zeros(int(audio_sr * item), dtype=np.float32))
        prev_kind = kind
    full_audio = np.concatenate(combined)"""
assert merge_re.search(src), "merge anchor not found"
src = merge_re.sub(lambda _: new_merge, src, count=1)

open(PATH, "w", encoding="utf-8").write(src)
print("PATCHED_OK")
