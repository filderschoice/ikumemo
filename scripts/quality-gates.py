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
ktlint・detekt の失敗で、違反の出たファイルが作業ツリーに存在しないときは「キャッシュの古い結果の疑い」と出し、
`./gradlew <失敗したタスク> --rerun` の対処を示す（ビルドキャッシュから消したファイルの違反が復元される事故への対策、BL-128）。

使い方（Git Bash・PowerShell のどちらからでも同じ）:

    python scripts/quality-gates.py                       # 全ゲート
    python scripts/quality-gates.py --skip-gradle         # Gradle 以外（文書だけの変更）
    python scripts/quality-gates.py --only ktlint,detekt  # 指定したゲートだけ（--list で名前の一覧）
    python scripts/quality-gates.py --quick --tests "*DataViewModelTest"  # 軽い検証

軽い検証（--quick）は、Gradle を ktlint・detekt・コンパイル（core と mobile の main とテスト。
`:core:compileTestKotlin :mobile:compileDebugUnitTestKotlin`）と、--tests で渡したテストだけにする（Android Lint・全テスト・
assembleDebug は流さず SKIP と出す）。--tests はくり返し渡せ、Gradle の --tests と同じ書き方で、クラス名と同じ名前の
.kt を core/src/test と mobile/src/test* から探して core と mobile のどちらのテストかを決める（見つからなければ失敗）。
Gradle 以外の 4 ゲートは同じ。最後の行で軽い検証と分かるようにする（フル検証の成功と取り違えないため）。

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

# 軽い検証（--quick）の Gradle のゲート。test のタスクは --tests から組み立てる。compile は振り分けられない失敗の受け皿
QUICK_GATES = {
    "ktlint": GRADLE_GATES["ktlint"],
    "detekt": GRADLE_GATES["detekt"],
    "compile": ([":core:compileTestKotlin", ":mobile:compileDebugUnitTestKotlin"], re.compile(r".")),
    "test": ([], re.compile(r":core:test$|:testDebugUnitTest$")),
}
QUICK_SKIPPED = ["lint", "build"]  # 軽い検証では流さない
QUICK_ORDER = ["ktlint", "detekt", "compile", "test", *QUICK_SKIPPED, *OTHER_GATES]
TEST_DIRS = {
    ":mobile:testDebugUnitTest": ["mobile/src/test", "mobile/src/testDebug"],
    ":core:test": ["core/src/test"],
}

# Gradle の出力から原因の分かる行だけを拾う（ktlint・detekt・コンパイル・テスト・Lint の書式）
GRADLE_INTERESTING = re.compile(
    r"^e: |\.kts?:\d+|\.xml:\d+| FAILED$|^\s+(?:java|kotlin|org\.junit)\.\S*(?:Error|Exception)|"
    r"Execution failed for task|Lint found|tests completed|^\s*> (?!Task |Run with|Get more help)"
)
GRADLE_NOISE = re.compile(r"^> Task (?!.* FAILED$)")

# 違反の行に出る Kotlin のファイルのパス（絶対・相対。Windows のドライブ名も拾う）
VIOLATION_PATH = re.compile(r"((?:[A-Za-z]:)?[^\s:'\"(]+\.kts?):\d+")
# ビルドキャッシュ（org.gradle.caching=true）から古い結果が戻りうる、ソースを検査するゲート
CACHED_CHECK_GATES = {"ktlint", "detekt"}


def missing_violation_files(output: str) -> list[str]:
    """Gradle の出力の違反の行に出たのに、作業ツリーに存在しないファイルを返す（リポジトリからの相対）。

    ファイルを消した後もキャッシュ（FROM-CACHE）から違反入りの結果が復元される事故の検出に使う（BL-128）。
    """
    missing: list[str] = []
    for raw in VIOLATION_PATH.findall(output):
        path = pathlib.Path(raw)
        full = path if path.is_absolute() else ROOT / path
        if not full.exists():
            try:
                shown = full.resolve().relative_to(ROOT).as_posix()
            except ValueError:
                shown = raw
            missing.append(shown)
    return list(dict.fromkeys(missing))


def stale_cache_note(gate: str, tasks: list[str], missing: list[str]) -> list[str]:
    """存在しないファイルの違反で失敗したゲートに、キャッシュの古い結果の疑いと対処を示す行を返す。"""
    if gate not in CACHED_CHECK_GATES or not tasks or not missing:
        return []
    shown = ", ".join(missing[:3]) + (f" ほか {len(missing) - 3} 件" if len(missing) > 3 else "")
    return [
        f"キャッシュの古い結果の疑い: 存在しないファイルの違反が出ている（{shown}）",
        f"  対処: ./gradlew {' '.join(tasks)} --rerun で該当タスクだけ流し直す（ビルドキャッシュを使わずに検査し直す）",
    ]


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


def resolve_tests(patterns: list[str]) -> tuple[list[str], list[str]]:
    """--tests の各パターンを core・mobile のテストのタスクへ振り分け、(Gradle の引数, 見つからないパターン) を返す。"""
    found: dict[str, list[str]] = {task: [] for task in TEST_DIRS}
    missing: list[str] = []
    for pattern in patterns:
        classes = [part.strip("*") for part in pattern.split(".") if re.fullmatch(r"[A-Z]\w*", part.strip("*"))]
        hits = [
            task
            for task, dirs in TEST_DIRS.items()
            if classes and any(any((ROOT / d).rglob(f"{classes[-1]}.kt")) for d in dirs)
        ]
        if not hits:
            missing.append(pattern)
        for task in hits:
            found[task].append(pattern)
    args: list[str] = []
    for task, names in found.items():
        if names:
            args += [task, *[item for name in names for item in ("--tests", name)]]
    return args, missing


