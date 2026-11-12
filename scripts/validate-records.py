"""記録ファイルのYAML検証（品質ゲート）。

`docs/records/managed/BACKLOG.md` と `docs/records/managed/EXECUTE.md` の
`COPILOT_RECORDS:BEGIN` 〜 `COPILOT_RECORDS:END` の間を抽出し、YAMLとして読み込めることを
確認する（手順の正本は docs/records/spec/FORMAT.md「YAMLとしての体裁」）。
1件でも書式が壊れるとファイル全体がパースできなくなるため、記録ファイルを更新したら実行する。

あわせて、FORMAT.md が定める体裁のうち機械的に確認できるものを検査する。

- 改行コードがLFであること（PowerShellでの一括置換によるCRLF化を検出する）
- BACKLOG.md の必須キーが揃っていること、`優先度`・`状態`・`区分` が許容値であること、`担当` が空でないこと
- BACKLOG.md の `id` が重複していないこと
- BACKLOG.md の `依存` が配列で、BACKLOG.md に存在する `id` だけを指していること
  （完了項目を削除したときに、他タスクの `依存` に削除済みの `id` が残る事故を検出する）
- EXECUTE.md の必須キー（`date`・`summary`・`details` の `変更内容`・`変更ファイル`・`検証コマンド`・`検証結果`）が
  揃っていること、`date` が `YYYY-MM-DD HH:mm`、`変更ファイル` と `関連ID` が配列であること
- BACKLOG.md の `id` が EXECUTE.md の `関連ID` で使われていないこと（採番の衝突の検出）。
  ただし `状態: 進行中` の項目は、途中までの実施を記録したものとして除く

使い方:

    python scripts/validate-records.py

終了コードは、すべて成功で0、いずれかが失敗で1。PyYAML が必要（`pip install pyyaml`）。
"""

import io
import re
import sys

RECORD_FILES = [
    "docs/records/managed/BACKLOG.md",
    "docs/records/managed/EXECUTE.md",
]
BEGIN_MARKER = "<!-- COPILOT_RECORDS:BEGIN -->"
END_MARKER = "<!-- COPILOT_RECORDS:END -->"
FENCE_PATTERN = re.compile(r"^```yaml\s*$|^```\s*$", re.M)

BACKLOG_REQUIRED_KEYS = ["id", "区分", "タスク内容", "優先度", "状態", "担当", "完了条件", "依存"]
BACKLOG_PRIORITIES = ["P1", "P2", "P3"]
BACKLOG_STATES = ["未着手", "進行中", "ブロック", "要確認", "完了"]
BACKLOG_CATEGORIES = ["不具合", "UX改善", "機能追加", "性能", "品質ゲート", "人手検証"]
EXECUTE_REQUIRED_KEYS = ["date", "summary", "details"]
EXECUTE_REQUIRED_DETAIL_KEYS = ["変更内容", "変更ファイル", "検証コマンド", "検証結果"]
EXECUTE_DATE_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$")


def load_records(path: str, errors: list[str]) -> list | None:
    """マーカー内をYAMLとして読み込む。失敗した場合は errors へ追記して None を返す。"""
    raw = io.open(path, encoding="utf-8", newline="").read()
    if "\r" in raw:
        errors.append(f"{path}: 改行コードがCRLFになっている（FORMAT.md の規定はLF）")
    if BEGIN_MARKER not in raw or END_MARKER not in raw:
        errors.append(f"{path}: {BEGIN_MARKER} / {END_MARKER} が見つからない")
        return None
    body = raw.split(BEGIN_MARKER)[1].split(END_MARKER)[0]
    try:
        import yaml
    except ImportError:
        errors.append("PyYAML が見つからない。pip install pyyaml を実行する")
        return None
    try:
        return yaml.safe_load(FENCE_PATTERN.sub("", body))
    except Exception as exc:  # noqa: BLE001 - YAMLの失敗理由をそのまま見せる
        errors.append(f"{path}: YAMLとして読み込めない - {exc}")
        return None


