# -*- coding: utf-8 -*-
"""TTS 아이콘 v3: 🗣 이모지 스타일 — 원 없이 말하는 머리 실루엣 + 음파."""
import math
from PIL import Image, ImageDraw

S = 1000
BLACK = (0, 0, 0, 255)

img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# 말하는 머리 실루엣 (오른쪽을 보는 옆모습, 입 벌림)
ctrl = [
    (330, 900),   # 목 왼쪽 아래
    (300, 700),   # 목덜미
    (195, 520),   # 뒤통수
    (235, 290),   # 뒤통수 위
    (420, 130),   # 정수리
    (590, 175),   # 앞머리
    (650, 280),   # 이마
    (640, 350),   # 미간
    (715, 445),   # 코끝
    (650, 490),   # 코밑
    (700, 525),   # 윗입술
    (620, 560),   # 입 안쪽 (벌린 입)
    (690, 600),   # 아랫입술
    (640, 680),   # 턱
    (560, 740),   # 턱선
    (520, 800),   # 목 앞
    (520, 900),   # 목 오른쪽 아래
]

def catmull_rom(pts, seg=18):
    ext = [pts[0]] + pts + [pts[-1]]
    out = []
    for i in range(1, len(ext) - 2):
        p0, p1, p2, p3 = ext[i - 1], ext[i], ext[i + 1], ext[i + 2]
        for t_i in range(seg):
            t = t_i / seg
            t2, t3 = t * t, t * t * t
            x = 0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t
                       + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                       + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3)
            y = 0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t
                       + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                       + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
            out.append((x, y))
    out.append(pts[-1])
    return out

d.polygon(catmull_rom(ctrl), fill=BLACK)

# 음파 3줄 — 입에서 퍼져나가는 호 (🗣 스타일)
M = (660, 555)  # 입 중심
def arc(r, width, sweep=38):
    bbox = [M[0] - r, M[1] - r, M[0] + r, M[1] + r]
    d.arc(bbox, start=-sweep, end=sweep, fill=BLACK, width=width)

arc(160, 52)
arc(255, 52)
arc(350, 52)

img = img.resize((512, 512), Image.LANCZOS)
img.save("/sessions/vibrant-friendly-galileo/mnt/outputs/tts_voice_icon3.png")

# 크기별 미리보기
canvas = Image.new("RGBA", (320, 90), (245, 237, 227, 255))
for i, sz in enumerate([64, 48, 32, 24]):
    s = img.resize((sz, sz), Image.LANCZOS)
    canvas.alpha_composite(s, (20 + i * 75, (90 - sz) // 2))
canvas.convert("RGB").save("/sessions/vibrant-friendly-galileo/mnt/outputs/icon3_sizes_preview.png")

prev = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
prev.alpha_composite(Image.open("/sessions/vibrant-friendly-galileo/mnt/outputs/tts_voice_icon3.png"))
prev.convert("RGB").save("/sessions/vibrant-friendly-galileo/mnt/outputs/icon3_full_preview.png")
print("done")
