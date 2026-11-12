"""品質ゲートを 1 コマンドで流し、結果を短くまとめる。

流すコマンドは CLAUDE.md「本リポジトリの品質ゲート定義」の表と同じ（ゲートの定義の正本は CLAUDE.md の表で、
本スクリプトはそれを順に流すだけ。表を変えたら下の GRADLE_GATES・OTHER_GATES も合わせる）。

1. `./gradlew --stop`（常駐プロセスを止めてメモリを空ける。Gradle のゲートを流すときだけ）
2. Gradle の 5 ゲートを 1 回の呼び出しで流す
   `./gradlew --continue ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug`
   （`--continue` で 1 つが失敗しても残りを流し、失敗したタスク名からゲートごとの合否を出す）
3. markdownlint（`Summary: 0 issues` と、`Linting: N files` の N が
   `git ls-files --cached --others --exclude-standard "*.md"` の件数と一致すること）
4. `python scripts/validate-records.py`
5. `python scripts/make-user-guide.py --check`
6. `python scripts/scan-secrets.py`（コミット前の秘密情報スキャン）

出力は、成功したゲートは 1 行、失敗したゲートは原因の分かる抜粋だけ（全文は build/quality-gates/ のログ）、
最後にゲートごとの合否と所要時間の表。呼び出したエージェントの文脈を圧迫しないよう、出力を短く保つ。

使い方（Git Bash・PowerShell のどちらからでも同じ）:

    python scripts/quality-gates.py                       # 全ゲート
    python scripts/quality-gates.py --skip-gradle         # Gradle 以外（文書だけの変更）
    python scripts/quality-gates.py --only ktlint,detekt  # 指定したゲートだけ（--list で名前の一覧）

終了コードは、流したゲートがすべて成功で0、1 つでも失敗で1、引数の誤りで2。
Claude Code ではバックグラウンドで流さず、通常のコマンド（タイムアウト 10 分）で流す（CLAUDE.md）。
"""

import os
import pathlib
import re
import shutil
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parent.parent
LOG_DIR = ROOT / "build" / "quality-gates"
GRADLEW = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))

# ゲート名 → (Gradle のタスク, 失敗したタスク名をこのゲートに振り分けるための正規表現)
GRADLE_GATES = {
    "ktlint": (["ktlintCheck"], re.compile(r"ktlint", re.I)),
    "detekt": (["detekt"], re.compile(r"detekt", re.I)),
    "lint": (["lintDebug"], re.compile(r":lint", re.I)),
    "test": (["testDebugUnitTest", ":core:test"], re.compile(r"(?i):test|UnitTest")),
    "build": (["assembleDebug"], re.compile(r".")),  # 上のどれにも当たらない失敗（コンパイルなど）はビルドへ
}
OTHER_GATES = ["markdownlint", "records", "user-guide", "secrets"]
ALL_GATES = list(GRADLE_GATES) + OTHER_GATES
EXCERPT_LINES = 25

# Gradle の出力から原因の分かる行だけを拾う（ktlint・detekt・コンパイル・テスト・Lint の書式）
GRADLE_INTERESTING = re.compile(
    r"^e: |\.kts?:\d+|\.xml:\d+| FAILED$|^\s+(?:java|kotlin|org\.junit)\.\S*(?:Error|Exception)|"
    r"Execution failed for task|Lint found|tests completed|^\s*> (?!Task |Run with|Get more help)"
)
GRADLE_NOISE = re.compile(r"^> Task (?!.* FAILED$)")


def child_env() -> dict[str, str]:
    """子プロセスの出力を UTF-8 にそろえる（Windows の既定は cp932 のため）。"""
    env = dict(os.environ)
    env["PYTHONUTF8"] = "1"
    env["PYTHONIOENCODING"] = "utf-8"
    return env


