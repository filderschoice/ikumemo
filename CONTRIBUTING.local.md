# コントリビューションガイド（リポジトリ固有）

本リポジトリは、Android 向け買い物メモアプリ「いくメモ」の開発リポジトリです。本ファイルは
`CONTRIBUTING.md`（配布元から取り込む共通規約）に対するリポジトリ固有の内容で、外部の方からの報告の
受け付け方針（前半）と、開発者向けの固有規約（後半）の 2 部構成です。共通規約との分担は `CONTRIBUTING.md` 冒頭を
参照してください。

- 脆弱性の報告は公開 Issue ではなく [SECURITY.md](SECURITY.md) の手順に従ってください。

## 受け付けている報告と、受け付けていないもの

本アプリの実装は、Claude Code による AI 実装を主体として開発者本人が行う体制で進めています（sesami-wear と同じ体制）。
コードの変更経路を開発者側へ一本化しているため、**外部からの Pull Request は受け付けていません**。
一方で、Issue でのご報告・ご要望は歓迎します。いただいた内容は開発者が確認し、対応する場合は開発者側で実装します。

| 種類 | 受け付け | 窓口 |
| --- | --- | --- |
| 不具合の報告 | 受け付けます | [Issue](https://github.com/filderschoice/ikumemo/issues/new/choose) |
| 機能の要望・改善提案 | 受け付けます | [Issue](https://github.com/filderschoice/ikumemo/issues/new/choose) |
| 使い方の質問 | 受け付けます | [Issue](https://github.com/filderschoice/ikumemo/issues/new/choose) |
| 誤字・ドキュメントの誤りの指摘 | 受け付けます | [Issue](https://github.com/filderschoice/ikumemo/issues/new/choose) |
| 脆弱性の報告 | 受け付けます（非公開経路） | [SECURITY.md](SECURITY.md) |
| Pull Request | 受け付けていません | — |

GitHub を使わない利用者の方からのご意見は、アプリ内の「ご意見・ご要望」（ご意見フォーム）で受け付け、
開発者が個人情報を除いて Issue へ転記します（[PLAN.md](PLAN.md) §7.3）。

### Pull Request を受け付けていない理由

- 実装・テスト・記録ファイル（`docs/records/managed/`）の更新を、AI エージェントを含む一貫した手順で
  行っており、外部からの変更を取り込むとこの手順と記録の整合を保てないためです。
- 個人が余暇で開発しており、外部の Pull Request をレビューする体制を維持できないためです。

Pull Request をいただいても、内容を確認せずにクローズする場合があります。あらかじめご了承ください。
ご提案は Issue へお願いします。

### Issue を作成するときのお願い

- 目的に合ったテンプレート（不具合の報告 / 機能の要望・改善提案 / 使い方の質問）を選んでください。
- **お名前・メールアドレス・住所などの個人情報を書かないでください。** スクリーンショットに写り込んでいないかも
  ご確認ください。
- 既に同じ内容の Issue がないかご確認ください。同じ内容がある場合は、その Issue へのリアクション（👍）で
  お知らせください。
- 個人が余暇に開発しているため、返信までに数日〜数週間かかる場合があります。
- ご要望に対応するかどうか、対応する時期は開発者が判断します。対応しないままクローズする場合もあります。

### 要望・提案を実装するかどうかの判断基準

いただいたご要望は、次の観点で判断します（開発者自身の実装にも同じ基準を適用します）。

- 本アプリの目的（「何を・いつ・どこで買ったか」と「使い切り忘れ防止」）に沿っていること
- 「やらないこと」に当たらないこと。金額の記録・家計簿機能、家族などとの共有・同期、位置情報によるお店の
  自動判定は行いません（[PLAN.md](PLAN.md) §9）
- アカウント・サーバー・インターネット通信を持たない構成を保つこと。広告 SDK・アクセス解析 SDK・
  クラッシュ収集 SDK は組み込みません
- 技術的な用語や複雑な設定が苦手な人でも迷わないこと（1 画面 1 目的、やさしい言葉）
- 個人で保守を継続できる範囲に収まること

---

以降は、開発者（メンテナーおよび AI エージェント）向けの固有規約です。

## 規定ブランチ

- 規定ブランチ: `main`

## レビュー責任者

本リポジトリの現在の体制はメンテナー 1 名（[@filderschoice](https://github.com/filderschoice)、
`.github/CODEOWNERS` で全パスを担当）です。`CONTRIBUTING.md`「レビュー要件」の各担当は、
現時点ではすべてメンテナーが兼務します。体制を拡大した場合は本節と `.github/CODEOWNERS` を更新します。

| 担当 | 担当者・チーム |
| --- | --- |
| セキュリティ担当 | `@filderschoice` |
| プライバシー担当 | `@filderschoice` |
| プロダクトオーナー | `@filderschoice` |

- すべての変更は、規定ブランチへマージする前にメンテナーのレビューを必須とします。
- `rules/` 配下の変更は、セキュリティ観点・プライバシー観点の双方をレビューで確認します。
- 互換性に影響する変更（特にデータベースのスキーマ変更）は、移行方針の提示とメンテナーの承認を必須とします。
- AI エージェント（Claude Code / GitHub Copilot）が作成した変更にも、人手による変更と同じレビュー要件を適用します。

## 自律ループ実行モードの採否

- 採否: 採用（`CLAUDE.md`「自律ループ実行モード（Loop Engineering）」、Skill `autonomous-loop`）

## 例外の追跡

- 追跡場所: `docs/records/managed/BACKLOG.md` へ理由・期限・責任者つきで登録する。

## ドキュメント構成（追加分）

`CONTRIBUTING.md`「ドキュメント構成」の表に無い、本リポジトリ固有のファイル・ディレクトリです。

| パス | 役割 | 詳細 |
| --- | --- | --- |
| `PLAN.md` | 開発開始前の計画書。要件と判断の背景を残す資料 | 開発開始後は本文を更新しない（PLAN D-16）。markdownlint の対象外 |
| `core/` / `mobile/` | アプリのソースコード | 構成は [README.md](README.md)「プロジェクト構成」 |
| `config/detekt/` | detekt の設定 | sesami-wear から流用し、`@Preview` 関数を未使用の検出から除外 |
| `scripts/version.properties` / `scripts/release-build.*` | バージョンの正本とリリースビルド | 使い方は [README.md](README.md)「リリースビルド」 |
| `site/` / `docs/store/` / `docs/design/` / `docs/` 直下の利用者・オーナー向け文書 / `baselineprofile/` | 紹介ページ・ストア掲載・デザイン・利用者向け文書・Baseline Profile のモジュール | 一覧は [README.md](README.md)「プロジェクト構成」「関連ドキュメント」（ここへは個別に足さない） |

## その他の開発プロセス

### 変更手順（記録先の割当）

`CONTRIBUTING.md`「変更手順」の「記録ファイルを更新する」工程では、変更の種類ごとに次へ記録します。

- コード修正を伴う変更: `docs/records/managed/EXECUTE.md`（実施記録）と、設計方針が変わる場合は
  `docs/records/managed/DESIGN.md`
- リポジトリのルール・ドキュメントのみの変更: `CHANGELOG.md`

### 品質ゲート

本リポジトリには CI がなく（`.github/workflows/` を置いていません）、品質ゲートはローカル実行が唯一の
ゲートです。コミット・プルリクエスト作成の前に、`CLAUDE.md`「本リポジトリの品質ゲート定義」の
コマンドを実行し、すべて成功することを確認します。コマンドと合否基準の正本は同節で、ここへは複製しません。
ビルド環境の準備は [README.md](README.md)「前提環境」を参照してください。

- `python scripts/validate-records.py` は PyYAML が必要です（`pip install pyyaml`）。体裁の正本は
  [docs/records/spec/FORMAT.md](docs/records/spec/FORMAT.md)「YAMLとしての体裁」です。
- ktlint の指摘は `./gradlew ktlintFormat` で自動修正できます。
- Markdownlint の設定は `.markdownlint-cli2.yaml` です。Git 管理外の `*.local.md`（作業メモ）は検査対象外です。
