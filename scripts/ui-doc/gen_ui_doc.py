"""いくメモ UI デザイン設計書（単一 HTML）の生成スクリプト。

文言は mobile/src/main/res/values/strings.xml、色・角の丸みは designsystem/ThemePreset.kt から実行時に読み込む
（転記の写し間違いを防ぎ、文言・色の変更は再実行だけで追従させる）。画面の配置は Compose のコードを読んで再現したもので、
実機のスクリーンショットではない。3.0 節だけは、スクリーンショットテスト（Roborazzi、mobile/screenshots/）が
Robolectric で実際の Compose のコードを描いた画像を埋め込む。

使い方（リポジトリのルートで）:
    python -I scripts/ui-doc/gen_ui_doc.py docs/design/UI_DESIGN.html

画面・部品を足したときは、下の「画面のモック」の関数を足し、SCREENS の並びに加える。
文字列リソースに無いキーを使うと KeyError で止まる（消した文言が設計書に残らないようにするため）。
"""

import datetime
import html
import pathlib
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "docs/design/UI_DESIGN.html"
MAIN = ROOT / "mobile/src/main"

# ---------------------------------------------------------------- 実装からの読み込み


def load_strings():
    tree = ET.parse(MAIN / "res/values/strings.xml")
    return {e.get("name"): (e.text or "") for e in tree.getroot().iter("string")}


S = load_strings()


def s(key, *args):
    """文字列リソースの文言（%1$s・%1$d を args で置き換え、HTML として安全にする）。"""
    text = S[key]
    for i, a in enumerate(args, start=1):
        text = re.sub(r"%%%d\$[sd]" % i, str(a), text)
    return html.escape(text)


def load_themes():
    src = (MAIN / "kotlin/com/ikumemo/mobile/designsystem/ThemePreset.kt").read_text(encoding="utf-8")
    themes = {}
    for m in re.finditer(r"\n    ([A-Z]+)\(\n(.*?)\n    \)[,;]", src, re.S):
        name, body = m.group(1), m.group(2)

        def palette(kind):
            block = re.search(kind + r" =\s*IkumemoPalette\((.*?)\)", body, re.S).group(1)
            return {k: "#" + v[2:] for k, v in re.findall(r"(\w+) = 0x([0-9A-Fa-f]{8})", block)}

        corners = re.search(r"CornerSet\(small = (\d+), medium = (\d+), large = (\d+)\)", body).groups()
        label = re.search(r"R\.string\.(\w+)", body).group(1)
        themes[name] = {"label": S[label], "light": palette("light"), "dark": palette("dark"),
                        "corners": [int(c) for c in corners]}
    return themes


THEMES = load_themes()

# debug 版のアプリ名（src/debug の strings.xml）
DEBUG_APP_NAME = next(
    (e.text for e in ET.parse(ROOT / "mobile/src/debug/res/values/strings.xml").getroot().iter("string") if e.get("name") == "app_name"),
    "（debug の名前なし）",
)


def git(*args):
    try:
        return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return "（取得できず）"


COMMIT = git("rev-parse", "--short", "HEAD")


def load_help_index():
    """使い方の目次（assets/help/index.md の「## 章」と「- [題名](file.md)」）。"""
    chapters = []
    for line in (MAIN / "assets/help/index.md").read_text(encoding="utf-8").splitlines():
        if line.startswith("## "):
            chapters.append((line[3:].strip(), []))
        m = re.match(r"^- \[(.+)\]\((.+)\.md\)$", line.strip())
        if m and chapters:
            chapters[-1][1].append(m.group(1))
    return chapters


HELP = load_help_index()


def screenshot(screen):
    """スクリーンショットテストの正解画像（テーマ「ポップ」・明るい・普通）を data URI で返す。"""
    import base64
    png = ROOT / f"mobile/screenshots/{screen}/pop_light_normal.png"
    return "data:image/png;base64," + base64.b64encode(png.read_bytes()).decode()
TODAY = datetime.date.today().isoformat()

# ---------------------------------------------------------------- 見た目（CSS）

