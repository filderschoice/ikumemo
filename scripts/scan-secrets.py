"""コミット前の秘密情報スキャン（品質ゲート）。

rules/guardrails-unified.v1.md 12.1 が自律ループ実行モードのコミット前に求める「秘密情報スキャン」の決まったコマンド。
これから記録しようとしている変更に資格情報が混ざっていないかを確かめる。

調べる範囲:

- 内容: `git diff HEAD`（ステージ済みと未ステージの両方。HEAD が無い新しいリポジトリでは `git diff --cached`）で
  追加された行と、未追跡のファイル（.gitignore で除外したものは除く）の全行。`--staged` を付けるとステージ済みだけを見る
- ファイル名: 追跡中と未追跡（.gitignore で除外したものは除く）のファイルに、署名鍵・資格情報のファイル
  （*.jks・*.keystore・keystore.properties・local.properties・secrets.properties・.env）が無いこと

検出する種類: 秘密鍵の見出し、Google API キー、GitHub トークン、AWS のアクセスキー、署名のパスワードの値
（storePassword・keyPassword・RELEASE_STORE_PASSWORD・RELEASE_KEY_PASSWORD に直接書いた値）。

誤検出を避ける工夫:

- 検出したときは「ファイル:行 種類」だけを出し、値そのものは出さない（ログや会話に秘密を残さないため）
- 値に dummy・example・sample・fake・test・placeholder・changeme・replace-me・xxxx などを含むもの、
  `<...>`・`${...}` の差し込み、同じ文字の繰り返しなど、明らかなダミー値は数えない（秘密鍵の見出しは除く）
- Kotlin などのコードで `storePassword = props.getProperty("...")` のように値を読み込む書き方は数えない
  （引用符で囲んだ値を直接書いた場合だけ数える。.properties・.env では `=` の右側をそのまま値として扱う）
- 行に `scan-secrets: allow` と書いた行は数えない（公開のテストベクタなど、意図して置く値のため。使う理由を同じ行に書く）
- 本スクリプト自身のパターンは、検出対象の文字列がソースに現れないように組み立てている

使い方:

    python scripts/scan-secrets.py           # 作業ツリーの変更（ステージ済み＋未ステージ＋未追跡）を調べる
    python scripts/scan-secrets.py --staged  # ステージ済みの変更と未追跡のファイルだけを調べる

カレントディレクトリの Git リポジトリを調べる。終了コードは、検出なしで0、検出ありで1、git の実行に失敗したら2。
"""

import pathlib
import re
import subprocess
import sys

DASHES = "-" * 5
PATTERNS = [
    ("秘密鍵の見出し", re.compile(DASHES + r"BEGIN (?:[A-Z0-9]+ )*PRIVATE KEY(?: BLOCK)?" + DASHES), False),
    ("Google API キー", re.compile(r"\b" + "AI" + r"za[0-9A-Za-z_\-]{35}(?![0-9A-Za-z_\-])"), True),
    ("GitHub トークン", re.compile(r"\b(?:" + "gh" + r"[pousr]_[A-Za-z0-9]{36,}|github_" + r"pat_[A-Za-z0-9_]{22,})\b"), True),
    ("AWS アクセスキー", re.compile(r"\b(?:" + "AK" + r"IA|AS" + r"IA)[0-9A-Z]{16}\b"), True),
]
# 署名のパスワード。値の取り出し方はファイルの種類で変える（下の password_findings）
PASSWORD_KEY = re.compile(r"(?i)\b(?:store_?pass(?:word)?|key_?pass(?:word)?|release_(?:store|key)_password)\b\s*[:=]\s*(.*)$")
QUOTED_VALUE = re.compile(r"""^(["'])(.*?)\1""")
PLAIN_FILE_SUFFIXES = {".properties", ".env", ".txt", ".cfg", ".ini", ""}

FORBIDDEN_NAMES = {"keystore.properties", "local.properties", "secrets.properties", ".env"}
FORBIDDEN_SUFFIXES = {".jks", ".keystore", ".p12", ".pfx"}

DUMMY_WORDS = re.compile(
    r"(?i)dummy|example|sample|fake|test|placeholder|changeme|change_me|replace-me|replace_me|your[-_]|xxxx|\*\*\*\*|\.\.\."
)
ALLOW_MARKER = "scan-secrets: allow"
MAX_FILE_BYTES = 2 * 1024 * 1024


