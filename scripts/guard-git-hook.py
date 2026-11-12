"""Claude Code の PreToolUse フック: `git push` と `--no-verify` を止める（BL-122）。

拒否の 1 段目は `.claude/settings.json` の `permissions.deny`（`Bash(git push *)` など）で、本フックは 2 段目。
deny のルールはコマンドの書き出しで照合するため `git -C path push` のような書き方を取りこぼす
（公式ドキュメント permissions「what a Bash rule doesn't match」）。本フックはコマンドを字句に分けて、
git の大域オプションを読み飛ばしたうえでサブコマンドを見る。

入出力（公式ドキュメント hooks の PreToolUse）:

- 標準入力に JSON。`tool_name` が `Bash` か `PowerShell` のとき `tool_input.command` を調べる
- 止めるときは終了コード 2 で、標準エラーへ理由を書く（Claude Code はそれを Claude へ返し、コマンドは実行されない）
- それ以外は終了コード 0 で何も出さない（通常の権限の判定へ進む）

フック自身の異常（入力が読めない・JSON でない・想定外の例外）では止めず、終了コード 0 で通す。
無関係なコマンドまで止めて作業全体を止めないためで、止める役目の 1 段目は permissions.deny が担う。

止める対象:

- git のサブコマンドが `push`（`git push`、`git -C path push`、`git -c k=v push`、`& git push` など）
- git のコマンドに `--no-verify` がある（`git commit --no-verify`、`git push --no-verify`、`git merge --no-verify`）
- `git commit` の `-n`（`--no-verify` の短い形。`-nm` のようにまとめた形も含む）

通す例: コミットメッセージや grep の検索語に push という語が入っているだけのコマンド。引用符の中は 1 語として扱い、
区切りごとにコマンドの位置の語（先頭の変数代入と sudo・env などの前置きは読み飛ばす）だけを見て、引数の中の git は見ない。
bash のヒアドキュメントの本文（`git commit -F - <<'EOF'` で渡すメッセージなど）も見ない。

既知の取りこぼし: git の別名（`git config alias.p push` の後の `git p`）、変数やスクリプト経由の実行。
これらは規約（guardrails 12.2）で禁止しており、仕組みでの完全な遮断は狙わない。

確かめ方: `python scripts/guard-git-hook.py --self-test`（止める例・通す例を流し、すべて期待どおりなら終了コード 0）。
"""

import json
import re
import shlex
import sys

BLOCK_MESSAGE = (
    "BL-122 のフックで止めました（{reason}）。git push はユーザーが実行し、"
    "検証を飛ばす --no-verify は使いません（guardrails 12.2）。"
)

SEPARATORS = {";", "&&", "||", "|", "&", "|&", "(", ")", "\n", ";;", "{", "}"}
# git の大域オプションのうち、次の語を値に取るもの
GIT_OPTIONS_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--super-prefix", "--config-env"}
# git commit のオプションのうち、次の語を値に取るもの（その値を -n と取り違えないため）
COMMIT_OPTIONS_WITH_VALUE = {
    "-m", "-F", "-C", "-c", "-t",
    "--message", "--file", "--author", "--date", "--template", "--reuse-message", "--reedit-message",
    "--fixup", "--squash", "--trailer", "--cleanup", "--pathspec-from-file",
}
SHELLS = {"bash", "sh", "zsh", "pwsh", "powershell", "cmd"}
SHELL_COMMAND_FLAGS = {"-c", "-command", "/c", "-lc"}
HERE_STRING = re.compile(r"@'.*?'@|@\".*?\"@", re.S)
HEREDOC = re.compile(r"<<-?\s*(['\"]?)([A-Za-z_][A-Za-z0-9_]*)\1")
ASSIGNMENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")
DURATION = re.compile(r"^\d+(\.\d+)?[smhd]?$")  # timeout 10 git push の 10
# 後ろの語をコマンドとして実行する前置きのコマンド
WRAPPERS = {"sudo", "env", "command", "exec", "time", "nohup", "xargs", "nice", "timeout"}
FALLBACK_PATTERNS = [
    re.compile(r"(?:^|[;&|\n(])\s*&?\s*git(?:\.exe)?\b[^;&|\n'\"]*?\bpush\b"),
    re.compile(r"\bgit(?:\.exe)?\b[^;&|\n'\"]*?\s--no-verify\b"),
]
MAX_DEPTH = 3


def program_name(token: str) -> str:
    """`/usr/bin/git.exe` → `git` のように、パスと拡張子を外して小文字にする。"""
    name = re.split(r"[\\/]", token)[-1].lower()
    return name[:-4] if name.endswith(".exe") else name