CSS = """
:root{--bg:#f7f4f2;--ink:#1f1b1c;--sub:#5f5658;--line:#e2d9da;--brand:#B83B5E;--card:#fff}
*{box-sizing:border-box}html{scroll-behavior:smooth}
body{margin:0;background:var(--bg);color:var(--ink);font-family:"Noto Sans JP","Yu Gothic UI","Hiragino Sans",Roboto,sans-serif;line-height:1.7;font-size:15px}
header.top{background:var(--brand);color:#fff;padding:28px 24px}
header.top .in{max-width:1200px;margin:0 auto;display:flex;gap:20px;align-items:center;flex-wrap:wrap}
header.top h1{margin:0;font-size:28px;line-height:1.3}header.top p{margin:4px 0 0;opacity:.9}
.iconwrap{width:84px;height:84px;border-radius:50%;overflow:hidden;flex:none;box-shadow:0 2px 8px rgba(0,0,0,.3)}
nav.toc{position:sticky;top:0;z-index:20;background:#fff;border-bottom:1px solid var(--line)}
nav.toc .in{max-width:1200px;margin:0 auto;padding:0 24px;display:flex;gap:4px;overflow-x:auto;white-space:nowrap}
nav.toc a{padding:10px;color:var(--sub);text-decoration:none;font-size:13px;border-bottom:2px solid transparent}
nav.toc a:hover{color:var(--brand);border-color:var(--brand)}
main{max-width:1200px;margin:0 auto;padding:8px 24px 80px}section{padding-top:40px}
h2{font-size:22px;margin:0 0 4px;padding-left:12px;border-left:6px solid var(--brand)}h3{font-size:17px;margin:28px 0 8px}
.lead{color:var(--sub);margin:4px 0 16px}
.note{background:#fff8e1;border:1px solid #f0d98a;border-radius:8px;padding:10px 14px;font-size:13.5px;margin:12px 0}
.note.info{background:#eef3fb;border-color:#bfd0ea}
table{border-collapse:collapse;width:100%;background:#fff;font-size:13.5px;margin:8px 0 16px}
th,td{border:1px solid var(--line);padding:7px 10px;vertical-align:top;text-align:left}th{background:#f3eced;font-weight:600}
code{background:#f0eaeb;border-radius:4px;padding:1px 5px;font-size:12.5px;font-family:Consolas,"Cascadia Mono",monospace}
.grid{display:flex;flex-wrap:wrap;gap:30px;align-items:flex-start}figure{margin:0}
figcaption{font-size:13px;color:var(--sub);margin-top:14px;max-width:360px}figcaption b{color:var(--ink)}
.tag{display:inline-block;font-size:11px;padding:1px 8px;border-radius:10px;background:#efe6e7;color:#5a4446;margin-right:4px}
.tag.unk{background:#fde7d6;color:#8a4a12}.tag.todo{background:#e3e9f5;color:#2a4a7a}
.sw{display:inline-block;width:18px;height:18px;border-radius:4px;border:1px solid rgba(0,0,0,.2);vertical-align:-4px;margin-right:6px}
.flow{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:10px 0}
.fb{background:#fff;border:1.5px solid var(--brand);border-radius:10px;padding:8px 12px;font-size:13px;text-align:center}
.fb.s{background:var(--brand);color:#fff}.fb.d{border-style:dashed;border-color:#9a8a8c;color:var(--sub)}.ar{color:var(--sub);font-size:18px}
footer{max-width:1200px;margin:0 auto;padding:0 24px 40px;color:var(--sub);font-size:12.5px}

/* ---- 端末フレーム（色は --p-* のテーマ変数で塗る） ---- */
.phone{width:360px;height:720px;background:var(--p-background);border-radius:26px;box-shadow:0 0 0 9px #1c1b1f,0 10px 28px rgba(0,0,0,.28);
 overflow:hidden;position:relative;color:var(--p-onSurface);font-family:Roboto,"Noto Sans JP","Yu Gothic UI",sans-serif;display:flex;flex-direction:column}
.phone.short{height:auto;min-height:420px}
.sb{height:26px;flex:none;display:flex;justify-content:space-between;align-items:center;padding:0 16px;font-size:12px}
.appbar{height:64px;flex:none;display:flex;align-items:center;padding:0 4px 0 16px;gap:8px}
.appbar .ttl{font-size:22px;flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.appbar .back{width:48px;height:48px;display:flex;align-items:center;justify-content:center;margin-left:-12px}
.body{flex:1;overflow:hidden;display:flex;flex-direction:column}.body>*{flex-shrink:0}
.tbtn{display:inline-flex;align-items:center;gap:4px;height:40px;padding:0 12px;border-radius:20px;color:var(--p-primary);font-size:14px;font-weight:500;white-space:nowrap}
.fbtn{display:inline-flex;align-items:center;justify-content:center;gap:8px;border-radius:999px;background:var(--p-primary);color:var(--p-onPrimary);font-weight:500;white-space:nowrap;padding:0 24px;height:40px;font-size:14px}
.fbtn.big{height:64px;min-width:220px;font-size:16px}
.obtn{display:inline-flex;align-items:center;justify-content:center;gap:6px;border-radius:999px;border:1px solid var(--p-outline);color:var(--p-primary);height:40px;padding:0 20px;font-size:14px;font-weight:500;white-space:nowrap}
.chip{display:inline-flex;align-items:center;gap:6px;height:32px;padding:0 12px;border-radius:8px;border:1px solid var(--p-outline);font-size:14px;color:var(--p-onSurfaceVariant);white-space:nowrap}
.chip.sel{background:var(--p-secondaryContainer);color:var(--p-onSecondaryContainer);border-color:transparent}
.row{display:flex;align-items:center;min-height:56px;border-bottom:1px solid var(--p-surfaceVariant);padding-right:16px;gap:4px}
.row .nm{flex:1;font-size:16px}.row .qt{font-size:14px;color:var(--p-onSurfaceVariant)}
.row.hl{background:var(--p-secondaryContainer)}
.circ{width:48px;height:48px;display:flex;align-items:center;justify-content:center;flex:none}
.circ i{width:20px;height:20px;border:2px solid var(--p-onSurfaceVariant);border-radius:50%}
.swipe{display:flex;align-items:center;gap:8px;min-height:56px;padding:0 24px;background:var(--p-primaryContainer);color:var(--p-onPrimaryContainer)}
.swipe.l{justify-content:flex-end;background:var(--p-surfaceVariant);color:var(--p-onSurfaceVariant)}
.swipe .rest{background:var(--p-surface);flex:none;width:200px;min-height:56px;margin:0 -24px 0 auto;display:flex;align-items:center;padding-left:16px}
.swipe.l .rest{margin:0 auto 0 -24px}
.nav{height:80px;flex:none;background:var(--p-surfaceVariant);display:flex;justify-content:space-around;align-items:center}
.nav div{display:flex;flex-direction:column;align-items:center;font-size:12px;gap:4px;width:90px}
.nav .pill{width:64px;height:32px;border-radius:16px;display:flex;align-items:center;justify-content:center}
.nav .on .pill{background:var(--p-secondaryContainer)}.nav .on{font-weight:600}
.snack{position:absolute;left:12px;right:12px;bottom:96px;background:#322f30;color:#f5eff0;border-radius:4px;padding:0 8px 0 16px;min-height:48px;display:flex;align-items:center;font-size:14px;box-shadow:0 3px 6px rgba(0,0,0,.3)}
.snack span{flex:1}.snack b{color:var(--p-primaryContainer);font-weight:500;padding:0 8px}
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.32);display:flex;flex-direction:column;justify-content:flex-end;z-index:5}
.spot{position:relative;z-index:6;box-shadow:0 0 0 2000px rgba(0,0,0,.55);border-radius:12px}
.bubble{position:absolute;left:16px;right:16px;z-index:7;background:var(--p-surface);color:var(--p-onSurface);border-radius:12px;padding:16px;box-shadow:0 2px 8px rgba(0,0,0,.3);font-size:15px;display:flex;flex-direction:column;gap:8px}
.scrim.c{justify-content:center;align-items:center}
.sheet{background:var(--p-surface);border-radius:28px 28px 0 0;padding:8px 16px 24px;display:flex;flex-direction:column;gap:12px;max-height:88%;overflow:hidden}
.sheet .grab{width:32px;height:4px;border-radius:2px;background:var(--p-onSurfaceVariant);opacity:.4;margin:14px auto 10px}
.dlg{background:var(--p-surface);border-radius:28px;padding:24px;width:312px;box-shadow:0 4px 16px rgba(0,0,0,.3);display:flex;flex-direction:column;gap:14px}
.dlg .h{font-size:22px;line-height:1.35}.dlg .acts{display:flex;justify-content:flex-end;gap:4px}
.fld{position:relative;border:1px solid var(--p-outline);border-radius:4px;min-height:56px;padding:0 4px 0 16px;display:flex;align-items:center;font-size:16px;margin-top:6px}
.fld label{position:absolute;left:12px;top:-9px;background:var(--p-surface);padding:0 4px;font-size:12px;color:var(--p-onSurfaceVariant)}
.fld .v{flex:1}.fld .mic{width:48px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--p-onSurfaceVariant)}
.fld .mic.on{color:var(--p-primary)}
.ttl-s{font-size:14px;font-weight:500}.sub{color:var(--p-onSurfaceVariant);font-size:14px}
.sect{font-size:16px;font-weight:500;color:var(--p-primary);padding:24px 16px 8px}
.srow{min-height:56px;display:flex;align-items:center;padding:12px 16px;border-bottom:1px solid var(--p-surfaceVariant);gap:16px;font-size:16px}
.srow .t{flex:1}.srow .d{display:block;font-size:14px;color:var(--p-onSurfaceVariant)}
.tog{width:52px;height:32px;border-radius:16px;flex:none;position:relative;background:var(--p-surfaceVariant);border:2px solid var(--p-outline)}
.tog::after{content:"";position:absolute;top:6px;left:6px;width:16px;height:16px;border-radius:50%;background:var(--p-outline)}
.tog.on{background:var(--p-primary);border-color:var(--p-primary)}.tog.on::after{left:24px;top:2px;width:24px;height:24px;background:var(--p-onPrimary)}
.card{background:var(--p-surfaceVariant);border-radius:var(--r-m);padding:12px 16px}
.cal{display:grid;grid-template-columns:repeat(7,1fr);text-align:center;font-size:14px;padding:0 8px}
.cal div{height:44px;display:flex;flex-direction:column;align-items:center;justify-content:center;position:relative}
.cal .wd{height:28px;font-size:12px;color:var(--p-onSurfaceVariant)}
.cal .sun{color:var(--p-error)}.cal .sat{color:var(--p-tertiary)}
.cal .sel span{background:var(--p-primary);color:var(--p-onPrimary);border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center}
.cal i{width:6px;height:6px;border-radius:50%;position:absolute;bottom:3px}
.wcell{display:inline-block;border-radius:16px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,.25);vertical-align:top}
.home{background:linear-gradient(160deg,#5d6e8a,#9fb2c8);padding:24px;border-radius:18px;display:flex;flex-wrap:wrap;gap:20px;align-items:flex-start;width:100%}
.notif{background:#fff;border-radius:20px;padding:14px 16px;width:360px;box-shadow:0 2px 8px rgba(0,0,0,.2);font-size:14px;color:#1f1b1c}
.notif .a{display:flex;gap:16px;margin-top:10px;color:#B83B5E;font-weight:500}
.mini{transform:scale(.5);transform-origin:top left;width:360px;height:720px}
.minibox{width:180px;height:360px;overflow:hidden}
@media print{nav.toc{display:none}.phone,.grid figure{break-inside:avoid}}
"""


def theme_vars(theme="POP", mode="light"):
    p = THEMES[theme][mode]
    c = THEMES[theme]["corners"]
    v = ";".join(f"--p-{k}:{val}" for k, val in p.items())
    defaults = {"error": "#BA1A1A" if mode == "light" else "#FFB4AB", "onError": "#FFFFFF" if mode == "light" else "#690005"}
    for k, val in defaults.items():
        if k not in p:
            v += f";--p-{k}:{val}"
    return v + f";--r-s:{c[0]}px;--r-m:{c[1]}px;--r-l:{c[2]}px"


# ---------------------------------------------------------------- アイコン（Material Icons の形を簡略化して再現）

