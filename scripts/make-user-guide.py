"""使い方の Web 版（docs/USER_GUIDE.md）を、アプリ内の使い方の正本（mobile/src/main/assets/help/）から作る。

PLAN §2.12「内容の管理」。アプリと Web で説明が食い違わないよう、Web 版は手で直さずこのスクリプトで作り直す。

使い方:
    python scripts/make-user-guide.py          # docs/USER_GUIDE.md を書き直す
    python scripts/make-user-guide.py --check  # 書き直さずに、正本と一致しているかだけ確かめる（一致しなければ終了コード 1）
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
HELP = ROOT / 'mobile' / 'src' / 'main' / 'assets' / 'help'
OUTPUT = ROOT / 'docs' / 'USER_GUIDE.md'

COMMENT = re.compile(r'<!--.*?-->', re.S)
PAGE_LINK = re.compile(r'^- \[(?P<title>[^\]]+)\]\((?P<file>[^)]+\.md)\)$')


def read(path: pathlib.Path) -> str:
    return path.read_text(encoding='utf-8')


def page_body(path: pathlib.Path) -> str:
    """ページの本文。先頭の見出し（# 題名）と、アプリ向けの書き込み（<!-- try: ... -->）を除く。"""
    lines = COMMENT.sub('', read(path)).strip().splitlines()
    if not lines or not lines[0].startswith('# '):
        raise SystemExit(f'{path.name}: 1 行目は「# 題名」にしてください')
    return '\n'.join(lines[1:]).strip()


def build() -> str:
    out = [
        '# いくメモ 使い方',
        '',
        '<!-- このファイルは scripts/make-user-guide.py が mobile/src/main/assets/help/ から作る。直接は直さない -->',
        '',
        'アプリの「せってい」→「使い方」と同じ内容です。',
    ]
    for line in COMMENT.sub('', read(HELP / 'index.md')).splitlines():
        line = line.strip()
        if line.startswith('## '):
            out += ['', line]
            continue
        match = PAGE_LINK.match(line)
        if match:
            out += ['', f"### {match['title']}", '', page_body(HELP / match['file'])]
    return '\n'.join(out) + '\n'


def main() -> int:
    text = build()
    if '--check' in sys.argv[1:]:
        current = OUTPUT.read_text(encoding='utf-8') if OUTPUT.exists() else ''
        if current != text:
            print('docs/USER_GUIDE.md が使い方の正本と一致しません。python scripts/make-user-guide.py で作り直してください')
            return 1
        print('docs/USER_GUIDE.md: OK')
        return 0
    OUTPUT.write_text(text, encoding='utf-8', newline='\n')
    print('docs/USER_GUIDE.md を作りました')
    return 0


if __name__ == '__main__':
    sys.exit(main())