def tokenize(command: str, powershell: bool) -> list[str]:
    """引用符を解いて語に分ける。改行と制御演算子は 1 語として残す。"""
    lexer = shlex.shlex(command, posix=True, punctuation_chars="();<>|&\n{}")
    lexer.whitespace = " \t\r"
    lexer.whitespace_split = True
    lexer.commenters = ""
    if powershell:
        lexer.escape = "`"
    return list(lexer)


def split_segments(tokens: list[str]) -> list[list[str]]:
    segments: list[list[str]] = [[]]
    for token in tokens:
        if token in SEPARATORS:
            segments.append([])
        else:
            segments[-1].append(token)
    return [segment for segment in segments if segment]


def git_subcommand(args: list[str]) -> tuple[str | None, list[str]]:
    """git の後ろの語から、大域オプションを読み飛ばしてサブコマンドと残りの引数を返す。"""
    index = 0
    while index < len(args):
        arg = args[index]
        if arg in GIT_OPTIONS_WITH_VALUE:
            index += 2
            continue
        if arg.startswith("-"):
            index += 1
            continue
        return arg.lower(), args[index + 1 :]
    return None, []


def commit_has_short_no_verify(args: list[str]) -> bool:
    previous = ""
    for arg in args:
        if previous in COMMIT_OPTIONS_WITH_VALUE:
            previous = ""
            continue
        if arg == "--":
            return False
        if re.fullmatch(r"-[A-Za-z]+", arg) and "n" in arg[1:]:
            # -m などの値を取るオプションが n より前にあれば、n はその値の一部
            cluster = arg[1:]
            before_n = cluster[: cluster.index("n")]
            if not any(f"-{c}" in COMMIT_OPTIONS_WITH_VALUE for c in before_n):
                return True
        previous = arg
    return False


def check_git(args: list[str]) -> str | None:
    subcommand, rest = git_subcommand(args)
    if subcommand == "push":
        return "git push"
    if "--no-verify" in args:
        return "--no-verify"
    if subcommand == "commit" and commit_has_short_no_verify(rest):
        return "git commit -n（--no-verify）"
    return None


def command_word_index(segment: list[str]) -> int | None:
    """区切りの中でコマンドとして実行される語の位置。先頭の変数代入（A=b）と前置きのコマンド（sudo・env など）を読み飛ばす。"""
    index = 0
    while index < len(segment):
        token = segment[index]
        if ASSIGNMENT.match(token):
            index += 1
            continue
        if program_name(token) in WRAPPERS:
            index += 1
            while index < len(segment) and (segment[index].startswith("-") or DURATION.match(segment[index])):
                index += 1
            continue
        return index
    return None


def check_segment(segment: list[str], depth: int) -> str | None:
    # 引数の中の git（grep の検索語・ヒアドキュメントで渡すメッセージなど）は見ない。コマンドの位置の語だけを見る
    index = command_word_index(segment)
    if index is None:
        return None
    name = program_name(segment[index])
    if name == "git":
        return check_git(segment[index + 1 :])
    if name in SHELLS and depth < MAX_DEPTH:
        for flag_index in range(index + 1, len(segment) - 1):
            if segment[flag_index].lower() in SHELL_COMMAND_FLAGS:
                inner = segment[flag_index + 1]
                return check_command(inner, name in {"pwsh", "powershell"}, depth + 1)
    return None


def strip_heredocs(command: str) -> str:
    """bash のヒアドキュメントの本文（<<EOF から EOF の行まで）を除く。本文はコマンドではなくデータのため。"""
    lines = command.split("\n")
    kept: list[str] = []
    terminator: str | None = None
    for line in lines:
        if terminator is not None:
            if line.strip() == terminator:
                terminator = None
            continue
        kept.append(line)
        match = HEREDOC.search(line)
        if match:
            terminator = match.group(2)
    return "\n".join(kept)


def check_command(command: str, powershell: bool, depth: int = 0) -> str | None:
    """止める理由を返す。止めないときは None。"""
    text = HERE_STRING.sub("''", command)
    if not powershell:
        text = strip_heredocs(text)
    try:
        tokens = tokenize(text, powershell)
    except ValueError:
        # 引用符の対応が取れないなど。引用符を含まない素直な書き方だけを正規表現で見る
        for pattern in FALLBACK_PATTERNS:
            if pattern.search(text):
                return "git push または --no-verify"
        return None
    for segment in split_segments(tokens):
        reason = check_segment(segment, depth)
        if reason:
            return reason
    return None


