<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- date: 2026-10-25 13:30
  summary: メモ画面の基本（文字で追加・編集シート・重複判定）と下タブ 3 つの画面骨格を追加（M1）
  details:
    変更内容: >-
      ShoppingRepository（品目の作成・メモへの追加・編集の保存）と ShoppingListViewModel を追加した。正規化した品名が同じ物が
      メモにあれば追加せず、既存の項目を一瞬光らせて「○○はもうメモにあります」を出す（R-01・R-02）。編集で別の項目と同じ品名に
      変えた場合も品名だけは変えず同じ知らせを出す。続けて追加したときに並びが入れ替わらないよう、書き込みは Mutex で直列化した。
      Navigation Compose（型安全ルート）で下タブ（メモ・カレンダー・おしらせ）を作り、カレンダーとおしらせは見出しと案内文だけの
      骨格とした。手動 DI の ViewModel 生成は AppViewModelFactory に集約した。依存に Navigation Compose 2.8.6、
      lifecycle-viewmodel-compose、kotlinx-serialization-json 1.7.3、material-icons-extended、Turbine 1.2.0、Compose UI Test を追加した
    変更ファイル:
      - gradle/libs.versions.toml
      - build.gradle.kts
      - mobile/build.gradle.kts
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ItemSheets.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（ViewModel テスト 5 件。追加・重複時の既存項目の提示・空文字の無視・編集の保存・
      編集での重複を確認）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-012
- date: 2026-10-25 12:40
  summary: core/text に品名の正規化（表記ゆれの吸収）と自作の読み辞書を追加（M1）
  details:
    変更内容: >-
      ProductNameNormalizer を追加し、NFKC・英字の小文字化・空白除去のあと、ReadingDictionary で漢字表記を読みへ
      長い語から優先して置き換え、カタカナをひらがなへ変換する（R-02）。辞書は日常の買い物の品名 166 語を自作した。
      「酒」と「鮭」のように別の物が同じ読みになる語は辞書に入れない（料理酒は残す）
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/text/ProductNameNormalizer.kt
      - core/src/main/kotlin/com/ikumemo/core/text/ReadingDictionary.kt
      - core/src/main/kotlin/com/ikumemo/core/text/DefaultReadings.kt
      - core/src/test/kotlin/com/ikumemo/core/text/ProductNameNormalizerTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（「たまご」「タマゴ」「卵」「玉子」「半角ﾀﾏｺﾞ」が同じ正規化名になるテストを含む 9 件）、
      markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-011
- date: 2026-10-25 12:10
  summary: Room のデータモデル（6 テーブル）・DAO・スキーマ JSON・手動 DI の入れ物を追加（M1）
  details:
    変更内容: >-
      PLAN §4.3 の Store・Product・ShoppingItem・ShoppingTrip・PurchaseRecord・Reminder を Room のエンティティとして定義し、
      日付は epochDay、日時は epoch ミリ秒、おしらせの状態は列挙名で保存する型変換を追加した。記録を消すと紐づくおしらせが
      外部キーの CASCADE で消える（R-12）、お店は archived で使わない印を付けて行を残す（R-16）。
      KSP（2.0.21-1.0.27）と Room 2.6.1 を導入し、スキーマ JSON を mobile/schemas へ出力して Git 管理する。
      IkumemoApplication が AppContainer（DB と Clock）を 1 つ持つ手動 DI とした。DAO テスト用に Robolectric 4.14.1 を導入し、
      Robolectric が targetSdk 36 に未対応のため test resources の robolectric.properties で sdk=35 を指定した
    変更ファイル:
      - gradle/libs.versions.toml
      - build.gradle.kts
      - mobile/build.gradle.kts
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Entities.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Converters.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/IkumemoDatabase.kt
      - mobile/schemas/com.ikumemo.mobile.data.db.IkumemoDatabase/1.json
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/db/IkumemoDatabaseTest.kt
      - mobile/src/test/resources/robolectric.properties
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（DAO テスト 5 件を含む）、markdownlint 0 issues（27 files、対象件数と一致）、記録ファイル OK
    関連ID:
      - BL-010