def check_backlog_keys(records: list, errors: list[str]) -> None:
    """BACKLOG.md の必須キーと、許容値が決まっているキーの値を確認する。"""
    for record in records:
        task_id = record.get("id", "(idなし)")
        for key in BACKLOG_REQUIRED_KEYS:
            if key not in record:
                errors.append(f"BACKLOG.md {task_id}: 必須キー `{key}` が無い")
        priority = record.get("優先度")
        if priority is not None and priority not in BACKLOG_PRIORITIES:
            errors.append(f"BACKLOG.md {task_id}: 優先度 `{priority}` は許容値ではない")
        state = record.get("状態")
        if state is not None and state not in BACKLOG_STATES:
            errors.append(f"BACKLOG.md {task_id}: 状態 `{state}` は許容値ではない")
        category = record.get("区分")
        if category is not None and category not in BACKLOG_CATEGORIES:
            errors.append(f"BACKLOG.md {task_id}: 区分 `{category}` は許容値ではない（FORMAT.md の表の 6 値から選ぶ）")
        if "担当" in record and not str(record.get("担当") or "").strip():
            errors.append(f"BACKLOG.md {task_id}: 担当 が空")


def check_backlog_duplicate_ids(records: list, errors: list[str]) -> None:
    """BACKLOG.md の `id` が重複していないことを確認する。"""
    seen: set = set()
    for record in records:
        task_id = record.get("id")
        if task_id is None:
            continue
        if task_id in seen:
            errors.append(f"BACKLOG.md {task_id}: id が重複している")
        seen.add(task_id)


def check_execute_keys(records: list, errors: list[str]) -> None:
    """EXECUTE.md の必須キーと `date` の形式、配列で書くキーの型を確認する。"""
    for index, record in enumerate(records, start=1):
        label = f"EXECUTE.md {index} 件目（date {record.get('date', 'なし')}）"
        for key in EXECUTE_REQUIRED_KEYS:
            if key not in record:
                errors.append(f"{label}: 必須キー `{key}` が無い")
        date = record.get("date")
        if date is not None and not EXECUTE_DATE_PATTERN.match(str(date)):
            errors.append(f"{label}: date `{date}` は YYYY-MM-DD HH:mm の形式ではない")
        details = record.get("details")
        if not isinstance(details, dict):
            if "details" in record:
                errors.append(f"{label}: details はキーと値の組で書く")
            continue
        for key in EXECUTE_REQUIRED_DETAIL_KEYS:
            if key not in details:
                errors.append(f"{label}: 必須キー `details.{key}` が無い")
        for key in ["変更ファイル", "関連ID"]:
            if key in details and not isinstance(details[key], list):
                errors.append(f"{label}: details.{key} は配列で書く")


def collect_related_ids(records: list) -> set:
    """EXECUTE.md の `関連ID` に書かれた `id` を集める。"""
    related: set = set()
    for record in records:
        details = record.get("details")
        if isinstance(details, dict) and isinstance(details.get("関連ID"), list):
            related.update(details["関連ID"])
    return related


def check_id_collisions(backlog: list, related_ids: set, errors: list[str]) -> None:
    """BACKLOG.md の `id` が EXECUTE.md で実施済みとして使われていないことを確認する（採番の衝突）。"""
    for record in backlog:
        task_id = record.get("id")
        if task_id in related_ids and record.get("状態") != "進行中":
            errors.append(
                f"BACKLOG.md {task_id}: EXECUTE.md の 関連ID で使われた番号と重なる"
                "（新しい id は BACKLOG と EXECUTE の両方で未使用の番号にする）"
            )


def check_backlog_dependencies(records: list, errors: list[str]) -> None:
    """BACKLOG.md の `依存` が配列で、存在する `id` だけを指していることを確認する。"""
    ids = {record.get("id") for record in records}
    for record in records:
        task_id = record.get("id", "(idなし)")
        depends = record.get("依存")
        if depends is None:
            continue
        if not isinstance(depends, list):
            errors.append(f"BACKLOG.md {task_id}: 依存 は配列で書く（依存が無い場合は []）")
            continue
        for dep in depends:
            if dep not in ids:
                errors.append(
                    f"BACKLOG.md {task_id}: 依存 `{dep}` は BACKLOG.md に存在しない"
                    "（完了して削除した id は依存から外し、空になったら [] にする）"
                )


def main() -> int:
    errors: list[str] = []
    loaded: dict[str, list] = {}
    for path in RECORD_FILES:
        records = load_records(path, errors)
        if records is None:
            continue
        print(f"{path}: OK ({len(records)} records)")
        loaded[path] = records
        if path.endswith("BACKLOG.md"):
            check_backlog_keys(records, errors)
            check_backlog_duplicate_ids(records, errors)
            check_backlog_dependencies(records, errors)
        else:
            check_execute_keys(records, errors)
    backlog = loaded.get(RECORD_FILES[0])
    execute = loaded.get(RECORD_FILES[1])
    if backlog is not None and execute is not None:
        check_id_collisions(backlog, collect_related_ids(execute), errors)
    if errors:
        print("")
        for error in errors:
            print(f"NG: {error}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