def decide(raw: str) -> str | None:
    data = json.loads(raw)
    if not isinstance(data, dict):
        return None
    tool = data.get("tool_name")
    tool_input = data.get("tool_input")
    if tool not in ("Bash", "PowerShell") or not isinstance(tool_input, dict):
        return None
    command = tool_input.get("command")
    if not isinstance(command, str):
        return None
    return check_command(command, tool == "PowerShell")


SELF_TEST_BLOCK = [
    ("Bash", "git push"),
    ("Bash", "git push --force origin main"),
    ("Bash", "git commit --no-verify -m x"),
    ("Bash", "git -C path push"),
    ("Bash", "git -c push.default=current push origin main"),
    ("Bash", "cd /c/repo && git push origin feature/x"),
    ("Bash", "git status\ngit push"),
    ("Bash", "git commit -n -m x"),
    ("Bash", "git commit -nm x"),
    ("Bash", "/usr/bin/git.exe push"),
    ("Bash", "bash -c 'git push origin main'"),
    ("Bash", "git push --no-verify"),
    ("Bash", "sudo git push"),
    ("Bash", "GIT_DIR=.git git push"),
    ("Bash", "env -i A=b git push"),
    ("Bash", "timeout 10 git push"),
    ("Bash", "git commit -F - <<'EOF'\nmsg\nEOF\ngit push"),
    ("PowerShell", "git push"),
    ("PowerShell", "& git push origin main"),
    ("PowerShell", "git status; git push"),
    ("PowerShell", "git commit --no-verify -m x"),
    ("PowerShell", "pwsh -Command \"git push\""),
]
SELF_TEST_ALLOW = [
    ("Bash", "git status"),
    ("Bash", "git log --oneline"),
    ("Bash", "python scripts/quality-gates.py --quick"),
    ("Bash", "./gradlew ktlintCheck"),
    ("Bash", "git commit -m \"docs: push はユーザーが実行する\""),
    ("Bash", "git commit -m 'git push を止める'"),
    ("Bash", "git commit -m \"$(cat <<'EOF'\nfeat: git push と --no-verify を止める\n\n本文 git push\nEOF\n)\""),
    ("Bash", "grep -rn \"git push\" docs"),
    ("Bash", "grep -n push scripts/guard-git-hook.py"),
    ("Bash", "git log --grep=push"),
    ("Bash", "git commit -m -n"),
    ("Bash", "git diff -- push.txt"),
    ("Bash", "echo 'unbalanced"),
    ("Bash", "echo git push"),
    ("Bash", "git commit -q -F - <<'EOF'\nfeat: x\n\npermissions.deny に git push と --no-verify を足した。\nフックは git -C path push なども止める\ngit push\nEOF\ngit log --oneline -1"),
    ("Bash", "cat <<EOF > note.txt\ngit commit --no-verify\nEOF"),
    ("PowerShell", "git commit -m @'\nfeat: don't git push\n'@"),
    ("PowerShell", "Select-String -Pattern 'git push' -Path docs\\*.md"),
    ("PowerShell", "git log --oneline -5"),
]


def self_test() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    failures = 0
    for expected, cases in ((True, SELF_TEST_BLOCK), (False, SELF_TEST_ALLOW)):
        for tool, command in cases:
            raw = json.dumps({"tool_name": tool, "tool_input": {"command": command}})
            blocked = decide(raw) is not None
            if blocked != expected:
                failures += 1
                print(f"NG {'止めるべき' if expected else '通すべき'} {tool}: {command!r}")
    for raw in ["", "not json", "[]", json.dumps({"tool_name": "Read", "tool_input": {"file_path": "x"}})]:
        try:
            if decide(raw) is not None:
                failures += 1
                print(f"NG 異常な入力を止めた: {raw!r}")
        except (ValueError, TypeError):
            pass  # main() では例外を握って通す
    total = len(SELF_TEST_BLOCK) + len(SELF_TEST_ALLOW)
    print(f"self-test: {total} 件すべて OK" if failures == 0 else f"self-test: {failures} 件 NG")
    return 1 if failures else 0


def main() -> int:
    if sys.argv[1:] == ["--self-test"]:
        return self_test()
    try:
        raw = sys.stdin.buffer.read().decode("utf-8", errors="replace")
        reason = decide(raw)
    except Exception:  # noqa: BLE001 フック自身の異常では止めない
        return 0
    if reason is None:
        return 0
    try:
        sys.stderr.reconfigure(encoding="utf-8")
        sys.stderr.write(BLOCK_MESSAGE.format(reason=reason) + "\n")
        sys.stderr.flush()
    except Exception:  # noqa: BLE001 書けなくても止める判定は変えない
        pass
    return 2


if __name__ == "__main__":
    sys.exit(main())
