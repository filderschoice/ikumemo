# CLAUDE.md

Claude Code 固有の差分と追加規約です。全エージェント共通規約の正本（`.github/copilot-instructions.md`）と
ガードレールは下記の `@import` で同時に読み込まれます。共通規約と矛盾する場合は本ファイルを優先します。
その他の参照先と読むタイミングは、共通規約「参照するドキュメント」を参照してください。

@rules/guardrails-unified.v1.md
@.github/copilot-instructions.md

## 共通規約に対する Claude Code 固有の差分（MUST）

| 項目 | Claude Code での差分 |
| --- | --- |
| git操作 | 自律ループ実行モード中の作業ブランチへの `git add` / `git commit` のみ実行してよい。`git push` はモードを問わず常にユーザーが実行する |
| PR説明文・コードレビュー・PR作成 | `.github/instructions/pr.instructions.md` を自動適用する機構が無いため、Skill `pr-create` の手順で同ファイルを**明示的に読んでから**従う |

## 本リポジトリの品質ゲート定義（MUST）

全エージェント共通で使用する正本です。`区分: 人手検証` のBACKLOGタスクは合否判定から除外します。

| ゲート | コマンド | 合否基準 |
| --- | --- | --- |
| コードスタイル（ktlint） | `./gradlew ktlintCheck` | 終了コード0 |
| 静的解析（detekt） | `./gradlew detekt` | 終了コード0（`config/detekt/detekt.yml` の `maxIssues: 0`） |
| Android Lint | `./gradlew lintDebug` | 終了コード0 |
| 単体テスト | `./gradlew testDebugUnitTest :core:test` | 終了コード0（`core` / `mobile` 全モジュール。`mobile` は debug 側だけ流す。遅いテストは下記） |
| ビルド（型検査を兼ねる） | `./gradlew assembleDebug` | 終了コード0 |
| 脆弱性チェック | (未導入) | Gradle の依存脆弱性スキャン（`dependencyCheck` 等）は導入しない（sesami-wear に合わせたオーナー判断、2026-10-24）。導入した場合は本節を更新する |
| Markdown の静的解析 | `npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml` | `Summary: 0 issues`、かつ出力の `Linting: N files` が `git ls-files --cached --others --exclude-standard "*.md"` の件数と一致（対象漏れの検出） |
| 記録ファイルの検証 | `python scripts/validate-records.py`（PyYAMLが必要） | 終了コード0 |
| 使い方の Web 版 | `python scripts/make-user-guide.py --check` | 終了コード0（`docs/USER_GUIDE.md` がアプリ内の使い方の正本 `mobile/src/main/assets/help/` と一致。正本を直したら `python scripts/make-user-guide.py` で作り直す） |
| 秘密情報スキャン | ステージ済みの変更がある場面（コミット前）は `git add` の後に `python scripts/scan-secrets.py --require-changes`、ステージしない場面（対話実行・Copilot・一括実行）は `python scripts/scan-secrets.py --include-unstaged`、PR作成前・ループの最後は `python scripts/scan-secrets.py --range main...HEAD --require-changes`（標準ライブラリのみ。未コミットの変更とコミット範囲の両方を確かめるときは2回に分けて実行する。ファイルの削除だけをコミットする場面は検査対象が空になるため `--require-changes` を外す） | 終了コード0（検出時は値を出さずにファイル・行・規則名を表示し1。`--require-changes` 付きで検査対象が空のとき、git の実行に失敗したときは2。guardrails 12.1 のコミット前のスキャン） |
| 一括実行 | `python scripts/quality-gates.py` | 終了コード0（上の全ゲートを順に流す。下記） |

- `python scripts/quality-gates.py` は上の表と同じコマンドを順に流す（`./gradlew --stop` → Gradle の5ゲートを1回の呼び出しで
  → markdownlint（件数の照合まで）→ 記録ファイル → 使い方の Web 版 → 秘密情報スキャン（`--include-unstaged`））。ゲートの定義の正本は上の表で、
  表を変えたらスクリプトの `GRADLE_GATES`・`OTHER_GATES` も合わせる。出力は成功したゲートが1行、失敗したゲートは原因の抜粋だけ
  （全文は `build/quality-gates/*.log`）、最後に合否の表。文書だけの変更は `--skip-gradle`、一部だけは `--only ktlint,detekt`
  （名前は `--list`）。Git Bash・PowerShell のどちらからでも `python` で起動できる
- Gradle 系の5ゲートは `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug` で一度に実行できる。
  ktlint の違反は `./gradlew ktlintFormat` で自動修正できる（品質ゲートには含めない）
