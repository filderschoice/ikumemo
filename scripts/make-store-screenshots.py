#!/usr/bin/env python3
"""ストア用スクリーンショットの加工（BL-217、ぼかしを切り落とす形へ BL-231）。

実機で撮った元画像（ぼかし前。リポジトリへは入れない）に、個人情報が写り込みうる領域のぼかし・塗りつぶしを入れ、
Play の規格に合わせて 1080x1920（9:16）の PNG にする。実機の画面は約 9:20 で「長辺は短辺の 2 倍まで」を超えるため、
縦に収まるよう縮小して中央に置き、余白は元画像の地の色に近い単色で埋める。座標は元画像（1344x2992）のピクセル。

個人情報が写る領域は、ぼかすより切り落とす方を優先する（ぼかしの箱が目立つため。BL-231）。ステータスバー（上端 140px）は
--crop 0,140,1344,2992 で切り落とす。--crop は元画像すべてに効き、切り出した範囲が縦長なら 1 画面、横長なら帯として置く。

使い方（--blur / --fill / --panel は何回でも指定できる。--blur・--fill は切り落とせない領域だけに使う）:
  1 画面     python scripts/make-store-screenshots.py OUT.png RAW.png --crop 0,140,1344,2992
  切り出し   python scripts/make-store-screenshots.py OUT.png RAW.png --crop 40,1330,1304,1650
             （通知の 1 枚だけを撮りたいときなど。横長の帯は幅に合わせて拡大して中央に置く）
  2x2 の並び python scripts/make-store-screenshots.py OUT.png A.png B.png C.png D.png --crop 0,140,1344,2992
  部品だけ   python scripts/make-store-screenshots.py OUT.png RAW.png --panel 39,1011,975,1369 --panel 39,1421,1305,2603
             --radius 110 --bg 38,52,78
             （ホーム画面のウィジェットなど。各範囲を角丸で切り抜き、元の左右の位置を保って単色の上に縦に並べる）

2026-11-16 の 6 枚の元画像と指定（元画像は scratchpad にだけ置き、リポジトリへは入れない）:
  1-voice voice.png / 2-swipe c1.png / 3-calendar d2.png は 1 画面の指定。6-themes は th_pop・th_gentle・th_cool・th_simple の順に
  2x2 の指定。5-widget は widget.png に上の「部品だけ」の指定。4-notification は通知カードの切り出し。

依存は Pillow。ぼかしは判読できない強さ（半径 40 を 2 回）にする。
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

CANVAS = (1080, 1920)
BLUR_RADIUS = 40
GRID_GAP = 40
CROP_MARGIN = 60
PANEL_GAP = 80


def parse_box(text: str) -> tuple[int, int, int, int]:
    x0, y0, x1, y1 = (int(v) for v in text.split(","))
    return x0, y0, x1, y1


def mask(img: Image.Image, blur: list[str], fill: list[str]) -> Image.Image:
    img = img.convert("RGB")
    for text in blur:
        box = parse_box(text)
        region = img.crop(box)
        for _ in range(2):  # 端の色の引き込みで縞が残らないよう 2 回かける
            region = region.filter(ImageFilter.GaussianBlur(BLUR_RADIUS))
        img.paste(region, box[:2])
    for text in fill:
        box = parse_box(text)
        img.paste(img.getpixel((max(box[0] - 1, 0), max(box[1] - 1, 0))), box)
    return img


def single(img: Image.Image, bg: tuple[int, int, int] | None) -> Image.Image:
    scale = CANVAS[1] / img.height
    size = (round(img.width * scale), CANVAS[1])
    img = img.resize(size, Image.LANCZOS)
    canvas = Image.new("RGB", CANVAS, bg or img.getpixel((2, img.height // 2)))
    canvas.paste(img, ((CANVAS[0] - size[0]) // 2, 0))
    return canvas


def cropped(img: Image.Image, bg: tuple[int, int, int] | None) -> Image.Image:
    width = CANVAS[0] - CROP_MARGIN * 2
    img = img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)
    canvas = Image.new("RGB", CANVAS, bg or img.getpixel((0, 0)))
    canvas.paste(img, (CROP_MARGIN, (CANVAS[1] - img.height) // 2))
    return canvas


def panels(img: Image.Image, boxes: list[str], radius: int, bg: tuple[int, int, int] | None) -> Image.Image:
    # 角丸で切り抜いた部品を、元の左右の位置を保ったまま 1 つの倍率で縮小し、縦に並べて中央に置く
    rects = [parse_box(text) for text in boxes]
    left = min(r[0] for r in rects)
    right = max(r[2] for r in rects)
    total_h = sum(r[3] - r[1] for r in rects)
    width = CANVAS[0] - CROP_MARGIN * 2
    scale = min(width / (right - left), (CANVAS[1] - CROP_MARGIN * 2 - PANEL_GAP * (len(rects) - 1)) / total_h)
    height = round(total_h * scale) + PANEL_GAP * (len(rects) - 1)
    canvas = Image.new("RGB", CANVAS, bg or (40, 40, 48))
    x0 = (CANVAS[0] - round((right - left) * scale)) // 2
    y = (CANVAS[1] - height) // 2
    for r in rects:
        part = img.crop(r)
        size = (round(part.width * scale), round(part.height * scale))
        part = part.resize(size, Image.LANCZOS)
        mask_img = Image.new("L", size, 0)
        ImageDraw.Draw(mask_img).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), round(radius * scale), fill=255)
        canvas.paste(part, (x0 + round((r[0] - left) * scale), y), mask_img)
        y += size[1] + PANEL_GAP
    return canvas


def grid(images: list[Image.Image], bg: tuple[int, int, int] | None) -> Image.Image:
    # 2 列 x 2 行。高さを 2 行と隙間で割り、幅は縦横比から決める
    panel_h = (CANVAS[1] - GRID_GAP * 3) // 2
    panel_w = round(images[0].width * panel_h / images[0].height)
    left = (CANVAS[0] - panel_w * 2 - GRID_GAP) // 2
    canvas = Image.new("RGB", CANVAS, bg or (245, 242, 245))
    for i, img in enumerate(images):
        panel = img.resize((panel_w, panel_h), Image.LANCZOS)
        x = left + (i % 2) * (panel_w + GRID_GAP)
        y = GRID_GAP + (i // 2) * (panel_h + GRID_GAP)
        canvas.paste(panel, (x, y))
    return canvas


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("out", type=Path)
    parser.add_argument("raw", type=Path, nargs="+", help="元画像（4 枚なら 2x2 に並べる）")
    parser.add_argument("--blur", action="append", default=[], help="x0,y0,x1,y1 をぼかす")
    parser.add_argument("--fill", action="append", default=[], help="x0,y0,x1,y1 を単色で塗りつぶす")
    parser.add_argument("--crop", help="x0,y0,x1,y1 だけを残す（ぼかし・塗りつぶしの後に、元画像すべてで切り出す）")
    parser.add_argument("--panel", action="append", default=[], help="x0,y0,x1,y1 を角丸で切り抜いて並べる")
    parser.add_argument("--radius", type=int, default=0, help="--panel の角丸の半径（元画像のピクセル）")
    parser.add_argument("--bg", help="余白の色 R,G,B（省略時は画像から取る）")
    args = parser.parse_args()
    bg = tuple(int(v) for v in args.bg.split(",")) if args.bg else None
    images = [mask(Image.open(p), args.blur, args.fill) for p in args.raw]
    if args.crop:
        images = [img.crop(parse_box(args.crop)) for img in images]
    if len(images) == 1 and args.panel:
        result = panels(images[0], args.panel, args.radius, bg)
    elif len(images) == 4:
        result = grid(images, bg)
    elif len(images) == 1 and images[0].width > images[0].height:
        result = cropped(images[0], bg)
    elif len(images) == 1:
        result = single(images[0], bg)
    else:
        parser.error("元画像は 1 枚か 4 枚")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    result.save(args.out, "PNG", optimize=True)
    print(f"wrote {args.out} {result.size}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