ICON_PATHS = {
    # res/drawable の pathData を転記
    "mic": "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
    "keyboard": "M20,5L4,5c-1.1,0 -1.99,0.9 -1.99,2L2,17c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2L22,7c0,-1.1 -0.9,-2 -2,-2zM11,8h2v2h-2L11,8zM11,11h2v2h-2v-2zM8,8h2v2L8,10L8,8zM8,11h2v2L8,13v-2zM7,13L5,13v-2h2v2zM7,10L5,10L5,8h2v2zM16,17L8,17v-2h8v2zM16,13h-2v-2h2v2zM16,10h-2L14,8h2v2zM19,13h-2v-2h2v2zM19,10h-2L17,8h2v2z",
    # 以下は Material Icons（Rounded）の形の近似
    "gear": "M19.4,13a7.5,7.5 0 0,0 0,-2l2.1,-1.6 -2,-3.5 -2.5,1a7.6,7.6 0 0,0 -1.7,-1L14.9,3h-4l-.4,2.9a7.6,7.6 0 0,0 -1.7,1l-2.5,-1 -2,3.5L6.4,11a7.5,7.5 0 0,0 0,2l-2.1,1.6 2,3.5 2.5,-1a7.6,7.6 0 0,0 1.7,1l.4,2.9h4l.4,-2.9a7.6,7.6 0 0,0 1.7,-1l2.5,1 2,-3.5zM12.9,15.5a3.5,3.5 0 1,1 0,-7a3.5,3.5 0 0,1 0,7z",
    "back": "M20,11H7.8l5.6,-5.6L12,4l-8,8 8,8 1.4,-1.4L7.8,13H20z",
    "cart": "M7,18c-1.1,0 -2,.9 -2,2s.9,2 2,2 2,-.9 2,-2 -.9,-2 -2,-2zM17,18c-1.1,0 -2,.9 -2,2s.9,2 2,2 2,-.9 2,-2 -.9,-2 -2,-2zM7.2,14.8L7.2,14.7 8.1,13h7.4c.8,0 1.4,-.4 1.7,-1l3.9,-7 -1.7,-1 -3.9,7H8.5L4.3,2H1v2h2l3.6,7.6 -1.4,2.4C4.5,15.4 5.5,17 7,17h12v-2H7.4c-.1,0 -.2,-.1 -.2,-.2z",
    "calendar": "M19,4h-1V2h-2v2H8V2H6v2H5c-1.1,0 -2,.9 -2,2v14c0,1.1 .9,2 2,2h14c1.1,0 2,-.9 2,-2V6c0,-1.1 -.9,-2 -2,-2zM19,20H5V10h14v10z",
    "bell": "M12,22c1.1,0 2,-.9 2,-2h-4c0,1.1 .9,2 2,2zM18,16v-5c0,-3.1 -1.6,-5.6 -4.5,-6.3V4c0,-.8 -.7,-1.5 -1.5,-1.5S10.5,3.2 10.5,4v.7C7.6,5.4 6,7.9 6,11v5l-2,2v1h16v-1l-2,-2z",
    "check": "M9,16.2L4.8,12l-1.4,1.4L9,19 21,7l-1.4,-1.4z",
    "close": "M19,6.4L17.6,5 12,10.6 6.4,5 5,6.4 10.6,12 5,17.6 6.4,19 12,13.4 17.6,19 19,17.6 13.4,12z",
    "camera": "M12,15.2a3.2,3.2 0 1,0 0,-6.4a3.2,3.2 0 0,0 0,6.4zM9,2L7.2,4H4c-1.1,0 -2,.9 -2,2v12c0,1.1 .9,2 2,2h16c1.1,0 2,-.9 2,-2V6c0,-1.1 -.9,-2 -2,-2h-3.2L15,2H9z",
    "store": "M20,4H4v2h16V4zM21,14v-2l-1,-5H4l-1,5v2h1v6h10v-6h4v6h2v-6h1zM12,18H6v-4h6v4z",
    "search": "M15.5,14h-.8l-.3,-.3A6.5,6.5 0 1,0 14,15.5l.3,.3v.8l5,5 1.5,-1.5 -5,-5zM9.5,14a4.5,4.5 0 1,1 0,-9a4.5,4.5 0 0,1 0,9z",
    "stop": "M8,6h8c1.1,0 2,.9 2,2v8c0,1.1 -.9,2 -2,2H8c-1.1,0 -2,-.9 -2,-2V8c0,-1.1 .9,-2 2,-2z",
    "chev": "M10,6L8.6,7.4 13.2,12l-4.6,4.6L10,18l6,-6z",
}


def icon(name, size=24, color="currentColor"):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" aria-hidden="true" style="flex:none">'
            f'<path d="{ICON_PATHS[name]}" fill="{color}"/></svg>')


LAUNCHER = """<svg viewBox="0 0 108 108" width="84" height="84" aria-label="アプリのアイコン">
<rect width="108" height="108" fill="#FF7A6B"/>
<path fill="#FFFFFF" d="M34,30h40a8,8 0,0 1,8 8v26a8,8 0,0 1,-8 8H50l-10,9v-9h-6a8,8 0,0 1,-8 -8V38a8,8 0,0 1,8 -8z"/>
<path fill="none" stroke="#FF7A6B" stroke-width="3" stroke-linecap="round" d="M45,49a9,9 0,0 1,18 0"/>
<path fill="#FF7A6B" d="M39,49h30l-3,15a3,3 0,0 1,-3 2.4H45a3,3 0,0 1,-3 -2.4z"/></svg>"""

# ---------------------------------------------------------------- 部品

SAMPLE = [("牛乳", "2本", False), ("たまご", "1パック", False), ("食パン", None, True), ("トイレットペーパー", None, False)]


def phone(inner, theme="POP", mode="light", cls=""):
    return f'<div class="phone {cls}" style="{theme_vars(theme, mode)}"><div class="sb"><span>9:41</span><span>▮▮ 100%</span></div>{inner}</div>'


def appbar(title, back=False, actions=""):
    b = f'<div class="back">{icon("back")}</div>' if back else ""
    return f'<div class="appbar">{b}<div class="ttl">{title}</div>{actions}</div>'


def bottom_nav(active):
    tabs = [("cart", s("tab_memo")), ("calendar", s("tab_calendar")), ("bell", s("tab_reminder"))]
    cells = "".join(
        f'<div class="{"on" if i == active else ""}"><span class="pill">{icon(ic)}</span>{label}</div>'
        for i, (ic, label) in enumerate(tabs))
    return f'<div class="nav">{cells}</div>'


def item_row(name, qty=None, photo=False, circle=True, hl=False, spot=False):
    c = '<div class="circ"><i></i></div>' if circle else '<div style="width:16px"></div>'
    p = f'<span style="color:var(--p-onSurfaceVariant);margin-right:8px">{icon("camera", 20)}</span>' if photo else ""
    q = f'<span class="qt">{html.escape(qty)}</span>' if qty else ""
    cls = ("hl " if hl else "") + ("spot" if spot else "")
    return f'<div class="row {cls}">{c}<span class="nm">{html.escape(name)}</span>{p}{q}</div>'


def field(label, value="", mic=True, listening=False, multi=False):
    m = ""
    if mic:
        m = f'<span class="mic {"on" if listening else ""}">{icon("stop" if listening else "mic")}</span>'
    h = "min-height:88px;align-items:flex-start;padding-top:14px" if multi else ""
    return f'<div class="fld" style="{h}"><label>{label}</label><span class="v">{html.escape(value)}</span>{m}</div>'


def sheet(inner):
    return f'<div class="scrim"><div class="sheet"><div class="grab"></div>{inner}</div></div>'


def dialog(title, text, acts):
    a = "".join(f'<span class="tbtn">{x}</span>' for x in acts)
    t = f'<div class="sub">{text}</div>' if text else ""
    return f'<div class="scrim c"><div class="dlg"><div class="h">{title}</div>{t}<div class="acts">{a}</div></div></div>'


def srow(title, detail=None, tog=None, chev=False):
    d = f'<span class="d">{detail}</span>' if detail else ""
    t = f'<span class="tog {"on" if tog else ""}"></span>' if tog is not None else ""
    c = icon("chev") if chev else ""
    return f'<div class="srow"><span class="t">{title}{d}</span>{t}{c}</div>'


def fig(mock, title, caption):
    return f'<figure>{mock}<figcaption><b>{title}</b><br>{caption}</figcaption></figure>'


# ---------------------------------------------------------------- 画面のモック

def memo_screen(items=True, voice=True, overlay="", snack="", circle=True, hl=None, practice=False, spot=None):
    settings = f'<span class="tbtn">{icon("gear", 20)}{s("settings_title")}</span>'
    chip_cls = "chip spot" if spot == "chip" else "chip"
    chip = f'<div style="padding:0 16px"><span class="{chip_cls}">{icon("store", 18)}{s("store_chip_unselected")}</span></div>'
    if items:
        rows = item_row(s("guide_practice_item"), spot=spot == "practice") if practice else ""
        rows += "".join(item_row(n, q, p, circle, hl == n) for n, q, p in SAMPLE)
    else:
        rows = (f'<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:24px;'
                f'text-align:center;font-size:16px">{s("home_empty")}<span class="tbtn">{s("help_open")}</span></div>')
    mic_cls = "fbtn big spot" if spot == "mic" else "fbtn big"
    if voice:
        add = (f'<span class="{mic_cls}">{icon("mic")}{s("memo_add_by_voice")}</span>'
               f'<span class="tbtn">{icon("keyboard")}{s("memo_add_by_text")}</span>')
    else:
        add = (f'<div class="sub" style="text-align:center">{s("voice_unavailable")}</div>'
               f'<span class="tbtn">{s("help_open")}</span>'
               f'<span class="fbtn" style="height:56px;min-width:200px">{icon("keyboard")}{s("memo_add_by_text")}</span>')
    body = (f'<div class="body">{chip}<div style="flex:1;display:flex;flex-direction:column">{rows}</div>'
            f'<div style="display:flex;flex-direction:column;align-items:center;gap:8px;padding:16px">{add}</div></div>')
    return phone(appbar(s("memo_title"), actions=settings) + body + bottom_nav(0) + snack + overlay)


def guide_bubble(key, top=None):
    """その場のガイドの吹き出し（GuideOverlay）。top が None なら読み上げ利用時の画面下のカード。"""
    pos = f"top:{top}px" if top is not None else "bottom:96px"
    return (f'<div class="bubble" style="{pos}"><div>{s(key)}</div><div style="display:flex;align-items:center">'
            f'<span class="tbtn" style="font-size:12px">{s("guide_disable_all")}</span><span style="flex:1"></span>'
            f'<span class="fbtn">{s("guide_ok")}</span></div></div>')


