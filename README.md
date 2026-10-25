# いくメモ（Ikumemo）

話しかけるだけで買い物メモ。買ったらスワイプで記録。あとからカレンダーで振り返れる Android アプリです。

> 買い物に「**いく**」ときの、いちばん身近な「**メモ**」。
> いく前に話して残す。買ったらスワイプで記録。その記録が、次にいくときの「いつもの」になる。

- アカウント登録なし・サーバーなし・広告なし・解析ツールなし。データはすべて端末の中に保存します
- 家計簿（金額の記録）と、家族などとの共有は行いません。「何を・いつ・どこで買ったか」と
  「使い切り忘れ防止」に集中します

## 開発の状況

開発中です（マイルストーン M0「準備」の段階）。Google Play では未公開です。
要件と判断の背景は [PLAN.md](PLAN.md)、今後のタスクは
[docs/records/managed/BACKLOG.md](docs/records/managed/BACKLOG.md) を参照してください。

## 前提環境

- JDK 17
- Android SDK（compileSdk / targetSdk 36、build-tools 36.0.0）
- Gradle 8.13（リポジトリ同梱の Gradle Wrapper を使うため、別途インストールは不要）
- Markdown の静的解析と記録ファイルの検証には Node.js（`npx`）と Python 3 + PyYAML を使います

Android SDK の場所は環境変数 `ANDROID_HOME`、または `local.properties`（`.gitignore` 対象）の
`sdk.dir` で指定します。

## ビルド・実行・テスト

Gradle Wrapper 経由で、すべてリポジトリのルートから実行します（Windows では `gradlew.bat`）。

```bash
# コードスタイル（ktlint）。違反は ./gradlew ktlintFormat で自動修正できる
./gradlew ktlintCheck

# 静的解析（detekt）と Android Lint
./gradlew detekt lintDebug

# 単体テスト（core / mobile 全モジュール）
./gradlew testDebugUnitTest :core:test

# 時間のかかるテスト（2 万件の検索性能など）も含めて流す
./gradlew testDebugUnitTest -PslowTests

# デバッグ版 APK のビルド
./gradlew assembleDebug

# 接続した端末へデバッグ版を入れる
./gradlew :mobile:installDebug
```

デバッグ版は applicationId が `com.ikumemo.mobile.debug`、アプリ名が「いくメモ（debug）」になり、
Google Play 版と同じ端末に並べて入れられます。

品質ゲート（コミット・プルリクエスト前に成功を確認するコマンドと合否基準）の正本は
[CLAUDE.md](CLAUDE.md)「本リポジトリの品質ゲート定義」です。本リポジトリに CI はありません。

## リリースビルド

Google Play へ提出する署名付きビルドを作るには、`local.properties`（`.gitignore` 対象、コミットしないこと）へ
次のキーを追加します。

```properties
RELEASE_STORE_FILE=/absolute/path/to/upload-keystore.jks
RELEASE_STORE_PASSWORD=<keystore のパスワード>
RELEASE_KEY_ALIAS=<key alias の名前>
RELEASE_KEY_PASSWORD=<key のパスワード>
```

アップロード鍵は手元の PC だけに置き、リポジトリや GitHub へは渡しません（Play App Signing を使います）。
キーが無い場合も `assembleDebug` 等の通常のビルドには影響しません（リリースビルドが署名なしになるだけです）。

`versionCode` / `versionName` の管理を含めたリリースビルドは、次のバッチファイルで行います。
現在のバージョンは `scripts/version.properties` に記録され、ビルドが成功したときだけ更新されます。

```bat
rem versionCode を 1 増やしてビルドする（versionName はそのまま）
scripts\release-build.bat

rem versionCode / versionName を指定してビルドする（増やさない）
scripts\release-build.bat -VersionCode 10 -VersionName 1.0.0
```

成果物は `mobile/build/outputs/bundle/release/mobile-release.aab` です。Play Console へ手動で
アップロードします（内部テスト → クローズドテスト → 製品版）。Play への自動アップロードは行いません。

## プロジェクト構成

| パス | 内容 |
| --- | --- |
| `core/` | Android に依存しない純 Kotlin/JVM のロジック（`com.ikumemo.core`）。JVM の単体テストだけで検証する |
| `mobile/` | Android アプリ本体（`com.ikumemo.mobile`）。画面・データ層・ウィジェット |
| `mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/` | テーマ 4 種（ポップ／やさしい／クール／シンプル）、配色、文字の大きさ |
| `config/detekt/` | detekt の設定 |
| `scripts/` | バージョン管理・リリースビルド・記録ファイルの検証 |
| `docs/records/` | 設計書・実施記録・未対応事項（AI エージェントが更新する記録ファイル） |
| `rules/` | AI エージェントのガードレール |

## 関連ドキュメント

- [PLAN.md](PLAN.md): 開発開始前の計画書（要件と判断の背景。開発開始後は更新しない）
- [docs/design/WIREFRAME.md](docs/design/WIREFRAME.md): 主要な画面とシートのワイヤーフレーム
- [docs/records/managed/DESIGN.md](docs/records/managed/DESIGN.md): 実装済み内容の統合設計書
- [docs/records/managed/BACKLOG.md](docs/records/managed/BACKLOG.md): 未対応事項・課題・次ステップ
- [CONTRIBUTING.local.md](CONTRIBUTING.local.md): 報告の受け付け方針（Issue は受付、外部の Pull Request は非受付）と
  本リポジトリ固有の開発プロセス（共通規約は [CONTRIBUTING.md](CONTRIBUTING.md)）
- [SECURITY.md](SECURITY.md): 脆弱性の報告方法
- [docs/USER_GUIDE.md](docs/USER_GUIDE.md): 使い方（アプリ内の使い方 `mobile/src/main/assets/help/` から
  `scripts/make-user-guide.py` で作る）
- [docs/SUPPORT.md](docs/SUPPORT.md): よくある質問・ご意見の送り方（利用者向け）
- [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md): バージョンごとの変更点（利用者向け）
- [docs/CLOSED_TEST.md](docs/CLOSED_TEST.md): クローズドテスト参加の案内
- [docs/FEEDBACK.md](docs/FEEDBACK.md): ご意見の受け付けと運用の手順（オーナー向け）
- [docs/store/PRIVACY_POLICY.md](docs/store/PRIVACY_POLICY.md) / [docs/store/STORE_LISTING.md](docs/store/STORE_LISTING.md):
  プライバシーポリシー（公開版）とストア掲載文の正本
- [CHANGELOG.md](CHANGELOG.md): リポジトリの運用ルール・ドキュメントの変更履歴
- [CLAUDE.md](CLAUDE.md) / [.github/copilot-instructions.md](.github/copilot-instructions.md): AI エージェントの実行ルール

## ライセンス

[Apache License 2.0](LICENSE)。著作権表示は [NOTICE](NOTICE) を参照してください。