def run(command: list[str], log_name: str) -> tuple[int, str, float]:
    """コマンドを流し、(終了コード, 出力, 秒) を返す。出力の全文はログへ残す。"""
    start = time.monotonic()
    try:
        result = subprocess.run(
            command,
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            env=child_env(),
            check=False,
        )
        code, output = result.returncode, (result.stdout or "") + (result.stderr or "")
    except OSError as exc:
        code, output = 127, f"コマンドを起動できない: {command[0]} ({exc})"
    elapsed = time.monotonic() - start
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    (LOG_DIR / f"{log_name}.log").write_text(output, encoding="utf-8")
    return code, output, elapsed


def tail(text: str, count: int = EXCERPT_LINES) -> list[str]:
    lines = [line for line in text.splitlines() if line.strip()]
    return lines[-count:]


def gradle_excerpt(output: str) -> list[str]:
    """Gradle の出力から、原因の分かる行と「What went wrong」の節だけを抜き出す。"""
    picked: list[str] = []
    lines = output.splitlines()
    for index, line in enumerate(lines):
        if line.startswith("* What went wrong"):
            for follow in lines[index + 1 : index + 6]:
                if follow.startswith("* "):
                    break
                picked.append(follow)
        elif GRADLE_INTERESTING.search(line) and not GRADLE_NOISE.search(line):
            picked.append(line)
    # 絶対パスはリポジトリからの相対にして短くする
    prefixes = (str(ROOT) + os.sep, str(ROOT).replace("\\", "/") + "/")
    for prefix in prefixes:
        picked = [line.replace(prefix, "") for line in picked]
    unique = list(dict.fromkeys(line.rstrip() for line in picked if line.strip()))
    return unique[:EXCERPT_LINES] if unique else tail(output)


def run_gradle(selected: list[str]) -> dict[str, tuple[bool, float, list[str]]]:
    """選んだ Gradle のゲートを 1 回の呼び出しで流し、ゲートごとの (成否, 秒, 抜粋) を返す。"""
    run([GRADLEW, "--stop"], "gradle-stop")
    tasks = [task for gate in selected for task in GRADLE_GATES[gate][0]]
    code, output, elapsed = run([GRADLEW, "--console=plain", "--continue", *tasks], "gradle")
    results: dict[str, tuple[bool, float, list[str]]] = {}
    if code == 0:
        return {gate: (True, elapsed, []) for gate in selected}
    failed_tasks = re.findall(r"Execution failed for task '([^']+)'", output)
    failed_tasks += re.findall(r"^> Task (\S+) FAILED$", output, re.M)
    failed_tasks = list(dict.fromkeys(failed_tasks))
    excerpt = gradle_excerpt(output)
    assigned: set[str] = set()
    for gate in selected:
        if gate == "build":
            continue
        hits = [task for task in failed_tasks if GRADLE_GATES[gate][1].search(task)]
        assigned.update(hits)
        results[gate] = (not hits, elapsed, excerpt if hits else [])
    rest = [task for task in failed_tasks if task not in assigned]
    unknown = not failed_tasks  # 失敗したのにタスク名が出ない（設定の誤り・プロセスの強制停止など）
    if "build" in selected:
        results["build"] = (not rest and not unknown, elapsed, excerpt if rest or unknown else [])
    elif rest or unknown:
        # build を選んでいないときは、振り分けられない失敗を選んだゲートすべての失敗にする
        for gate in selected:
            results[gate] = (False, elapsed, excerpt)
    if unknown:
        note = f"Gradle が終了コード {code} で終わったが、失敗したタスク名が出ていない（強制停止・設定の誤りなど）"
        for gate, (ok, seconds, lines) in list(results.items()):
            if not ok:
                results[gate] = (ok, seconds, [note, *lines])
    return {gate: results[gate] for gate in selected}


