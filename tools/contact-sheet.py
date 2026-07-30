#!/usr/bin/env python3
"""Composite the creature contact sheet: 8 bodies x 4 states on three backdrops.

The v1.1 run built these sheets with an ad-hoc script that never entered the
repository, so the sheets could be looked at but not reproduced. This is that
script, kept, so the next run can re-shoot the same comparison after touching a
renderer instead of inventing a new one.

Input:  the raw transparent StillRender frames that ContactSheetDumpTest writes.
          gradlew :core:creature:testDebugUnitTest --tests '*ContactSheetDumpTest*'
          -> core/creature/build/contact/<concept>-<state>.png
Output: three sheets in the directory given on the command line.

    python3 tools/contact-sheet.py docs/design/v11/contact

The point of the exercise is the third backdrop. "Busy photo" is a generated
blob field, not a photograph — a deliberately hard case rather than a realistic
one. It stays synthetic on purpose: it is reproducible, and a body that survives
it survives a real wallpaper.
"""
import os
import sys
import random
from PIL import Image, ImageDraw, ImageFilter

CONCEPTS = [
    "spirit_orb",
    "fox_kit",
    "jelly",
    "pixel_pet",
    "robot",
    "sprout",
    "ember",
    "moth",
]
STATES = ["alert-day", "eating-charge", "sleepy-low", "asleep-night"]

CELL = 256
LABEL_W = 128
HEADER_H = 28


def light_wallpaper(w, h):
    """Warm paper with a faint vertical weave — the worst case for pale bodies."""
    img = Image.new("RGB", (w, h), (238, 235, 228))
    d = ImageDraw.Draw(img)
    for x in range(0, w, 3):
        d.line([(x, 0), (x, h)], fill=(233, 230, 223))
    for i in range(6):
        cx, cy = w * (0.2 + 0.13 * i), h * (0.1 + 0.14 * i)
        r = min(w, h) * 0.18
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(243, 241, 236))
    return img.filter(ImageFilter.GaussianBlur(6))


def dark_wallpaper(w, h):
    """Near-black with a cool cast — the case every glow already passes."""
    img = Image.new("RGB", (w, h), (14, 16, 24))
    d = ImageDraw.Draw(img)
    for x in range(0, w, 5):
        d.line([(x, 0), (x, h)], fill=(18, 20, 30))
    for i in range(5):
        cx, cy = w * (0.15 + 0.17 * i), h * (0.12 + 0.15 * i)
        r = min(w, h) * 0.22
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(20, 24, 38))
    return img.filter(ImageFilter.GaussianBlur(8))


def busy_photo(w, h):
    """Saturated overlapping blobs on black: bright and dark, everywhere."""
    rng = random.Random(20260730)
    img = Image.new("RGB", (w, h), (0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(260):
        cx, cy = rng.uniform(0, w), rng.uniform(0, h)
        r = rng.uniform(min(w, h) * 0.02, min(w, h) * 0.09)
        col = (rng.randint(30, 255), rng.randint(30, 255), rng.randint(30, 255))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    return img.filter(ImageFilter.GaussianBlur(10))


BACKDROPS = [
    ("light-wallpaper", "LIGHT WALLPAPER", light_wallpaper, (40, 40, 40), (250, 250, 250)),
    ("dark-wallpaper", "DARK WALLPAPER", dark_wallpaper, (235, 235, 235), (10, 10, 14)),
    ("busy-photo", "BUSY PHOTO", busy_photo, (245, 245, 245), (25, 25, 30)),
]


def build(src_dir, out_dir):
    w = LABEL_W + CELL * len(STATES)
    h = HEADER_H + CELL * len(CONCEPTS)
    os.makedirs(out_dir, exist_ok=True)

    for slug, title, backdrop, text_rgb, gutter_rgb in BACKDROPS:
        sheet = Image.new("RGB", (w, h), gutter_rgb)
        # The backdrop covers only the cells, so the label column stays legible.
        sheet.paste(backdrop(CELL * len(STATES), CELL * len(CONCEPTS)), (LABEL_W, HEADER_H))
        d = ImageDraw.Draw(sheet)
        d.rectangle([0, 0, w, HEADER_H], fill=gutter_rgb)
        d.rectangle([0, 0, LABEL_W, h], fill=gutter_rgb)
        d.text((6, 8), title, fill=text_rgb)
        for col, state in enumerate(STATES):
            d.text((LABEL_W + col * CELL + 6, 8), state, fill=text_rgb)
        for row, concept in enumerate(CONCEPTS):
            d.text((6, HEADER_H + row * CELL + CELL // 2), concept, fill=text_rgb)
            for col, state in enumerate(STATES):
                path = os.path.join(src_dir, f"{concept}-{state}.png")
                if not os.path.exists(path):
                    d.text(
                        (LABEL_W + col * CELL + 6, HEADER_H + row * CELL + 6),
                        "MISSING",
                        fill=(255, 0, 0),
                    )
                    continue
                frame = Image.open(path).convert("RGBA").resize((CELL, CELL), Image.LANCZOS)
                sheet.paste(frame, (LABEL_W + col * CELL, HEADER_H + row * CELL), frame)
        out = os.path.join(out_dir, f"contact-{slug}.png")
        sheet.save(out)
        print("wrote", out)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "docs/design/v11/contact"
    src = sys.argv[2] if len(sys.argv) > 2 else "core/creature/build/contact"
    if not os.path.isdir(src):
        sys.exit(f"no frames in {src} — run ContactSheetDumpTest first")
    build(src, out)