def onboarding_screen(page):
    """初めての案内（OnboardingContent）。page は 0〜3。"""
    pages = [("🎤", "onboarding_voice_title", "onboarding_voice_body"), ("👉", "onboarding_swipe_title", "onboarding_swipe_body"),
             ("📅", "onboarding_look_back_title", "onboarding_look_back_body"), ("🏠", "onboarding_home_title", "onboarding_home_body")]
    emoji, title, body = pages[page]
    last = page == len(pages) - 1
    demo = (f'<div style="display:flex;gap:8px;margin-top:8px"><span class="chip sel">{s("onboarding_demo_item1")}</span>'
            f'<span class="chip sel">{s("onboarding_demo_item2")}</span></div>') if page == 0 else ""
    pins = (f'<div style="display:flex;flex-direction:column;gap:8px;align-items:center;margin-top:16px">'
            f'<span class="obtn">{s("onboarding_pin_voice")}</span><span class="obtn">{s("onboarding_pin_memo")}</span></div>') if last else ""
    dots = "".join(f'<i style="display:inline-block;width:8px;height:8px;border-radius:50%;margin:0 4px;'
                   f'background:{"var(--p-primary)" if i == page else "var(--p-outline)"}"></i>' for i in range(len(pages)))
    inner = (f'<div class="body" style="padding:16px"><div style="text-align:right"><span class="tbtn">{s("onboarding_skip")}</span></div>'
             f'<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;gap:8px">'
             f'<div style="font-size:72px;line-height:1">{emoji}</div>{demo}<div style="font-size:22px;margin-top:16px">{s(title)}</div>'
             f'<div style="font-size:16px">{s(body)}</div>{pins}</div><div style="text-align:center">{dots}</div>'
             f'<div style="text-align:right;margin-top:16px"><span class="fbtn">{s("onboarding_later" if last else "onboarding_next")}</span></div></div>')
    return phone(inner)


def help_index_screen():
    rows = ""
    for chapter, pages in HELP[:3]:
        rows += f'<div class="sect" style="color:var(--p-primary)">{html.escape(chapter)}</div>' + "".join(srow(html.escape(t)) for t in pages)
    body = f'<div class="body"><div style="padding:16px"><span class="obtn">{s("help_show_onboarding")}</span></div>{rows}</div>'
    return phone(appbar(s("settings_help"), back=True) + body)


def help_page_screen():
    # 本文は assets/help/swipe.md と同じ文（ページの例）
    text = (MAIN / "assets/help/swipe.md").read_text(encoding="utf-8")
    lines = [ln for ln in re.sub(r"<!--.*?-->", "", text, flags=re.S).splitlines() if ln.strip()]
    title = lines[0][2:]
    items = "".join(f'<div style="font-size:15px">・{html.escape(ln[2:])}</div>' if ln.startswith("- ")
                    else f'<div style="font-size:15px">{html.escape(ln)}</div>' for ln in lines[1:])
    body = (f'<div class="body" style="padding:16px;gap:12px"><div style="font-size:22px">{html.escape(title)}</div>{items}'
            f'<div style="display:flex;gap:8px;margin-top:8px"><span class="obtn">{s("help_show_guide")}</span>'
            f'<span class="fbtn">{s("help_try")}</span></div></div>')
    return phone(appbar(s("settings_help"), back=True) + body, cls="short")


def home_screen_settings():
    cards = "".join(
        f'<div class="card"><div style="display:flex;gap:12px;align-items:center">{icon(ic, 40)}<div><div style="font-size:16px">{s(t)}</div>'
        f'<div class="sub">{s(d)}</div></div></div><div style="text-align:right;margin-top:8px"><span class="fbtn">{s("home_screen_pin")}</span></div></div>'
        for ic, t, d in [("mic", "home_screen_voice", "home_screen_voice_detail"), ("cart", "home_screen_memo", "home_screen_memo_detail")])
    body = (f'<div class="body" style="padding:16px;gap:16px">{cards}<div style="font-size:16px">{s("home_screen_shortcuts_title")}</div>'
            f'<div style="font-size:14px">{s("home_screen_shortcuts")}</div></div>')
    return phone(appbar(s("settings_home_screen"), back=True) + body)


def add_record_sheet():
    inner = (f'<div style="font-size:22px">{s("calendar_add_record")}</div>{field(s("edit_name"), "アイス")}'
             f'<span class="obtn" style="align-self:flex-start">{s("calendar_record_date", 2026, 10, 24)}</span>'
             f'<div class="ttl-s">{s("calendar_record_store")}</div><div style="display:flex;gap:8px"><span class="chip">さくらスーパー</span></div>'
             f'{field(s("edit_quantity"), "")}{field(s("edit_note"), "", multi=True)}'
             f'<div style="display:flex;justify-content:flex-end"><span class="fbtn" style="height:48px">{s("calendar_add_record_save")}</span></div>')
    return calendar_screen(overlay=sheet(inner))


def swipe_screen():
    right = (f'<div class="swipe">{icon("check")}{s("memo_bought")}<div class="rest">牛乳</div></div>')
    left = (f'<div class="swipe l"><div class="rest">たまご</div>{s("memo_dismissed")}{icon("close")}</div>')
    rows = right + left + item_row("食パン", photo=True)
    snack = f'<div class="snack"><span>{s("memo_bought_message", "牛乳")}</span><b>{s("memo_undo")}</b></div>'
    body = f'<div class="body"><div style="height:8px"></div>{rows}</div>'
    return phone(appbar(s("memo_title")) + body + bottom_nav(0) + snack)


def add_sheet():
    usual = "".join(f'<span class="chip">{s("add_sheet_chip", n)}</span>' for n in ["たまご", "食パン", "納豆", "バナナ"])
    inner = (f'<div style="font-size:22px">{s("add_sheet_title")}</div>'
             f'{field(s("add_sheet_hint"), "ぎゅう", mic=False)}'
             f'<div class="ttl-s">{s("add_sheet_usual")}</div><div style="display:flex;flex-wrap:wrap;gap:8px">{usual}</div>'
             f'<div style="display:flex;justify-content:flex-end;gap:8px"><span class="tbtn">{s("common_close")}</span>'
             f'<span class="fbtn">{s("add_sheet_submit")}</span></div>')
    return memo_screen(overlay=sheet(inner))


def edit_sheet(listening=False):
    stores = (f'<div class="ttl-s">{s("edit_planned_store")}</div><div style="display:flex;gap:8px;flex-wrap:wrap">'
              f'<span class="chip sel">{s("edit_planned_store_none")}</span><span class="chip">さくらスーパー</span>'
              f'<span class="chip">駅前ストア</span></div>')
    photo = (f'<div class="ttl-s">{s("edit_photo")}</div><div style="display:flex;gap:8px">'
             f'<span class="obtn">{icon("camera", 18)}{s("edit_photo_take")}</span><span class="obtn">{s("edit_photo_pick")}</span></div>')
    inner = (field(s("edit_name"), "牛乳", listening=listening) + field(s("edit_quantity"), "2本")
             + field(s("edit_note"), "低脂肪", multi=True) + stores + photo
             + f'<div style="display:flex;justify-content:flex-end"><span class="fbtn" style="height:48px">{s("common_close")}</span></div>')
    return memo_screen(overlay=sheet(inner))


def voice_sheet(state):
    if state == "listening":
        inner = (f'<div style="text-align:center;padding:24px 0"><div style="display:inline-flex;width:96px;height:96px;border-radius:50%;'
                 f'background:var(--p-primaryContainer);align-items:center;justify-content:center;color:var(--p-onPrimaryContainer)">{icon("mic", 48)}</div>'
                 f'<div style="font-size:22px;margin-top:12px">{s("voice_listening")}</div><div class="sub">{s("voice_partial", "牛乳2本とたまご")}</div></div>')
    elif state == "cards":
        cards = (f'<div class="card" style="display:flex;justify-content:space-between"><span>牛乳</span><span class="sub">2本</span></div>'
                 f'<div class="card" style="display:flex;justify-content:space-between"><span>たまご</span><span class="sub"></span></div>'
                 f'<div class="card" style="display:flex;justify-content:space-between;opacity:.6"><span>食パン</span><span class="sub">{s("voice_already_in_memo")}</span></div>')
        inner = (cards + f'<div style="display:flex;justify-content:space-between"><span class="tbtn">{icon("mic", 18)}{s("voice_retry")}</span>'
                 f'<span class="fbtn">{s("voice_add_all")}</span></div>')
    else:
        inner = (f'<div style="padding:16px 0;font-size:16px">{s("voice_permission_rationale")}</div>'
                 f'<div style="display:flex;justify-content:space-between"><span class="tbtn">{s("voice_type_instead")}</span>'
                 f'<span class="fbtn">{s("voice_use_microphone")}</span></div>')
    return memo_screen(overlay=sheet(inner))


def store_picker():
    rows = "".join(f'<div class="srow">{icon("store", 20)}<span class="t">{n}</span></div>' for n in ["さくらスーパー", "駅前ストア", "ドラッグかもめ"])
    inner = (f'<div style="font-size:22px">{s("store_picker_title")}</div>{rows}'
             f'<div class="srow"><span class="t">{s("store_picker_other")}</span>{icon("chev")}</div>'
             f'<div class="srow"><span class="t">{s("store_picker_new")}</span>{icon("chev")}</div>'
             f'<div style="display:flex;justify-content:flex-end"><span class="tbtn">{s("store_picker_clear")}</span></div>')
    return memo_screen(overlay=sheet(inner))