- いつ何を流すか（オーナー判断、2026-11-13。正本は本節）: 自律ループの各イテレーションは、コードを触ったら軽い検証
  `python scripts/quality-gates.py --quick --tests "*<変更に関係するテストクラス>"`（ktlint・detekt・コンパイル・指定したテストだけ。
  Android Lint・全テスト・assembleDebug は流さない）、文書・スクリプトだけなら `--skip-gradle`。フル検証（引数なし、上の表のすべて）は
  ループの最後と、ループを止める前に 1 回流す。軽い検証の成功はフル検証の成功ではない（最後の行で区別する）
- `mobile` の release 側の単体テスト（`testReleaseUnitTest`）は debug 側と同じ内容のため流さない。時間のかかるテスト（`@Category(SlowTest::class)`、
  例 2 万件の検索性能）は通常は外し、性能に関わる変更の時とマイルストーンの区切りに
  `./gradlew testDebugUnitTest -PslowTests` で流す（オーナー判断、2026-10-25）
- スクリーンショットテスト（Roborazzi、遅いテスト扱い）は見た目に関わる変更の時とマイルストーンの区切りに、画面ごとに
  `./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"`
  （`calendar`・`reminder` も同様）で終了コード0。判断基準・差分画像の読み方・記録し直し（`-Proborazzi.test.record=true`）は Skill `screenshot-verify`
- Claude Code では Gradle 系のゲートをバックグラウンドで流さず、通常のコマンド（タイムアウト 10 分）で流す。PC のメモリが少なく、
  バックグラウンドのコマンドはセッションの待機中に Claude Code に強制停止されるため。流す前に `./gradlew --stop` で常駐プロセスを止める
- 本リポジトリに CI はなく（sesami-wear に合わせたオーナー判断）、上記コマンドのローカル実行が唯一の品質ゲート
- 実機・エミュレータでの確認は自動実行の対象外とし、`BACKLOG.md` へ `区分: 人手検証` として記録する

## 記録ファイルの権限設定（MUST）

`docs/records/managed/` 配下の3ファイルは `.claude/settings.json` の `permissions.allow` により権限プロンプトなしで
編集できます（妥当性はコミット前の差分確認と `FORMAT.md` 準拠で担保する）。それ以外の編集は確認を挟みます。
3ファイルを編集すると、`.claude/settings.json` の `hooks` に登録した PostToolUse フック
（`.claude/hooks/validate-records-after-edit.py`）が `scripts/validate-records.py` を実行します。検証に失敗したと
伝えられたら、一連の編集を終えた時点で `FORMAT.md` に沿って直します（フックは編集を取り消しません）。

<!-- 配布バンドルの .claude/settings.json は記録ファイル関連の許可のみを含む。本リポジトリ固有の許可を追加する
場合は、確認を挟まずに編集させてよい範囲かを判断してから permissions.allow へ追記する。 -->

## 自律ループ実行モード（Loop Engineering）

人の応答を待たずに複数イテレーションを連続実行する、Claude Code 固有の運用モードです（Copilot は対象外）。

<!-- 本モードを採用しない場合は、本節と .claude/skills/autonomous-loop/ を削除する
（採否の判断は docs/guidelines/ADOPTION.md「1.2 自律ループ実行モードの採否」）。 -->

- **適用条件（MUST）**: ユーザーが開始を明示的に指示していること。指示されたスコープ内でのみ有効で、
  ループ終了と同時に通常の対話モードへ戻る。満たさない場合は `git add` / `git commit` を実行してはならない
- **起動方法（MUST）**: Skill `autonomous-loop` を起動し、その手順に従う。手順を記憶や推測で代用しない
- 統制要件（禁止操作・停止条件・秘密情報の取り扱い。サブエージェントへの委任時は 12.7）は常時読み込みの
  `rules/guardrails-unified.v1.md` セクション12、ブランチ・コミット規約は `CONTRIBUTING.md`「自律ループ実行モードの
  ブランチ・コミット規約」が正本

## 保守

共通規約の変更は `.github/copilot-instructions.md` へ、Claude Code 固有の変更は本ファイルへ反映します
（役割分担は `CONTRIBUTING.md`「エージェント指示ファイルの構成規約」）。

<!-- 本ファイルは配布元の templates/CLAUDE-template.md を基にしている。配布元の更新へ追従する場合は、
CHANGELOG.md で差分を確認し、本リポジトリ固有に書き換えた節（品質ゲート定義など）を上書きしないようマージする。 -->