def run_gradle(
    selected: list[str], gates: dict | None = None, fallback: str = "build"
) -> dict[str, tuple[bool, float, list[str]]]:
    """選んだ Gradle のゲートを 1 回の呼び出しで流し、ゲートごとの (成否, 秒, 抜粋) を返す。

    gates はゲート名 → (タスク, 失敗の振り分け)。fallback はどのゲートにも当たらない失敗を受けるゲート。
    """
    gates = GRADLE_GATES if gates is None else gates
    run([GRADLEW, "--stop"], "gradle-stop")
    tasks = [task for gate in selected for task in gates[gate][0]]
    code, output, elapsed = run([GRADLEW, "--console=plain", "--continue", *tasks], "gradle")
    results: dict[str, tuple[bool, float, list[str]]] = {}
    if code == 0:
        return {gate: (True, elapsed, []) for gate in selected}
    failed_tasks = re.findall(r"Execution failed for task '([^']+)'", output)
    failed_tasks += re.findall(r"^> Task (\S+) FAILED$", output, re.M)
    failed_tasks = list(dict.fromkeys(failed_tasks))
    excerpt = gradle_excerpt(output)
    missing = missing_violation_files(output)
    assigned: set[str] = set()
    for gate in selected:
        if gate == fallback:
            continue
        hits = [task for task in failed_tasks if gates[gate][1].search(task)]
        assigned.update(hits)
        results[gate] = (not hits, elapsed, stale_cache_note(gate, hits, missing) + excerpt if hits else [])
    rest = [task for task in failed_tasks if task not in assigned]
    unknown = not failed_tasks  # 失敗したのにタスク名が出ない（設定の誤り・プロセスの強制停止など）
    if fallback in selected:
        results[fallback] = (not rest and not unknown, elapsed, excerpt if rest or unknown else [])
    elif rest or unknown:
        # 受け皿のゲートを選んでいないときは、振り分けられない失敗を選んだゲートすべての失敗にする
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
        log = "gradle" if gate in GRADLE_GATES or gate in QUICK_GATES else gate
        print(f"    全文: build/quality-gates/{log}.log", flush=True)


def parse_args(argv: list[str]) -> tuple[list[str], bool, list[str] | None] | None:
    """(選んだゲート, --skip-gradle, 軽い検証の --tests の一覧。軽い検証でなければ None) を返す。誤りは None。"""
    selected = list(ALL_GATES)
    skip_gradle = False
    quick = False
    tests: list[str] = []
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg == "--skip-gradle":
            skip_gradle = True
        elif arg == "--quick":
            quick = True
        elif arg == "--tests" and index + 1 < len(argv):
            index += 1
            tests.append(argv[index])
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
    if tests and not quick:
        print("--tests は --quick と一緒に使う")
        return None
    if quick and (skip_gradle or selected != ALL_GATES):
        print("--quick は --only・--skip-gradle と一緒に使えない")
        return None
    return selected, skip_gradle, tests if quick else None


def run_quick_gradle(tests: list[str]) -> dict[str, tuple[bool, float, list[str]] | None]:
    """軽い検証の Gradle を流す。流さなかったゲートは None（SKIP）。"""
    test_args, missing = resolve_tests(tests)
    gates = dict(QUICK_GATES)
    gates["test"] = (test_args, QUICK_GATES["test"][1])
    selected = ["ktlint", "detekt", "compile"] + (["test"] if test_args else [])
    print(f"Gradle（軽い検証）: {' '.join(task for gate in selected for task in gates[gate][0])} を流す", flush=True)
    results: dict[str, tuple[bool, float, list[str]] | None] = dict(run_gradle(selected, gates, "compile"))
    results.update({gate: None for gate in QUICK_SKIPPED})
    if missing:
        note = [f"テストのクラスが見つからない: {name}（core/src/test・mobile/src/test* で <クラス名>.kt を探した）" for name in missing]
        ok, seconds, lines = results.get("test") or (True, 0.0, [])
        results["test"] = (False, seconds, note + lines)
    elif not test_args:
        results["test"] = None
    for gate in ["ktlint", "detekt", "compile", "test"]:
        if results[gate] is not None:
            report(gate, *results[gate])
    return results


def skip_reason(gate: str, quick: bool) -> str:
    if not quick:
        return "SKIP"
    return "SKIP（--tests の指定なし）" if gate == "test" else "SKIP（軽い検証）"


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parsed = parse_args(sys.argv[1:])
    if parsed is None:
        return 2
    selected, skip_gradle, tests = parsed
    quick = tests is not None
    gradle = [gate for gate in selected if gate in GRADLE_GATES and not skip_gradle]
    results: dict[str, tuple[bool, float, list[str]] | None] = {}

    if quick:
        selected = QUICK_ORDER
        results.update(run_quick_gradle(tests))
    elif gradle:
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
            print(f"| {gate} | {skip_reason(gate, quick)} | - |")
        else:
            seconds = f"{result[1]:.0f}" + ("（Gradle 一括）" if gate in GRADLE_GATES or gate in QUICK_GATES else "")
            print(f"| {gate} | {'OK' if result[0] else 'NG'} | {seconds} |")
    failed = [gate for gate, result in results.items() if result is not None and not result[0]]
    print("")
    if quick:
        status = "成功" if not failed else f"失敗 {len(failed)} 件: {', '.join(failed)}"
        print(f"軽い検証の結果: {status}（フル検証ではない。Android Lint・全テスト・assembleDebug は流していない）")
    else:
        print("全ゲート成功" if not failed else f"失敗 {len(failed)} 件: {', '.join(failed)}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