def calendar_screen(search=False, overlay=""):
    act = f'<span class="tbtn">{icon("search", 20)}{s("calendar_search_close") if search else s("calendar_search")}</span>'
    if search:
        res = "".join(
            f'<div class="srow"><span class="t">牛乳<span class="d">さくらスーパー</span></span><span class="sub">{s("calendar_result_date", 2026, 10, d)}</span></div>'
            for d in (24, 17, 9))
        body = f'<div class="body"><div style="padding:0 16px">{field(s("calendar_search_hint"), "牛乳", mic=False)}</div>{res}</div>'
        return phone(appbar(s("tab_calendar"), actions=act) + body + bottom_nav(1))
    wd = S["calendar_weekdays"].split(",")
    cells = "".join(f'<div class="wd {"sun" if i == 0 else "sat" if i == 6 else ""}">{w}</div>' for i, w in enumerate(wd))
    # 2026 年 10 月は木曜始まり
    cells += "<div></div>" * 4
    dots = {3: "var(--p-primary)", 10: "var(--p-tertiary)", 17: "var(--p-primary)", 24: "var(--p-secondary)"}
    for d in range(1, 32):
        col = (4 + d - 1) % 7
        cls = "sun" if col == 0 else "sat" if col == 6 else ""
        inner = f"<span>{d}</span>"
        if d == 24:
            cls += " sel"
        dot = f'<i style="background:{dots[d]}"></i>' if d in dots else ""
        cells += f'<div class="{cls}">{inner}{dot}</div>'
    head = (f'<div style="display:flex;align-items:center;justify-content:space-between;padding:0 8px">{icon("back")}'
            f'<span style="font-size:18px">{s("calendar_month", 2026, 10)}</span><span style="transform:scaleX(-1);display:inline-flex">{icon("back")}</span></div>')
    day = (f'<div style="padding:12px 16px 4px;font-size:16px;font-weight:500">{s("calendar_day_title", 10, 24, "土")}</div>'
           + "".join(f'<div class="srow"><span class="t">{n}<span class="d">{q}</span></span>{icon("camera", 20) if p else ""}</div>'
                     for n, q, p in [("牛乳", "2本", False), ("食パン", "さくらスーパー", True)])
           + f'<div style="padding:8px 16px"><span class="obtn">{s("calendar_add_record")}</span></div>')
    body = f'<div class="body">{head}<div class="cal">{cells}</div>{day}</div>'
    return phone(appbar(s("tab_calendar"), actions=act) + body + bottom_nav(1) + overlay)


def reminder_screen(due_sheet=False):
    recent = (f'<div class="sect">{s("reminder_recent")}</div>'
              f'<div class="srow"><span class="t">牛乳<span class="d">{s("reminder_bought_on", 10, 24)}</span></span><span class="obtn">{s("reminder_set_due")}</span></div>')
    pend = [("ヨーグルト", s("reminder_overdue", 10, 23), "var(--p-error)"), ("食パン", s("reminder_due_today", 10, 25), "var(--p-primary)"),
            ("豆腐", s("reminder_due_tomorrow", 10, 26), "var(--p-onSurface)"), ("しょうゆ", s("reminder_due_on", 11, 20), "var(--p-onSurfaceVariant)")]
    rows = "".join(f'<div class="srow"><span class="t">{n}<span class="d" style="color:{c}">{d}</span></span></div>' for n, d, c in pend)
    rows += f'<div style="padding:8px"><span class="tbtn">{s("help_not_arriving")}</span></div>'
    body = f'<div class="body">{recent}<div class="sect">{s("reminder_pending")}</div>{rows}</div>'
    overlay = ""
    if due_sheet:
        chips = "".join(f'<span class="chip">{s(k)}</span>' for k in
                        ["reminder_due_3days", "reminder_due_1week", "reminder_due_2weeks", "reminder_due_1month", "reminder_due_pick"])
        overlay = sheet(f'<div style="font-size:22px">{s("reminder_due_title", "牛乳")}</div><div style="display:flex;flex-wrap:wrap;gap:8px">{chips}</div>')
    return phone(appbar(s("tab_reminder")) + body + bottom_nav(2) + overlay)


SETTINGS_ITEMS = ["settings_help", "settings_appearance", "settings_operation", "settings_reminder", "settings_stores",
                  "settings_products", "settings_home_screen", "settings_feedback", "settings_about"]


def settings_screen():
    rows = "".join(srow(s(k)) for k in SETTINGS_ITEMS)
    return phone(appbar(s("settings_title"), back=True) + f'<div class="body">{rows}</div>', cls="short")


def appearance_screen():
    cards = ""
    for name, t in THEMES.items():
        p = t["light"]
        sel = "outline:3px solid var(--p-primary);outline-offset:2px;" if name == "POP" else ""
        cards += (f'<div style="width:76px;{sel}border-radius:{t["corners"][1]}px;overflow:hidden;border:1px solid {p["outline"]};background:{p["surface"]}">'
                  f'<div style="height:14px;background:{p["primary"]}"></div><div style="padding:4px;font-size:9px;color:{p["onSurface"]}">牛乳<br>'
                  f'<span style="display:inline-block;background:{p["secondaryContainer"]};color:{p["onSecondaryContainer"]};border-radius:4px;padding:0 3px">＋たまご</span></div>'
                  f'<div style="text-align:center;font-size:11px;padding:2px 0 4px;color:{p["onSurface"]}">{html.escape(t["label"])}</div></div>')
    chips_dark = "".join(f'<span class="chip {"sel" if i == 0 else ""}">{s(k)}</span>' for i, k in
                         enumerate(["appearance_dark_system", "appearance_dark_light", "appearance_dark_dark"]))
    chips_text = "".join(f'<span class="chip {"sel" if i == 0 else ""}">{s(k)}</span>' for i, k in
                         enumerate(["appearance_text_normal", "appearance_text_large", "appearance_text_extra_large"]))
    body = (f'<div class="body" style="padding:0 16px;gap:10px"><div class="ttl-s">{s("appearance_theme")}</div>'
            f'<div style="display:flex;gap:8px">{cards}</div><div class="ttl-s">{s("appearance_background")}</div>'
            f'<div style="display:flex;gap:8px"><span class="obtn">{s("appearance_background_pick")}</span>'
            f'<span class="tbtn">{s("appearance_background_remove")}</span></div>'
            f'<div class="sub">{s("appearance_scrim")}</div><div style="height:4px;border-radius:2px;background:var(--p-primary);'
            f'width:60%;position:relative"><i style="position:absolute;right:-10px;top:-8px;width:20px;height:20px;border-radius:50%;'
            f'background:var(--p-primary)"></i></div><div class="ttl-s">{s("appearance_dark_mode")}</div>'
            f'<div style="display:flex;gap:8px;flex-wrap:wrap">{chips_dark}</div><div class="ttl-s">{s("appearance_text_size")}</div>'
            f'<div style="display:flex;gap:8px;flex-wrap:wrap">{chips_text}</div>'
            f'<div class="card">{s("appearance_sample_item")}<br><span class="chip sel">{s("appearance_sample_chip")}</span></div></div>')
    return phone(appbar(s("settings_appearance"), back=True) + body)


def operation_screen():
    rows = (srow(s("operation_show_bought_button"), s("operation_show_bought_button_detail"), tog=True)
            + srow(s("operation_vibration"), s("operation_vibration_detail"), tog=True))
    return phone(appbar(s("settings_operation"), back=True) + f'<div class="body">{rows}</div>', cls="short")


def reminder_settings_screen():
    time_row = (f'<div class="srow"><span class="t">{s("reminder_settings_time")}<span class="d">{s("reminder_settings_time_detail")}</span></span>'
                f'<span style="color:var(--p-primary);font-size:16px;font-weight:500">9:00</span></div>')
    rows = time_row + srow(s("reminder_settings_on_due_date"), s("reminder_settings_on_due_date_detail"), tog=True)
    return phone(appbar(s("settings_reminder"), back=True) + f'<div class="body">{rows}</div>', cls="short")


def store_manage_screen(dialog_on=False):
    rows = "".join(
        f'<div style="padding:12px 8px 0 16px;border-bottom:1px solid var(--p-surfaceVariant)"><div style="font-size:16px">{n}</div>'
        f'<div style="display:flex;justify-content:flex-end"><span class="tbtn">{s("stores_merge")}</span><span class="tbtn">{s("stores_archive")}</span></div></div>'
        for n in ["さくらスーパー", "さくらスーパー駅前"])
    overlay = dialog(s("stores_archive_title", "さくらスーパー駅前"), s("stores_archive_detail"),
                     [s("reminder_settings_cancel"), s("stores_archive")]) if dialog_on else ""
    return phone(appbar(s("settings_stores"), back=True) + f'<div class="body">{rows}</div>' + overlay, cls="short")


def product_manage_screen():
    rows = "".join(srow(n, s("products_count", c), tog=on) for n, c, on in [("牛乳", 12, True), ("たまご", 9, True), ("お試しのお茶", 1, False)])
    body = f'<div class="body"><div class="sub" style="padding:16px">{s("products_detail")}</div>{rows}</div>'
    return phone(appbar(s("settings_products"), back=True) + body, cls="short")


