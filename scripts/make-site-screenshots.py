#!/usr/bin/env python3
"""紹介ページ（site/）用のスクリーンショットを作る（BL-227）。

docs/store/images/ のストア用画像（1080x1920）から、上下の単色の余白を切り落とし、Web 向けに幅 540px へ縮小して
site/assets/screenshots/ へ WebP で置く。ストア用画像を作り直したら、このスクリプトを流し直す。

使い方  python scripts/make-site-screenshots.py
依存は Pillow（WebP 書き出しに対応した版）。
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "docs" / "store" / "images"
DST = ROOT / "site" / "assets" / "screenshots"
WIDTH = 540
QUALITY = 80
TOLERANCE = 6

# 出力名 -> 元画像。
TARGETS = {
    "voice": "screenshot-1-voice.png",
    "swipe": "screenshot-2-swipe.png",
    "calendar": "screenshot-3-calendar.png",
    "notification": "screenshot-4-notification.png",
    "widget": "screenshot-5-widget.png",
    "themes": "screenshot-6-themes.png",
    # 1 画面の素の画像（BL-234）。縦横比を揃えるため余白の切り落としはしない（FULL_SCREEN）
    "due": "site-due.png",
    "import": "site-import.png",
    "data": "site-data.png",
    "look": "site-look.png",
}

FULL_SCREEN = {"due", "import", "data", "look"}


def trim_flat_rows(img: Image.Image) -> Image.Image:
    """上下の、地の色（左上の画素）と同じ行を切り落とす。"""
    bg = Image.new("RGB", img.size, img.getpixel((0, 0)))
    diff = ImageChops.difference(img, bg).point(lambda v: 255 if v > TOLERANCE else 0).convert("L")
    box = diff.getbbox()
    if box is None:
        return img
    return img.crop((0, box[1], img.width, box[3]))


def main() -> None:
    DST.mkdir(parents=True, exist_ok=True)
    for name, src in TARGETS.items():
        img = Image.open(SRC / src).convert("RGB")
        if name not in FULL_SCREEN:
            img = trim_flat_rows(img)
        height = round(img.height * WIDTH / img.width)
        img = img.resize((WIDTH, height), Image.LANCZOS)
        out = DST / f"{name}.webp"
        img.save(out, "WEBP", quality=QUALITY, method=6)
        print(f"{out.relative_to(ROOT)} {WIDTH}x{height} {out.stat().st_size} bytes")


if __name__ == "__main__":
    main()