def git(*args: str) -> str:
    """git を実行して標準出力を返す。失敗したら RuntimeError。"""
    result = subprocess.run(
        ["git", "-c", "core.quotepath=false", *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} が失敗した（終了コード {result.returncode}）")
    return result.stdout


def is_dummy(value: str) -> bool:
    """明らかなダミー値か。"""
    stripped = value.strip()
    if not stripped:
        return True
    if DUMMY_WORDS.search(stripped) or "<" in stripped or "${" in stripped or "$(" in stripped:
        return True
    # 同じ文字の繰り返しや、文字の種類が極端に少ない値（AIzaXXXX... など）
    return len(set(stripped)) <= 5


def password_findings(path: str, line: str) -> bool:
    """署名のパスワードを直接書いた行か。"""
    match = PASSWORD_KEY.search(line)
    if not match:
        return False
    rest = match.group(1).strip()
    quoted = QUOTED_VALUE.match(rest)
    if quoted:
        return not is_dummy(quoted.group(2))
    if pathlib.PurePosixPath(path).suffix.lower() not in PLAIN_FILE_SUFFIXES:
        return False  # コードで変数や関数から読み込む書き方は数えない
    return not is_dummy(rest.split()[0] if rest else "")


def scan_line(path: str, line_no: int, line: str, findings: list[tuple[str, int, str]]) -> None:
    """1 行を調べて、見つかった種類を findings へ足す。"""
    if ALLOW_MARKER in line:
        return
    for kind, pattern, check_dummy in PATTERNS:
        for match in pattern.finditer(line):
            if check_dummy and is_dummy(match.group(0)):
                continue
            findings.append((path, line_no, kind))
            break
    if password_findings(path, line):
        findings.append((path, line_no, "署名のパスワードの値"))


def scan_diff(diff: str, findings: list[tuple[str, int, str]]) -> None:
    """`git diff -U0` の追加行を調べる。"""
    path = None
    line_no = 0
    hunk = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")
    for line in diff.splitlines():
        if line.startswith("+++ "):
            target = line[4:]
            path = target[2:] if target.startswith("b/") else None
            continue
        match = hunk.match(line)
        if match:
            line_no = int(match.group(1))
            continue
        if path is None:
            continue
        if line.startswith("+"):
            scan_line(path, line_no, line[1:], findings)
            line_no += 1
        elif line.startswith(" "):
            line_no += 1


def scan_untracked(root: pathlib.Path, paths: list[str], findings: list[tuple[str, int, str]]) -> None:
    """未追跡のファイルの全行を調べる。大きすぎるファイルとバイナリは飛ばす。"""
    for rel in paths:
        file = root / rel
        if not file.is_file() or file.stat().st_size > MAX_FILE_BYTES:
            continue
        data = file.read_bytes()
        if b"\0" in data[:8192]:
            continue
        for index, line in enumerate(data.decode("utf-8", errors="replace").splitlines(), start=1):
            scan_line(rel, index, line, findings)


def forbidden_files(paths: list[str]) -> list[str]:
    """署名鍵・資格情報のファイル名に当たるものを返す。"""
    hits = []
    for rel in paths:
        name = pathlib.PurePosixPath(rel).name
        if name.lower() in FORBIDDEN_NAMES or pathlib.PurePosixPath(name).suffix.lower() in FORBIDDEN_SUFFIXES:
            hits.append(rel)
    return hits


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    staged_only = "--staged" in sys.argv[1:]
    try:
        root = pathlib.Path(git("rev-parse", "--show-toplevel").strip())
        has_head = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", "HEAD"], capture_output=True, check=False
        ).returncode == 0
        diff_args = ["diff", "-U0", "--no-color", "--no-ext-diff"]
        diff_args += ["--cached"] if staged_only or not has_head else ["HEAD"]
        diff = git(*diff_args)
        untracked = [p for p in git("ls-files", "--others", "--exclude-standard").splitlines() if p]
        tracked = [p for p in git("ls-files", "--cached").splitlines() if p]
    except (RuntimeError, OSError) as exc:
        print(f"NG: {exc}")
        return 2

    findings: list[tuple[str, int, str]] = []
    scan_diff(diff, findings)
    scan_untracked(root, untracked, findings)
    files = forbidden_files(sorted(set(tracked) | set(untracked)))

    for rel in files:
        print(f"NG: {rel} 署名鍵・資格情報のファイルが Git の対象になっている（.gitignore で除外する）")
    for path, line_no, kind in findings:
        print(f"NG: {path}:{line_no} {kind}（値は表示しない）")
    if files or findings:
        print(f"秘密情報の疑い {len(files) + len(findings)} 件。値を消すか、ダミーなら行に「{ALLOW_MARKER}」と理由を書く")
        return 1
    scope = "ステージ済みの変更" if staged_only else "作業ツリーの変更"
    print(f"秘密情報スキャン: OK（{scope}と未追跡 {len(untracked)} ファイル、追跡中 {len(tracked)} ファイルの名前を確認）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