def feedback_screen():
    faq = "".join(srow(s(k)) for k in ["faq_notification_q", "faq_voice_q", "faq_device_change_q", "faq_widget_q"])
    first = (f'<div class="srow" style="flex-direction:column;align-items:flex-start;gap:6px"><span>{s("faq_notification_q")}</span>'
             f'<span class="d sub">{s("faq_notification_a")}</span></div>')
    faq = first + "".join(srow(s(k)) for k in ["faq_voice_q", "faq_device_change_q", "faq_widget_q"])
    send = (f'<div class="sect">{s("feedback_send_title")}</div><div style="padding:0 16px;font-size:14px">{s("feedback_send_detail")}'
            f'<div class="sub" style="margin:8px 0">{s("feedback_device_info", "1.0.0", "15", "Google Pixel 8")}</div>'
            f'<span class="obtn">{s("feedback_copy_device_info")}</span><div style="margin:16px 0"><span class="fbtn" style="width:100%">{s("feedback_open_github")}</span></div></div>')
    body = f'<div class="body"><div class="sect">{s("feedback_faq_title")}</div>{faq}{send}</div>'
    return phone(appbar(s("settings_feedback"), back=True) + body, cls="short")


def about_screen():
    head = (f'<div style="padding:16px"><div style="font-size:24px">{s("app_name")}</div><div class="sub">{s("about_version", "1.0.0")}</div>'
            f'<div style="font-size:14px;margin-top:12px">{s("about_origin")}</div></div>')
    rows = "".join(srow(s(k)) for k in ["about_licenses", "about_privacy_policy", "about_github", "about_store"])
    return phone(appbar(s("settings_about"), back=True) + f'<div class="body">{head}{rows}</div>')


def voice_widget(wide=False, available=True, theme="POP", mode="light"):
    p = THEMES[theme][mode]
    ic = icon("mic" if available else "keyboard", 28, p["onPrimary"])
    if wide:
        label = s("widget_voice_label_wide") if available else s("widget_write_label")
        inner = f'<div style="display:flex;align-items:center;justify-content:center;gap:6px;width:150px;height:70px">{ic}<span>{label}</span></div>'
    else:
        label = s("widget_voice_label") if available else s("widget_write_label_short")
        inner = f'<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;width:70px;height:70px">{ic}<span style="font-size:13px">{label}</span></div>'
    return f'<div class="wcell" style="background:{p["primary"]};color:{p["onPrimary"]}">{inner}</div>'


def memo_widget(large=False, empty=False, narrow=False, theme="POP", mode="light"):
    p = THEMES[theme][mode]
    w = 250 if not narrow else 180
    head = (f'<div style="display:flex;align-items:center;height:48px;gap:8px;white-space:nowrap;overflow:hidden">'
            f'<b style="color:{p["primary"]}">{s("widget_memo_title")}</b><span>{s("widget_memo_count", 0 if empty else 5)}</span>'
            + ("" if empty else f'<span style="color:{p["primary"]};font-weight:500">{s("widget_memo_due_soon", 2)}</span>') + "</div>")
    if empty:
        body = f'<div style="height:{130 if large else 50}px;display:flex;align-items:center;justify-content:center;color:{p["onSurfaceVariant"]}">{s("widget_memo_empty")}</div>'
    elif large:
        lines = "".join(
            f'<div style="display:flex;height:48px;align-items:center"><span style="flex:1">{s("widget_memo_bullet", n)}</span>'
            f'<span style="color:{p["onSurfaceVariant"]}">{q or ""}</span></div>' for n, q, _ in SAMPLE)
        body = f'<div style="color:{p["onSurfaceVariant"]}">{s("widget_memo_store", "さくらスーパー")}</div>{lines}'
    else:
        n = 3 if not narrow else 2
        names = "　".join(s("widget_memo_bullet", f"{nm} {q}" if q else nm) for nm, q, _ in SAMPLE[:n])
        body = (f'<div style="display:flex;white-space:nowrap"><span style="flex:1;overflow:hidden;text-overflow:ellipsis">{names}</span>'
                f'<span style="color:{p["onSurfaceVariant"]};padding-left:8px">{s("widget_memo_more", 5 - n)}</span></div>')
    return (f'<div class="wcell" style="background:{p["surface"]};color:{p["onSurface"]};width:{w}px;padding:4px 12px 8px;font-size:14px">'
            f'{head}{body}</div>')


def notification():
    return (f'<div class="notif"><div style="font-size:12px;color:#5f5658">{s("app_name")} ・ {s("notification_channel_due")}</div>'
            f'<b>{s("notification_due_tomorrow", "豆腐")}</b><div>{s("notification_due_body")}</div>'
            f'<div class="a"><span>{s("reminder_used_up")}</span><span>{s("notification_buy_again")}</span><span>{s("notification_later")}</span></div></div>')


# ---------------------------------------------------------------- 章


def section(id_, title, lead, body):
    return f'<section id="{id_}"><h2>{title}</h2><p class="lead">{lead}</p>{body}</section>'


def grid(*figs):
    return '<div class="grid">' + "".join(figs) + "</div>"


def table(head, rows):
    h = "".join(f"<th>{x}</th>" for x in head)
    b = "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in rows)
    return f"<table><tr>{h}</tr>{b}</table>"


def ch_overview():
    return section("overview", "1. 概要と読み方", "UI の現状を数分で把握するための 1 枚。", f"""
<div class="note"><b>画面の絵はコードからの再現で、実機のスクリーンショットではありません。</b>ただし 3.0 節の 3 枚は、
スクリーンショットテスト（Roborazzi）が Robolectric の上で実際の Compose のコードを描いた画像です（実機ではないため、端末ごとの違いは含まない）。文言は
<code>strings.xml</code>、色と角の丸みは <code>designsystem/ThemePreset.kt</code> から生成時に読み込んでいます。
配置・余白は Compose のコードを読んで近づけたもので、実機と細部は異なります。アイコンは Material Icons の形の近似です
（ウィジェットのマイク・キーボードとアプリのアイコンは <code>res/drawable</code> の pathData をそのまま使用）。
品名・お店・日付は説明用のサンプルです。</div>
{table(["項目", "内容"], [
    ["対象", "いくメモ（Android、Jetpack Compose + Material 3、Glance ウィジェット）"],
    ["対象コミット", f"<code>{COMMIT}</code>（作成・更新日 {TODAY}）"],
    ["生成", "<code>python -I scripts/ui-doc/gen_ui_doc.py docs/design/UI_DESIGN.html</code>"],
    ["既定の見た目", f"テーマ「{html.escape(THEMES['POP']['label'])}」・明るい画面・文字の大きさ「{s('appearance_text_normal')}」（初期値）。画面幅 360dp を 360px で描画"],
    ["実機での確認", "<span class='tag unk'>未確認</span> 実機・エミュレータでの見た目の確認は人手検証（BACKLOG）として残っている"],
])}
<div class="note info">更新の手順: UI のコード・文字列・色を変えたら <code>git diff &lt;対象コミット&gt;..HEAD --stat -- mobile/src/main</code>
で差分を洗い、文言・色だけならスクリプトの再実行で追従する。画面や部品の追加・削除はスクリプトのモック関数を直してから再実行する。</div>""")


def ch_flow():
    f = lambda t, c="": f'<div class="fb {c}">{t}</div>'
    a = '<span class="ar">→</span>'
    flows = [
        [f("初回起動", "s"), a, f("初めての案内（3 枚＋ホーム画面に置く）"), f(s("tab_memo"))],
        [f("下タブ", "s"), f(s("tab_memo")), f(s("tab_calendar")), f(s("tab_reminder"))],
        [f(s("tab_memo"), "s"), a, f("追加シート（" + s("memo_add_by_voice") + " / " + s("memo_add_by_text") + "）"), f("編集シート"), f("お店を選ぶシート"),
         f("使い方のページ（" + s("help_open") + "）")],
        [f(s("tab_calendar"), "s"), a, f("日の詳細"), f("記録の編集シート"), f(s("calendar_add_record")), f(s("calendar_search"))],
        [f(s("tab_reminder"), "s"), a, f("期限を決めるシート"), f("通知の許可の案内"), f("使い方のページ（" + s("help_not_arriving") + "）")],
        [f("⚙ " + s("settings_title"), "s"), a] + [f(s(k)) for k in SETTINGS_ITEMS],
        [f(s("settings_help"), "s"), a, f("目次"), f("ページ"), f(s("help_show_guide") + " / " + s("help_try") + "（各画面へ）"), f(s("help_show_onboarding"))],
        [f(s("settings_about"), "s"), a, f(s("about_licenses")), f("ブラウザ（プライバシーポリシー・GitHub・ストア）", "d")],
        [f("ウィジェット・通知・ショートカット", "s"), a, f("音声入力シート（ホーム画面に重ねる）"), f("メモ（項目を光らせる・追加シート）"),
         f(s("tab_calendar")), f(s("tab_reminder"))],
    ]
    body = "".join(f'<div class="flow">{"".join(x)}</div>' for x in flows)
    return section("flow", "2. 画面構成と遷移", "下タブ 3 つ＋右上の ⚙ 設定（PLAN §3）。点線は外部。", body)


