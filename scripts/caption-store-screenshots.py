#!/usr/bin/env python3
"""Play 用のスクリーンショットに説明の文を入れる（BL-218）。

docs/store/images/ のストア用画像（screenshot-*.png、1080x1920、文なし）から上下左右の単色の余白を切り落とし、
上に説明の文、下に画面を置いた 1080x1920 の PNG を docs/store/images/play/ へ書き出す。
元の画像は紹介ページ（scripts/make-site-screenshots.py）でも使うため上書きしない。
元の画像を作り直したら、このスクリプトも流し直す。文の正本は docs/store/STORE_LISTING.md の「スクリーンショット」の表。

使い方  python scripts/caption-store-screenshots.py
依存は Pillow。文字は Noto Sans JP（SIL Open Font License）の太字で描く。
Windows に入っている NotoSansJP-VF.ttf を使い、無い環境では --font で ttf/otf を指定する。
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "docs" / "store" / "images"
DST = SRC / "play"
DEFAULT_FONT = Path("C:/Windows/Fonts/NotoSansJP-VF.ttf")

CANVAS = (1080, 1920)
# ポップのテーマの背景・文字の色と、フィーチャーグラフィックのアプリ名の色（StoreFeatureGraphicExportTest と同じ）
BACKGROUND = (0xFF, 0xF4, 0xEC)
TEXT_COLOR = (0x26, 0x18, 0x13)
ACCENT = (0xB5, 0x38, 0x1C)
CAPTION_TOP = 120
CAPTION_SIZE = 64
CAPTION_MAX_WIDTH = 960
ACCENT_SIZE = (120, 10)
SHOT_TOP = 380
SHOT_BOTTOM_MARGIN = 80
SHOT_MAX_WIDTH = 960
RADIUS = 36
TOLERANCE = 6
# 切り落とした後に地の色で足す余白。ウィジェットのように地の色の上に部品を並べた画像は、部品が角丸で切れないよう余白を残す
PANEL_PAD = 56

# 出力名 -> （元画像, 説明の文, 余白を足すか）。順番は Play に上げる順。
TARGETS = {
    "screenshot-1-voice.png": ("screenshot-1-voice.png", "話すだけで、2つに分かれて入ります", False),
    "screenshot-2-swipe.png": ("screenshot-2-swipe.png", "買ったら右へ、やめたら左へ", False),
    "screenshot-3-calendar.png": ("screenshot-3-calendar.png", "買った物は日付ごとに残ります", False),
    "screenshot-4-notification.png": ("screenshot-4-notification.png", "使い切りたい物は、前の日にお知らせ", False),
    "screenshot-5-widget.png": ("screenshot-5-widget.png", "ホーム画面から、すぐ話して追加", True),
    "screenshot-6-themes.png": ("screenshot-6-themes.png", "好きな色で使えます", False),
}


def trim_flat(img: Image.Image, pad: int) -> Image.Image:
    """上下左右の、地の色（左上の画素）と同じ余白を切り落とし、地の色で pad だけ余白を足す。"""
    color = img.getpixel((0, 0))
    bg = Image.new("RGB", img.size, color)
    diff = ImageChops.difference(img, bg).point(lambda v: 255 if v > TOLERANCE else 0).convert("L")
    box = diff.getbbox()
    if box is not None:
        img = img.crop(box)
    if pad:
        padded = Image.new("RGB", (img.width + pad * 2, img.height + pad * 2), color)
        padded.paste(img, (pad, pad))
        img = padded
    return img


def load_font(path: Path, size: int) -> ImageFont.FreeTypeFont:
    font = ImageFont.truetype(str(path), size)
    try:
        font.set_variation_by_name("Bold")
    except (OSError, ValueError):
        pass  # 可変フォントでない場合はそのまま使う
    return font


def wrap(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont) -> list[str]:
    """幅に収まらなければ読点の後ろで 2 行に分ける。"""
    if draw.textlength(text, font=font) <= CAPTION_MAX_WIDTH:
        return [text]
    cut = text.find("、")
    if cut < 0:
        cut = len(text) // 2 - 1
    return [text[: cut + 1], text[cut + 1 :]]


def compose(shot: Image.Image, caption: str, font: ImageFont.FreeTypeFont) -> Image.Image:
    canvas = Image.new("RGB", CANVAS, BACKGROUND)
    draw = ImageDraw.Draw(canvas)

    lines = wrap(draw, caption, font)
    y = CAPTION_TOP
    for line in lines:
        width = draw.textlength(line, font=font)
        draw.text(((CANVAS[0] - width) / 2, y), line, font=font, fill=TEXT_COLOR)
        y += round(CAPTION_SIZE * 1.4)
    bar_x = (CANVAS[0] - ACCENT_SIZE[0]) // 2
    bar_y = y + 24
    draw.rounded_rectangle(
        (bar_x, bar_y, bar_x + ACCENT_SIZE[0], bar_y + ACCENT_SIZE[1]), radius=ACCENT_SIZE[1] // 2, fill=ACCENT
    )

    top = max(SHOT_TOP, bar_y + ACCENT_SIZE[1] + 60)
    max_h = CANVAS[1] - top - SHOT_BOTTOM_MARGIN
    scale = min(max_h / shot.height, SHOT_MAX_WIDTH / shot.width)
    size = (round(shot.width * scale), round(shot.height * scale))
    shot = shot.resize(size, Image.LANCZOS)
    x = (CANVAS[0] - size[0]) // 2
    top += (max_h - size[1]) // 3  # 低い画像（通知など）は空いた高さの 1/3 だけ下げ、文の近くに置く

    round_mask = Image.new("L", size, 0)
    ImageDraw.Draw(round_mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=RADIUS, fill=255)
    shadow = Image.new("L", CANVAS, 0)
    shadow.paste(round_mask, (x, top + 12))
    shadow = shadow.filter(ImageFilter.GaussianBlur(24)).point(lambda v: v * 40 // 255)
    canvas.paste(Image.new("RGB", CANVAS, TEXT_COLOR), (0, 0), shadow)
    canvas.paste(shot, (x, top), round_mask)
    return canvas


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--font", type=Path, default=DEFAULT_FONT, help="文字に使う ttf/otf")
    args = parser.parse_args()
    if not args.font.exists():
        print(f"フォントが見つかりません: {args.font}（--font で指定する）", file=sys.stderr)
        return 1
    font = load_font(args.font, CAPTION_SIZE)
    DST.mkdir(parents=True, exist_ok=True)
    for out_name, (src_name, caption, pad) in TARGETS.items():
        shot = trim_flat(Image.open(SRC / src_name).convert("RGB"), PANEL_PAD if pad else 0)
        out = DST / out_name
        compose(shot, caption, font).save(out, optimize=True)
        print(f"{out.relative_to(ROOT).as_posix()} {CANVAS[0]}x{CANVAS[1]} {out.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
