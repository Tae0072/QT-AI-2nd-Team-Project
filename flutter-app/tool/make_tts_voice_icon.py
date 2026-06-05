# -*- coding: utf-8 -*-
"""TTS 아이콘 v2: 작은 크기 가독성 개선 — 흰 간격 확대, 얼굴 슬림화, 선 두께 정리."""
import math
from PIL import Image, ImageDraw

S = 1000
CX, CY = 500, 500
R_OUT = 480
RING_W = 52
BLACK = (0, 0, 0, 255)

img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# 1) 바깥 링 (얇게)
d.ellipse([CX - R_OUT, CY - R_OUT, CX + R_OUT, CY + R_OUT],
          outline=BLACK, width=RING_W)

def circle_pt(deg, r=R_OUT - 8):
    a = math.radians(deg)
    return (CX + r * math.cos(a), CY + r * math.sin(a))

# 2) 얼굴 실루엣 — 왼쪽으로 슬림하게 (오른쪽 여백 확보)
ctrl = [
    circle_pt(285),
    (480, 90),
    (520, 190),
    (505, 270),
    (565, 360),   # 코끝
    (515, 415),
    (548, 462),   # 윗입술
    (512, 505),   # 입
    (542, 552),   # 아랫입술
    (505, 620),   # 턱
    (455, 690),
    (395, 800),
    circle_pt(105),
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

rim = [circle_pt(a) for a in range(105, 286, 4)]
d.polygon(catmull_rom(ctrl) + rim, fill=BLACK)

# 3) 음파 2줄 — 간격을 크게, 선은 적당히 두껍게
M = (505, 500)
def arc(r, width, sweep=46):
    bbox = [M[0] - r, M[1] - r, M[0] + r, M[1] + r]
    d.arc(bbox, start=-sweep, end=sweep, fill=BLACK, width=width)

arc(170, 62)   # 안쪽 파동: 142~198  (입~파동1 간격 ~90)
arc(330, 62)   # 바깥 파동: 299~361  (파동1~2 간격 ~100, 링 내선 428과 간격 ~67)

img = img.resize((512, 512), Image.LANCZOS)
img.save("/sessions/vibrant-friendly-galileo/mnt/outputs/tts_voice_icon2.png")

# 크기별 미리보기 (앱바 배경색)
canvas = Image.new("RGBA", (320, 90), (245, 237, 227, 255))
for i, sz in enumerate([64, 48, 32, 24]):
    s = img.resize((sz, sz), Image.LANCZOS)
    canvas.alpha_composite(s, (20 + i * 75, (90 - sz) // 2))
canvas.convert("RGB").save("/sessions/vibrant-friendly-galileo/mnt/outputs/icon2_sizes_preview.png")
print("done")