def ch_screens():
    actual = grid(*[
        f'<figure><img src="{screenshot(sc)}" width="360" height="640" alt="{title}の実際の描画" style="border-radius:12px;'
        f'box-shadow:0 0 0 1px #ccc"><figcaption><b>{title}</b><br>mobile/screenshots/{sc}/pop_light_normal.png</figcaption></figure>'
        for sc, title in [("memo", s("tab_memo")), ("calendar", s("tab_calendar")), ("reminder", s("tab_reminder"))]])
    memo = grid(
        fig(memo_screen(), "メモ（項目あり）", "上にお店のチップ、下に 🎤 話して追加（64dp）と文字で追加。左の ○ で「買った」。"),
        fig(swipe_screen(), "スワイプと元に戻す", "右へで「買った」、左へで「やめた」。しきい値は画面幅の 40%。下に「元に戻す」。"),
        fig(memo_screen(items=False), "メモ（空）", "最初の案内の文言と「" + s("help_open") + "」（話して入れるページへ、H-03）。"),
        fig(memo_screen(voice=False, circle=False), "音声入力が無い端末・○ を消した設定", "マイクのボタンを出さず案内文と「" + s("help_open") + "」を出す。○ は操作の設定で消せる。"),
    )
    sheets = grid(
        fig(add_sheet(), "追加シート（文字）", "入力中の候補と「いつもの」チップ。チップは押すたびに 1 件入る。"),
        fig(edit_sheet(), "編集シート", "品名・数量・メモの横にマイク（BL-022）。買うお店と写真。閉じると保存。"),
        fig(edit_sheet(listening=True), "編集シート（品名を聞いている）", "聞いている欄のマイクが止めるボタンに変わる。分割しないで入れる。"),
        fig(store_picker(), "お店を選ぶシート", "最近使った順の上位 5 件・ほかのお店・新しいお店・選ばない。"),
    )
    voice = grid(
        fig(voice_sheet("permission"), "音声入力（許可の前）", "理由を添えてマイクの許可を求める。"),
        fig(voice_sheet("listening"), "音声入力（聞いている）", "途中の文字を「」で出す。"),
        fig(voice_sheet("cards"), "音声入力（確認カード）", "分けた品目をカードで確かめて追加。メモにある物は印を付ける。"),
    )
    others = grid(
        fig(calendar_screen(), "カレンダー", "月の表示（日曜は赤・土曜は青、CalendarColors）、お店の色のドット、選んだ日の記録と「" + s("calendar_add_record") + "」。"),
        fig(calendar_screen(search=True), "カレンダー（探す）", "品名で記録を検索し、日付へ移れる。"),
        fig(add_record_sheet(), "後から記録（F-17）", "メモに入れていなかった物を、選んだ日に買った物として記録する。品名が空のうちは押せない。"),
        fig(reminder_screen(), "お知らせ", "最近買った物（期限を決める）と、期限が近い順の一覧。"),
        fig(reminder_screen(due_sheet=True), "期限を決めるシート", "チップで選び、日付の入力をなるべく要らなくする。"),
    )
    guide = grid(
        fig(onboarding_screen(0), "初めての案内（1 枚目）", "初回起動時だけ全画面で出す。「飛ばす」は常に出す。権限は求めない。「" + s("onboarding_demo_spoken") + "」が 2 つに分かれる手本を動かす（絵は最後の形）。"),
        fig(onboarding_screen(3), "初めての案内（最後）", "ホーム画面に置く（W-03）。「置く」では閉じず、「後で」で閉じる。"),
        fig(memo_screen(items=False, spot="mic", overlay=guide_bubble("guide_g02", top=330)), "その場のガイド G-02",
            "空のメモで 🎤 を照らす。暗くするのは見た目だけで、照らした部品はそのまま押せる。"),
        fig(memo_screen(practice=True, spot="practice", overlay=guide_bubble("guide_g03", top=200)), "G-03 と「練習」",
            "一番上に DB に入れない「" + s("guide_practice_item") + "」を出し、動かすと練習を終える（記録・候補・元に戻すに残さない）。"),
        fig(memo_screen(overlay=guide_bubble("guide_g06")), "読み上げ利用時のガイド（R-56）",
            "TalkBack を使っているときは照らさず、画面下のカードで同じ内容を出して「" + s("guide_ok") + "」へ移る。"),
    )
    return section("screens", "3. 各画面", "状態の違いはコードの分岐から代表的な組み合わせを選んだ。",
                   "<h3>3.0 実際の描画（スクリーンショットテスト）</h3>"
                   "<p class='lead'>テーマ「ポップ」・明るい・普通。4 テーマ × 明暗 × 文字の大きさ 3 段階の 72 枚は mobile/screenshots/ にある。</p>"
                   + actual + "<h3>3.1 メモ</h3>" + memo + "<h3>3.2 メモのシート</h3>" + sheets + "<h3>3.3 話して追加</h3>" + voice
                   + "<h3>3.4 カレンダー・お知らせ</h3>" + others + "<h3>3.5 初めての案内とその場のガイド</h3>" + guide)


def ch_settings():
    g1 = grid(
        fig(settings_screen(), s("settings_title"), "PLAN §3 の並びのうち実装済みの項目。"),
        fig(appearance_screen(), s("settings_appearance"), "各テーマの色で描いた見本のカードで選ぶ。"),
        fig(operation_screen(), s("settings_operation"), "○ の表示と振動。初期値はどちらもオン。"),
        fig(reminder_settings_screen(), s("settings_reminder"), "時刻は時計の形のダイアログ（24 時間表示）で選ぶ。"),
    )
    g2 = grid(
        fig(store_manage_screen(), s("settings_stores"), "使わない（確認を 1 回出す）とまとめる。"),
        fig(store_manage_screen(dialog_on=True), "使わないお店にする確認", "記録のお店名は残ると説明する。"),
        fig(product_manage_screen(), s("settings_products"), "買った回数の多い順。オフにすると候補に出ない。"),
        fig(feedback_screen(), s("settings_feedback"), "よくある質問（押すと答え）→ GitHub。版と機種名をコピーできる。"),
        fig(about_screen(), s("settings_about"), "版・名前の由来・ライセンス一覧・外部のページ。"),
    )
    g3 = grid(
        fig(help_index_screen(), s("settings_help") + "（目次）", "設定の一番上。章とページは assets/help/index.md（先頭 3 章を表示）。"),
        fig(help_page_screen(), s("settings_help") + "（ページ）", "本文は assets/help/ の Markdown。下に「" + s("help_show_guide") + "」「" + s("help_try") + "」。"),
        fig(home_screen_settings(), s("settings_home_screen"), "2 種の見本と「" + s("home_screen_pin") + "」。メモ一覧は 4×4 で置かれる。"),
    )
    return section("settings", "4. 設定", "右上の ⚙ から開く画面。", g1 + g2 + g3)


def ch_dialogs():
    rows = [
        ["メモの知らせ（下に出る）", s("memo_already_in_memo", "牛乳"), "同じ物を入れたとき（R-01）。既存の項目を光らせる"],
        ["元に戻す", s("memo_bought_message", "牛乳") + " ／ " + s("memo_dismissed_message", "たまご") + " ＋ " + s("memo_undo"), "最新の 1 件だけ（R-04）"],
        ["音声入力", s("voice_not_heard"), "聞き取れなかったとき"],
        ["音声入力", s("voice_permission_denied") + "（" + s("voice_open_settings") + "）", "マイクの許可を断られたとき（R-44）"],
        ["通知の許可", s("reminder_permission_rationale") + "（" + s("reminder_permission_allow") + " ／ " + s("reminder_permission_later") + "）", "初めて期限を決めたとき（Android 13 以降）"],
        ["通知がオフ", s("reminder_notifications_off") + "（" + s("reminder_enable_notifications") + "）", "お知らせ画面の上"],
        ["お店", s("stores_archive_title", "○○") + " ／ " + s("stores_merge_title", "○○"), "お店の管理"],
        ["時刻", s("reminder_settings_pick_time") + "（" + s("common_done") + " ／ " + s("reminder_settings_cancel") + "）", "お知らせの設定"],
        ["ウィジェットから追加", s("widget_voice_added", 3), "トースト"],
        ["外部のページ", s("about_no_browser"), "ブラウザが無いとき"],
    ]
    guide_rows = [[g, s("guide_" + g.lower().replace("-", "")), w] for g, w in [
        ("G-02", "空のメモを初めて開いた（🎤）"), ("G-03", "初めてメモに項目が入った（練習項目）"),
        ("G-04", "練習を終えた・とばした（一番上の ○。○ を出す設定のときだけ）"), ("G-05", "初めて「買った」にした（元に戻す）"),
        ("G-06", "G-05 の後でお店を選んでいない（お店のチップ）"), ("G-07", "初めてカレンダーを開いた（その月の最初の買い物の日）"),
        ("G-08", "初めて「買った」の後にお知らせを開いた（一番上の「期限を決める」）")]]
    rows_n = [
        ["前日", s("notification_due_tomorrow", "豆腐"), s("notification_due_many_tomorrow", "豆腐", 2)],
        ["当日", s("notification_due_today", "食パン"), s("notification_due_many_today", "食パン", 2)],
        ["期限切れ", s("notification_due_past", "ヨーグルト"), s("notification_due_many_past", "ヨーグルト", 2)],
        ["種類が混ざる", "—", s("notification_due_many_mixed", "豆腐", 2)],
    ]
    return section("dialogs", "5. ダイアログ・知らせ・通知", "文言はすべて strings.xml から。", table(["場面", "文言", "出す条件"], rows)
                   + "<h3>通知（チャンネル「" + s("notification_channel_due") + "」）</h3>"
                   + grid(fig(notification(), "1 品目の通知", "押すとお知らせの一覧。ボタンは 1 品目の時だけ。"))
                   + table(["種類", "1 品目", "まとめた時（同じ日の 1 通）"], rows_n)
                   + "<h3>その場のガイド（1 画面に 1 つ・各 1 回。シート表示中は出さない）</h3>"
                   + table(["ID", "吹き出しの文", "きっかけ（照らす場所）"], guide_rows))