def run_markdownlint() -> tuple[bool, float, list[str]]:
    npx = shutil.which("npx")
    if npx is None:
        return False, 0.0, ["npx が見つからない（Node.js を入れる）"]
    code, output, elapsed = run(
        [npx, "markdownlint-cli2", "**/*.md", "--config", ".markdownlint-cli2.yaml"], "markdownlint"
    )
    problems: list[str] = []
    summary = re.search(r"Summary: (\d+) issues?", output)
    linting = re.search(r"Linting: (\d+) files?", output)
    listed = subprocess.run(
        ["git", "-c", "core.quotepath=false", "ls-files", "--cached", "--others", "--exclude-standard", "*.md"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    expected = len([line for line in listed.stdout.splitlines() if line])
    if code != 0 or summary is None or summary.group(1) != "0":
        problems += [line for line in output.splitlines() if re.search(r"\.md:\d+", line)][:EXCERPT_LINES]
        problems.append(summary.group(0) if summary else f"Summary が出ていない（終了コード {code}）")
    if linting is None or int(linting.group(1)) != expected:
        actual = linting.group(1) if linting else "不明"
        problems.append(f"対象の件数が合わない（Linting {actual} files、git ls-files の *.md は {expected} 件）")
    return not problems, elapsed, problems if problems else []


def run_python(gate: str, args: list[str]) -> tuple[bool, float, list[str]]:
    code, output, elapsed = run([sys.executable, *args], gate)
    return code == 0, elapsed, [] if code == 0 else tail(output)


def run_other(gate: str) -> tuple[bool, float, list[str]]:
    if gate == "markdownlint":
        return run_markdownlint()
    if gate == "records":
        return run_python(gate, ["scripts/validate-records.py"])
    if gate == "user-guide":
        return run_python(gate, ["scripts/make-user-guide.py", "--check"])
    return run_python(gate, ["scripts/scan-secrets.py"])


def report(gate: str, ok: bool, seconds: float, lines: list[str]) -> None:
    print(f"[{'OK' if ok else 'NG'}] {gate} ({seconds:.0f}s)", flush=True)
    if not ok:
        for line in lines:
            print(f"    {line[:200]}")
        log = "gradle" if gate in GRADLE_GATES else gate
        print(f"    全文: build/quality-gates/{log}.log", flush=True)


def parse_args(argv: list[str]) -> tuple[list[str], bool] | None:
    selected = list(ALL_GATES)
    skip_gradle = False
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg == "--skip-gradle":
            skip_gradle = True
        elif arg == "--list":
            print("ゲート名: " + ", ".join(ALL_GATES))
            sys.exit(0)
        elif arg == "--only" and index + 1 < len(argv):
            index += 1
            names = [name.strip() for name in argv[index].split(",") if name.strip()]
            unknown = [name for name in names if name not in ALL_GATES]
            if unknown or not names:
                print(f"知らないゲート名: {', '.join(unknown) or '(空)'}（--list で一覧）")
                return None
            selected = [gate for gate in ALL_GATES if gate in names]
        else:
            print(f"知らない引数: {arg}（使い方は scripts/quality-gates.py の先頭）")
            return None
        index += 1
    return selected, skip_gradle


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parsed = parse_args(sys.argv[1:])
    if parsed is None:
        return 2
    selected, skip_gradle = parsed
    gradle = [gate for gate in selected if gate in GRADLE_GATES and not skip_gradle]
    results: dict[str, tuple[bool, float, list[str]] | None] = {}

    if gradle:
        print(f"Gradle: {' '.join(task for gate in gradle for task in GRADLE_GATES[gate][0])} を流す", flush=True)
        for gate, result in run_gradle(gradle).items():
            results[gate] = result
            report(gate, *result)
    for gate in selected:
        if gate in GRADLE_GATES and skip_gradle:
            results[gate] = None
        elif gate in OTHER_GATES:
            results[gate] = run_other(gate)
            report(gate, *results[gate])

    print("")
    print("| ゲート | 結果 | 秒 |")
    print("| --- | --- | --- |")
    for gate in selected:
        result = results[gate]
        if result is None:
            print(f"| {gate} | SKIP | - |")
        else:
            seconds = f"{result[1]:.0f}" + ("（Gradle 一括）" if gate in GRADLE_GATES else "")
            print(f"| {gate} | {'OK' if result[0] else 'NG'} | {seconds} |")
    failed = [gate for gate, result in results.items() if result is not None and not result[0]]
    print("")
    print("全ゲート成功" if not failed else f"失敗 {len(failed)} 件: {', '.join(failed)}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