- date: 2026-10-25 10:40
  summary: バージョン管理とリリースビルドの仕組みを sesami-wear 準拠で追加（M0）
  details:
    変更内容: >-
      scripts/version.properties（VERSION_CODE=1、VERSION_NAME=0.1.0）を正本とし、mobile の versionCode と versionName を
      -PappVersionCode と -PappVersionName で上書きできるようにした。scripts/release-build.ps1 と .bat を追加し、
      versionCode を 1 増やして bundleRelease を実行、成功時だけ version.properties を LF・BOM なしで更新する。
      リリース署名は sesami-wear と同じく local.properties（.gitignore 対象）の RELEASE_STORE_FILE 等から読み、未設定なら署名なし。
      PLAN §6 は secrets.properties と記載しているが、オーナー指示（sesami-wear をベースにする）に合わせ local.properties とした
      （どちらも .gitignore 対象で、鍵がリポジトリへ入らない点は同じ）
    変更ファイル:
      - scripts/version.properties
      - scripts/release-build.ps1
      - scripts/release-build.bat
      - mobile/build.gradle.kts
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      aapt2 dump badging（既定値と -PappVersionCode=7 -PappVersionName=9.9.9 の両方）、./gradlew :mobile:bundleRelease、
      PowerShell の Parser.ParseFile による release-build.ps1 の構文確認、markdownlint、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL、APK は既定で versionCode 1 と 0.1.0-debug、注入時は 7 と 9.9.9-debug、
      アプリ名「いくメモ（debug）」、署名なしの mobile-release.aab（約 1.7MB）を生成、構文エラー 0 件
    関連ID:
      - BL-004
- date: 2026-10-25 10:10
  summary: デザインシステムの土台（4 テーマ × ライト/ダーク、文字サイズ、コントラスト自動テスト）を追加（M0）
  details:
    変更内容: >-
      mobile/designsystem に IkumemoTheme・ThemePreset（ポップ／やさしい／クール／シンプルの 4 種、各ライト/ダークの配色と
      角の丸み）・IkumemoPalette（ARGB の Long で持ち Compose 非依存で検査可能）・ContrastRatio（WCAG の計算）・
      TextSizeSetting（ふつう 1.0／大きい 1.15／とても大きい 1.3、端末の文字サイズとの積を 200% で頭打ち）を追加。
      初期テーマは PLAN に指定が無いため D-8 の並びの先頭（ポップ）とした。MainActivity を IkumemoTheme で表示するよう変更
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/IkumemoTheme.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemePreset.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/IkumemoPalette.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ContrastRatio.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/TextSizeSetting.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ThemeContrastTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ContrastRatioTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/TextSizeSettingTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL、単体テスト 12 件成功（4 テーマ × 明暗の文字と背景の 15 組すべてが 4.5:1 以上）、
      Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-003
- date: 2026-10-25 09:30
  summary: Gradle プロジェクトの雛形を sesami-wear 準拠で作成（M0）
  details:
    変更内容: >-
      Gradle Wrapper 8.13・Version Catalog（sesami-wear と同じ版）・core（純 Kotlin/JVM）と mobile（com.ikumemo.mobile）の
      2 モジュールを作成。ktlint と detekt を全モジュールへ適用し、detekt 設定は sesami-wear から流用して @Preview 関数を
      UnusedPrivateMember の対象外にした。debug は applicationIdSuffix .debug とアプリ名「いくメモ（debug）」。
      edge-to-edge の空画面、吹き出し＋かごのアダプティブアイコン（monochrome 付き）、.gitignore・.editorconfig を追加。
      アダプティブアイコンは aapt2 の制約で mipmap-anydpi-v26 に置き、Lint の ObsoleteSdkInt を lint.xml で除外した
    変更ファイル:
      - settings.gradle.kts
      - build.gradle.kts
      - gradle.properties
      - gradle/libs.versions.toml
      - gradle/wrapper/gradle-wrapper.jar
      - gradle/wrapper/gradle-wrapper.properties
      - gradlew
      - gradlew.bat
      - .gitignore
      - .editorconfig
      - config/detekt/detekt.yml
      - core/build.gradle.kts
      - mobile/build.gradle.kts
      - mobile/proguard-rules.pro
      - mobile/lint.xml
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/values/themes.xml
      - mobile/src/main/res/values/ic_launcher_background.xml
      - mobile/src/main/res/values-night/themes.xml
      - mobile/src/main/res/drawable/ic_launcher_foreground.xml
      - mobile/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
      - mobile/src/debug/res/values/strings.xml
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - Gradle は BUILD SUCCESSFUL（Lint は No issues found）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-001
```
<!-- COPILOT_RECORDS:END -->
