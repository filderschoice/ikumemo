# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-10-24（PLAN.md のタスク分解）

`PLAN.md`（開発計画書）のマイルストーン M0〜M10 を `docs/records/managed/BACKLOG.md` のタスクへ分解しました。

- M0（準備）〜M8（仕上げ）の実装タスクと、実機確認・Play Console・GitHub 設定などオーナーが行う作業
  （`区分: 人手検証`）を登録
- オーナー指示により sesami-wear に合わせ、CI（GitHub Actions）と Dependabot は導入しない。PLAN M0 の完了条件
  「CI でビルドされる」はローカルの品質ゲートで判定する
- LICENSE の配置は PLAN D-6 によりオーナーが行う（BL-007）
- `PLAN.md` の冒頭へ `<!-- markdownlint-disable-file -->` を追加（オーナー指示）。PLAN は D-16 により開発開始後は
  本文を更新しない資料のため、書式の指摘は直さず Markdown の静的解析の対象から外す

## 2026-10-25（品質ゲートの確定）

`CLAUDE.md`「本リポジトリの品質ゲート定義」の `replace-me` を、sesami-wear と同じ Gradle 系の5ゲート
（`ktlintCheck` / `detekt` / `lintDebug` / `testDebugUnitTest test` / `assembleDebug`）で確定しました（BL-002）。

- 脆弱性チェックは導入しない（オーナー判断）。行は残し `(未導入)` と理由を記載
- CI を持たず、ローカル実行が唯一の品質ゲートである旨と、人手検証の扱いを追記
- 導入時の記入方法のコメントは、記入完了に伴い削除

## 2026-10-25（リポジトリ文書の整備）

sesami-wear に合わせてリポジトリ文書を作成しました（BL-005）。

- `README.md`（前提環境・ビルド・テスト・リリースビルドの手順、プロジェクト構成）、`NOTICE`、`SECURITY.md`
  （報告窓口は GitHub Security Advisories とメール。オーナー指示）、`CONTRIBUTING.local.md`
- 外部からの Pull Request は受け付けない（オーナー指示、sesami-wear と同じ）。PLAN §12 の「コントリビューションを
  受けやすくする」は、Issue での報告・要望の受け付けと設計・ビルド手順のドキュメント化で満たす
- `.github/ISSUE_TEMPLATE/`（不具合・要望・質問・config）。ラベルは PLAN §7.3 の `type:` / `from:` に合わせた
  （GitHub 上のラベル作成はオーナー作業として BL-009 に追記）
- `.github/CODEOWNERS` の配布元プレースホルダを `@filderschoice` に置き換え

## 2026-10-25（ワイヤーフレームの作成）

`docs/design/WIREFRAME.md` を追加しました（BL-006）。PLAN §3 の画面構成をもとに、ホーム・追加シート・編集シート・
お店を選ぶシート・カレンダー・おしらせ・期限の設定・せってい・きせかえ・はじめての案内の配置と、
その場のガイドで照らす場所（目印）を整理しました。README.md の関連ドキュメントへリンクを追加しました。

## 2026-10-25（設計書の作成）

`docs/records/managed/DESIGN.md` に、M0 で実装した内容（モジュール構成、デザインシステム、バージョン管理と
リリース署名、品質ゲート、オーナー判断による運用制約）を再実装用の設計書としてまとめました。

## 2026-10-25（LICENSE の配置）

リポジトリ直下に Apache License 2.0 の `LICENSE` を置きました（BL-007）。PLAN D-6 ではオーナーが配置するとしていましたが、
オーナーの指示で自律ループのタスクに変更しました。

- 本文は apache.org 配布の `LICENSE-2.0.txt` と同一（MD5 `3b83ef96387f14655fc854ddc3c6bd57` で照合）。
  付録の著作権行は雛形のまま残し、著作権表示は `NOTICE` に書く
- README.md・SECURITY.md・NOTICE からの `LICENSE` へのリンクが有効になった

## 2026-10-25（LICENSE と公式本文の照合）

`LICENSE` を apache.org 公式の <https://www.apache.org/licenses/LICENSE-2.0.txt>（HTTP 200 で取得）と照合し、
バイト単位で一致することを確認しました（BL-092）。`LICENSE` の変更はありません。

- サイズ 11,358 バイト、MD5 `3b83ef96387f14655fc854ddc3c6bd57`、
  SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`（`cmp` で差分なし）

## 2026-10-25（実施記録の日時の修正）

`docs/records/managed/EXECUTE.md` の日時を日本時間（JST）の実際の時刻へ直しました（オーナー指示）。

- 自律ループで足した記録（BL-010〜BL-092）は、実際より先の時刻になっていたため、対応するコミットの日時に合わせた
- main から引き継いだ M0 の 3 件（BL-001・BL-003・BL-004）は UTC で書かれていたとみなし、9 時間足した（オーナー判断）
- BL-032 のコミットで BL-031 の記録を誤って重ねて追記し、BL-032 の記録が無かったため、重なった 1 件をコミットの内容から
  書き直した BL-032 の記録へ置き換えた

## 2026-10-25（品質ゲートの高速化）

`CLAUDE.md`「本リポジトリの品質ゲート定義」の単体テストのコマンドを見直しました（オーナー判断、BL-094）。

- `testDebugUnitTest test` を `testDebugUnitTest :core:test` に変更。`test` は mobile の release 側の単体テストも流し、
  debug と同じテストを 2 回動かしていたため
- 時間のかかるテスト（`@Category(SlowTest::class)`、例 2 万件の検索性能）は通常のゲートから外し、性能に関わる変更の時と
  マイルストーンの区切りに `./gradlew testDebugUnitTest -PslowTests` で流す
- `gradle.properties` で Gradle のビルドキャッシュを有効化
- 並列実行と Gradle の常駐プロセスの維持は、メモリ不足で処理が止められる環境のため入れない