def ch_widgets():
    home = (f'<div class="home">{voice_widget()}{voice_widget(wide=True)}{voice_widget(available=False)}'
            f'{memo_widget()}{memo_widget(narrow=True)}</div>'
            f'<div class="home" style="margin-top:16px">{memo_widget(large=True)}{memo_widget(empty=True)}'
            f'{voice_widget(theme="COOL", mode="dark")}{memo_widget(theme="COOL", mode="dark")}</div>')
    rows = [
        ["話して追加（1×1〜2×2）", s("widget_voice_name"), "押すと音声入力シートを重ねて開く。横 2 マス以上で文字を横に並べる。音声入力が無い端末は「" + s("widget_write_label") + "」"],
        ["メモ一覧（既定 4×4、3×2〜5×6）", s("widget_memo_name"), "表示専用。行はその項目、期限が近い件数はお知らせ、ほかはメモを開く。幅 250dp 未満で品名 2 件・数量を下へ。"
         "アプリから置く（W-03）と 4×4 で置かれ、長押しで 4×2 などへ小さくできる"],
        ["ショートカット（アイコンの長押し）", "、".join(s(k) for k in ["shortcut_voice", "shortcut_text", "shortcut_calendar", "shortcut_reminder"]),
         "起動時に動的ショートカットとして登録（debug 版でも動くように）。ホーム画面アプリが対応していれば単独で置ける"],
    ]
    return section("widgets", "6. ホーム画面ウィジェット", "Glance。色はアプリのテーマと暗い画面の設定に合わせる（背景はホーム画面のイメージ）。",
                   home + table(["ウィジェット", "選ぶ画面での名前", "動き"], rows)
                   + '<p class="lead">下段の右 2 つは テーマ「' + html.escape(THEMES["COOL"]["label"]) + '」・暗い画面 の例。</p>')


def ch_tokens():
    roles = ["primary", "onPrimary", "primaryContainer", "secondaryContainer", "tertiary", "background", "surface", "surfaceVariant",
             "onSurface", "onSurfaceVariant", "outline"]
    head = ["役割"] + [f'{html.escape(t["label"])}（明）' for t in THEMES.values()] + [f'{html.escape(t["label"])}（暗）' for t in THEMES.values()]
    rows = []
    for r in roles:
        row = [f"<code>{r}</code>"]
        for mode in ("light", "dark"):
            for t in THEMES.values():
                c = t[mode].get(r, "—")
                row.append(f'<span class="sw" style="background:{c}"></span><code>{c}</code>' if c != "—" else "—")
        rows.append(row)
    corners = [[html.escape(t["label"]), *[f"{v}dp" for v in t["corners"]]] for t in THEMES.values()]
    gallery = grid(*[
        f'<figure><div class="minibox"><div class="mini">{phone_memo_theme(n, m)}</div></div>'
        f'<figcaption><b>{html.escape(THEMES[n]["label"])}</b>（{"明るい" if m == "light" else "暗い"}）</figcaption></figure>'
        for n in THEMES for m in ("light", "dark")])
    return section("tokens", "7. デザイントークン", "定義元: <code>designsystem/ThemePreset.kt</code>・<code>IkumemoPalette.kt</code>・<code>TextSizeSetting.kt</code>。",
                   "<h3>7.1 4 テーマ × 明るい／暗い（メモ画面の縮小）</h3>" + gallery
                   + "<h3>7.2 色（主な役割）</h3>" + table(head, rows)
                   + "<h3>7.3 角の丸み</h3>" + table(["テーマ", "small", "medium", "large"], corners)
                   + "<h3>7.4 文字の大きさと寸法</h3>" + table(["項目", "値", "定義元"], [
                       [s("appearance_text_normal") + " / " + s("appearance_text_large") + " / " + s("appearance_text_extra_large"),
                        "端末の文字サイズ × 1.0 / 1.15 / 1.3（上限 2.0 倍）", "<code>TextSizeSetting.kt</code>"],
                       ["押せる部分の高さ", "48dp 以上（行 56dp、🎤 話して追加 64dp）", "各画面の <code>heightIn</code>"],
                       ["スワイプのしきい値", "画面幅の 40%", "<code>SwipeableItemRow.kt</code>"],
                       ["背景の写真の膜", "テーマの背景色、濃さ 0.3〜0.9（既定 0.6）。暗い画面では 0.15 濃く（上限 0.95）", "<code>BackgroundScrim.kt</code>"],
                       ["ガイドの周りの暗さ", "黒 55%", "<code>GuideOverlay.kt</code>"],
                       ["フォント", "端末標準（同梱しない）", "PLAN §2.7"],
                   ]))


def phone_memo_theme(name, mode):
    return memo_screen().replace(theme_vars("POP", "light"), theme_vars(name, mode))


def ch_variants():
    return section("variants", "8. 版（ビルドの種類）の違い", "UI に関わる違いだけ。", table(["項目", "debug", "release"], [
        ["アプリ名", html.escape(DEBUG_APP_NAME), s("app_name")],
        ["applicationId", "<code>com.ikumemo.mobile.debug</code>", "<code>com.ikumemo.mobile</code>"],
        ["ストアのページ（このアプリについて）", "release の ID のページを開く", "同左"],
    ]))


def ch_findings():
    items = [
        ("実機で未確認", "画面は Robolectric の画面テスト・コントラストのテスト・スクリーンショットテスト（72 枚）で確認。実機での配置、"
                       "ガイドの吹き出しの位置、背景の写真の見え方、ホーム画面アプリごとのウィジェットの大きさは<span class='tag unk'>未確認</span>（人手検証として BACKLOG に残っている）。"),
        ("未実装の UI", "PLAN の MVP の UI はすべて実装済み（「LINE などで送る」F-08 など S 以下の機能は対象外）。"),
        ("動き", "初めての案内 1 枚目は「牛乳とたまご」が 2 つに分かれる手本を 3 回、G-03 は練習項目を右へ少し動かす手本を 2 回見せて止まる。"
               "端末の「アニメーションを減らす」では動かさない（R-56）。本書の絵は止まった最後の形。"),
        ("アクセシビリティ", "アイコンだけのボタンには説明を付け、色だけで伝えず文字を添えている（スワイプの背景、設定の ⚙ に文字）。"
                          "メモの行は読み上げの操作の一覧に「" + s("memo_bought") + "」「" + s("memo_dismissed") + "」を出す。TalkBack での実際の読み上げ順は<span class='tag unk'>未確認</span>。"),
        ("暗い画面のカレンダー", "端末は明るい設定・アプリだけ暗い画面のとき、曜日の色とお店の点が明るい画面用の色になる不具合があり、スクリーンショットテストで見つけて直した（LocalDarkTheme）。"),
        ("ウィジェットの見出し", "メモ一覧（幅 250dp）の見出しは「" + s("widget_memo_title") + "」「" + s("widget_memo_count", 5) + "」「"
                         + s("widget_memo_due_soon", 2) + "」を 1 行に並べるため、本書の再現では収まらず末尾が切れる。"
                         "Glance では各文字が 1 行で、はみ出し方はホーム画面アプリの大きさと端末の文字サイズで変わる<span class='tag unk'>未確認</span>。"),
        ("ダークテーマ", "4 テーマとも暗い配色を持ち、ウィジェットにも反映する。Dynamic Color（おまかせ）は v1.x のため未対応。"),
        ("アイコン", "画面内のアイコンは Material Icons（Rounded）。本書の絵は形の近似。"),
    ]
    rows = "".join(f"<tr><th style='width:160px'>{a}</th><td>{b}</td></tr>" for a, b in items)
    return section("findings", "9. 所見", "根拠のある事実と「未確認」を分けて書く。優先度付けと起票はしない。", f"<table>{rows}</table>")


def build():
    toc = [("overview", "概要"), ("flow", "画面構成"), ("screens", "各画面"), ("settings", "設定"), ("dialogs", "知らせ・通知"),
           ("widgets", "ウィジェット"), ("tokens", "トークン"), ("variants", "版の違い"), ("findings", "所見")]
    nav = "".join(f'<a href="#{i}">{t}</a>' for i, t in toc)
    body = (ch_overview() + ch_flow() + ch_screens() + ch_settings() + ch_dialogs() + ch_widgets() + ch_tokens()
            + ch_variants() + ch_findings())
    return f"""<!doctype html>
<html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>いくメモ UI デザイン設計書</title><style>{CSS}</style></head><body>
<header class="top"><div class="in"><div class="iconwrap">{LAUNCHER}</div><div><h1>いくメモ UI デザイン設計書</h1>
<p>対象コミット {COMMIT} ・ {TODAY} ・ 画面の絵はコードからの再現</p></div></div></header>
<nav class="toc"><div class="in">{nav}</div></nav><main>{body}</main>
<footer>生成: scripts/ui-doc/gen_ui_doc.py（文言は strings.xml、色は ThemePreset.kt から読み込み）。</footer></body></html>
"""


if __name__ == "__main__":
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(build(), encoding="utf-8", newline="\n")
    print(f"wrote {OUT} ({OUT.stat().st_size // 1024} KB, commit {COMMIT})")
