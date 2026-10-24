<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用設計記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.design.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
# 実装用プロンプト設計書

## 目的

- 本書は、Android 向け買い物メモアプリ「いくメモ」を、現時点の実装と同じ内容で AI エージェントに再実装させるための
  実装用プロンプト設計書である。履歴は積み上げず、常に最新の実装を表す
- 要件の全体像と判断の背景は `PLAN.md`（開発開始前の計画書。開発開始後は更新しない）、未実装の作業は
  `docs/records/managed/BACKLOG.md` を参照する。本書には**実装済み**の内容だけを書く

## 対象システム概要

- 対象: 話して追加・スワイプで記録・カレンダーで振り返り・期限のおしらせを持つ買い物メモアプリ（Google Play で配布、
  ソースは GitHub で Apache-2.0 公開）
- 進捗: マイルストーン M0（準備）・M1 の完了条件（手入力で追加 → スワイプで記録 → 元に戻す）・M2 の完了条件
  （「牛乳2本とたまご」で 2 件追加）・M3（候補とお店）を実装済み。各入力欄のマイク（F-04）は未実装。
  記録の修正・削除（F-16）は処理のみ実装済みで、画面はカレンダー（M4）で作る
- 前提環境: JDK 17、Android SDK（compileSdk / targetSdk 36、minSdk 26）、Gradle 8.13（Wrapper 同梱）

## 実装済み機能要件

| 要件 ID | 内容 | 実装 |
| --- | --- | --- |
| M0 雛形 | core / mobile の 2 モジュールでビルドできる | `settings.gradle.kts`、`core/`、`mobile/` |
| PLAN §4.5 | applicationId `com.ikumemo.mobile`、debug は `.debug` と `-debug`、debug のアプリ名「いくメモ（debug）」 | `mobile/build.gradle.kts`、`mobile/src/debug/res/values/strings.xml` |
| PLAN §4.2 | targetSdk 35 以降の画面端までの表示（edge-to-edge） | `MainActivity` の `enableEdgeToEdge()` と `Scaffold` の余白 |
| PLAN D-8・§2.7 | プリセットテーマ 4 種（ポップ／やさしい／クール／シンプル）× ライト／ダーク、テーマごとの角の丸み | `designsystem/ThemePreset.kt`、`IkumemoTheme.kt` |
| PLAN §1.3-7 | 文字と背景のコントラスト比 4.5:1 以上を自動テストで保証 | `designsystem/ContrastRatio.kt`、`ThemeContrastTest` |
| PLAN §1.3-6・F-65 | 文字の大きさ（ふつう 1.0／大きい 1.15／とても大きい 1.3）を端末の文字サイズに掛け、200% で頭打ち | `designsystem/TextSizeSetting.kt`、`IkumemoTheme` の `LocalDensity` 差し替え |
| PLAN §1.2・§2.7 | アプリアイコン（吹き出し＋お買い物かご）、アダプティブアイコンとテーマアイコン（monochrome） | `mipmap-anydpi-v26/ic_launcher.xml`、`drawable/ic_launcher_foreground.xml` |
| PLAN §4.3 | Room の 6 テーブル（お店・品目・メモ・お買い物・記録・おしらせ）と DAO、スキーマ JSON の Git 管理 | `data/db/`、`mobile/schemas/` |
| R-02・PLAN §4.4 | 品名の正規化（NFKC・小文字化・空白除去 → 漢字を読みへ → カタカナをひらがなへ）と自作の読み辞書 | `core/text/ProductNameNormalizer.kt`、`ReadingDictionary.kt`、`DefaultReadings.kt` |
| F-03 | 文字で追加（追加シート。追加後もシートを開いたままにして続けて入れられる） | `shoppinglist/ItemSheets.kt` の `AddItemSheet` |
| F-04（一部） | 項目の編集シート（品名・数量・メモ・買うお店・写真。閉じると保存）。各欄のマイクは未実装 | `shoppinglist/EditItemSheet.kt` |
| F-07・R-15 | 写真は「撮る」（標準カメラ＋FileProvider）と「選ぶ」（Photo Picker）。`filesDir/photos/` へ長辺 1600px・Exif 無しの JPEG で保存し、「買った」では同じファイルを記録から参照。メモの行に写真ありの印 | `data/PhotoStore.kt`、`EditItemSheet.kt` |
| R-01・R-02 | 同じ物がメモにあれば追加せず、既存の項目を一瞬光らせて「○○はもうメモにあります」 | `data/ShoppingRepository.kt`、`ShoppingListViewModel` |
| F-10・F-11・D-4・R-05 | 右へスワイプで「買った」、左へで「やめた」。幅の 40% で確定し、超えた瞬間に軽い振動。背景は色＋アイコン＋文字 | `shoppinglist/SwipeableItemRow.kt` |
| F-13 | 行の左の ○ でも「買った」（○ を隠す設定は未実装） | `SwipeableItemRow.kt` の `ItemRowContent` |
| F-12・R-04 | 「買った：牛乳 ／ 元に戻す」を 5 秒表示（TalkBack 等の利用時は推奨時間まで延長）。元に戻せるのは最新の 1 件 | `ShoppingListScreen.kt` の `UndoEffect` |
| R-03・R-10・R-11・R-15 | 「やめた」は記録せず品目を残す。「買った」は端末の今日の日付で記録、同じ日の 2 回目も別記録、写真は同じファイルを参照 | `data/ShoppingRepository.kt` |
| R-12 | 購入回数・最後に買った日・平均間隔を残っている記録から計算し直す。記録を消すと紐づくおしらせも消す | `core/suggest/PurchaseStats.kt`、`data/ProductStatsUpdater.kt` |
| F-16（処理のみ） | 記録の修正（購入日・お店・数量・メモ・写真。日付かお店が変わればお買い物を付け替え）と削除。画面は未実装 | `data/RecordRepository.kt` |
| PLAN §3 | 下タブ 3 つ（メモ・カレンダー・おしらせ。アイコン＋文字ラベル）。カレンダーとおしらせは見出しと案内文だけ | `IkumemoApp.kt`、`calendar/`、`reminder/` |
| F-01・F-02・PLAN §4.4 | 音声テキストの分割・数量抽出・不要語除去・誤変換補正・N-best の選択 | `core/voice/VoiceTextParser.kt`、`QuantityParser.kt` |
| F-01・PLAN §3.2 | 🎤 話して追加（ホーム下部、64dp）→ 聞いている間は途中の文字を表示 → 確認カード（タップで直す、× で外す）→「追加する」 | `voice/VoiceInputSheet.kt`、`VoiceInputViewModel.kt` |
| R-30・R-31 | 日本語固定・オンデバイス優先。何も話さなければ 5 秒で止め「うまく聞き取れませんでした」＋もう一度話す／文字で入れる | `voice/SpeechEngine.kt`、`VoiceInputViewModel.kt` |
| R-01（音声） | 確認カードでメモにある物に「メモにあります」の印を付け、既定で外す | `VoiceInputViewModel.showCards` |
| PLAN §2.10・§5 | マイクの許可は初めて使う時に理由を添えて求める。拒否されたら端末の設定を開くボタンと文字入力の案内。機能の無い端末ではマイクを出さない | `voice/VoiceInputSheet.kt`、`ShoppingListScreen.kt` の `AddButtons` |
| F-20・F-21 | 追加シートに入力中の候補（正規化名の前方一致を先、部分一致を後）と「いつもの」チップ（購入回数 → 最近買った順 → 正規化名、最大 12 件）。メモにある物は出さない。チップは押すたびに 1 件入る | `core/suggest/Suggestions.kt`、`ShoppingListViewModel.addSuggestions`、`ItemSheets.kt` の `AddItemSheet` |
| F-14・F-30・F-31 | メモ上部のお店チップ → いつものお店から選ぶシート（最近使った順の上位 5 件・ほかのお店・新しいお店・選ばない）。選んだお店は「買った」に付く | `shoppinglist/StorePicker.kt`、`data/StoreRepository.kt` |
| R-06 | 選んだ時か最後の「買った」から 3 時間、または日付が変わったら選んだお店を解除（表示は 1 分ごとに見直す） | `data/AppSettings.kt` の `StoreSelection`、`StoreRepository` |
| R-16（処理のみ） | お店は消さずに使わない印、同じお店の統合（記録・メモ・お買い物を移す）。お店の管理の画面は未実装 | `StoreRepository.archive` / `merge` |
| PLAN §4.1 | 手動 DI（`IkumemoApplication` が `AppContainer` を 1 つ持つ） | `AppContainer.kt`、`IkumemoApplication.kt` |
| PLAN §4.5・§6 | バージョンの正本 `scripts/version.properties`、`-PappVersionCode` / `-PappVersionName` での注入、リリースビルドスクリプト | `scripts/version.properties`、`scripts/release-build.ps1` / `.bat`、`mobile/build.gradle.kts` |
| PLAN §6 | リリース署名は手元の PC だけに置き、`local.properties` から読む。未設定なら署名なし | `mobile/build.gradle.kts` の `signingConfigs` |

## 設計方針

### アーキテクチャ方針

- モジュール: `core`（純 Kotlin/JVM、Android 非依存、パッケージ `com.ikumemo.core`）と `mobile`（アプリ本体、
  `com.ikumemo.mobile`）。feature ごとの Gradle モジュール分割はしない（PLAN §4.5）
- `mobile` 内は画面ごとのパッケージに分ける。実装済みは `designsystem/`・`data/`（品目は `ProductRepository`）・`shoppinglist/`・`calendar/`・
  `reminder/`（後ろの 2 つは骨格のみ）。予定は PLAN §4.5 の
  `data/` `shoppinglist/` `calendar/` `reminder/` `settings/` `guide/` `help/` `voice/` `widget/`
- 画面は MVVM + 単方向データフロー。画面からの操作は `sealed interface` のイベント（例 `ShoppingListEvent`）を
  ViewModel の `onEvent()` 1 つで受ける。ViewModel は `StateFlow<UiState>` を公開し、DB の Flow と画面だけの状態
  （開いているシート・光らせる項目・一度だけ出す知らせ）を `combine` する。知らせは画面が表示したら `messageShown()` で消す
- ViewModel は `AppViewModelFactory`（`viewModelFactory` と `APPLICATION_KEY` から `AppContainer` を取り出す）で作る
- ViewModel の書き込み操作は `Mutex` で直列化し、操作した順に反映する。`onEvent` は画面だけの状態を変える操作と
  DB・設定を書き換える操作の 2 つの関数に分けて扱う（detekt の分岐数・関数数の上限に収めるため）
- 画面遷移は Navigation Compose の型安全ルート（`@Serializable` の `MemoRoute` / `CalendarRoute` / `ReminderRoute`）。
  下タブは状態を保存して切り替える（`saveState` / `restoreState`）
- UI は Jetpack Compose + Material 3。DI は手動（`IkumemoApplication` が持つ `AppContainer` から渡す。時刻は
  `AppContainer.clock` の `java.time.Clock` から取り、テストで差し替えられるようにする）
- 依存の版は sesami-wear と同じ版にそろえる（AGP 8.13.0、Kotlin 2.0.21、Compose BOM 2024.12.01 等。
  `gradle/libs.versions.toml`）。必要になったマイルストーンで追加し、使わない依存は入れない

### 設定の保存方針（DataStore）

- 端末内の設定は `AppSettings`（DataStore Preferences、ファイル名 `ikumemo_settings`）に置く。品目ごとに持たない値だけを入れる
- 選んでいるお店は `selected_store_id`・`selected_store_at`・`last_bought_at`（epoch ミリ秒）で持ち、解除の判定は読むたびに行う。
  「買った」に付けるときに解除の条件を満たしていれば保存値も消す
- テストは `testSettings()`（テストごとに別の一時ファイル）で作る

### 表記ゆれ方針（core/text）

- 同じ物の判定は `ProductNameNormalizer.normalize()` の結果の一致だけで行う（数量は比べない、R-02）
- 読み辞書は長い語から優先して置き換える（「糸蒟蒻」は「いと＋こんにゃく」ではなく「いとこんにゃく」）。辞書に無い漢字は
  そのまま残す
- 別の物が同じ読みになる語（「酒」と「鮭」など）は辞書に入れない。同じ物の別表記（「卵」「玉子」）だけを同じ読みにする
- 辞書は自作で、既存の辞書データや他社の商品リストは写さない（PLAN §4.4）

### 音声テキストの分割方針（core/voice）

- 区切りは「と」「、」「。」「・」「それから」「あと」「それと」と空白。NFKC のあとで照合する
- 品名の中の「と」は守る。守る語は、既定の読み辞書の読みのうち「と」を含むもの、`VoiceTextParser(knownNames)` に渡した
  過去の品名、読みの数量（「ひとつ」など）。各位置で最長一致を先に見る
- 不要な言葉（「を買わなきゃ」「が欲しい」「買っといて」「が切れた」等）は区切りとして扱い、品名から外す。
  語末の「を」「も」も外す
- 数量は品名の後ろだけを見る。「数字（算用・漢数字〜999）＋助数詞」「読みの数量」「誤変換（日本 → 2本）」の順に照合し、
  `算用数字＋助数詞`（こ→個、ほん→本、グラム→g、リットル→L 等）へそろえる。ひらがなの助数詞は数字が付くときだけ数量にする
- N-best は、過去の品名と正規化名が一致する品物の数が最も多い候補を採る。同点なら認識エンジンの順位を優先する
- テストケースは実機で得た認識結果を足していく（BL-023）

### デザインシステム方針

- 配色は `IkumemoPalette` に ARGB の `Long` で持ち、`toColorScheme()` で Material 3 の `ColorScheme` へ対応付ける。
  Compose の `Color` に依存させないことで、コントラスト比を Android 実行環境なしの単体テストで検査できる
- 検査する組み合わせは `IkumemoPalette.textPairs()`（on 系と対応する背景、補助文字 onSurfaceVariant を surface と
  background の両方、primary と error を surface 上の文字として）。配色を追加・変更したら必ずこの検査を通す
- 角の丸み: ポップ・やさしい 12/20/28dp、クール 4/8/12dp、シンプル 8/12/16dp（small/medium/large）
- 初期テーマはポップ（PLAN に既定値の指定が無いため D-8 の並びの先頭。きせかえ画面 BL-060 で変更可能）
- テーマと文字の大きさは `storageKey`（`pop` / `gentle` / `cool` / `simple`、`normal` / `large` / `extra_large`）で
  保存する前提。未知の値は既定値へ戻す
- フォントは端末標準を使い、同梱しない（PLAN §2.7）

### データ設計方針

- 「買った」「やめた」「元に戻す」は `ShoppingRepository` の 1 トランザクションで行う。元に戻すときはメモの項目を同じ ID・
  並び順で入れ直し（同じ品目がすでにメモにあれば入れない）、「買った」の記録と使われなくなったお買い物を消して集計し直す

- Room（`IkumemoDatabase`、版 1）。テーブルは `store`・`product`・`shopping_item`・`shopping_trip`・`purchase_record`・
  `reminder` の 6 つ。列は PLAN §4.3 のとおりで、ID は自動採番の `Long`
- 型の保存形式: `LocalDate` は epochDay、`Instant` は epoch ミリ秒、おしらせの状態（`PENDING` / `USED_UP` / `DISCARDED`）は列挙名
- 品目は `normalizedName`（core/text の正規化名）を一意にし、同じ物の判定に使う（R-02）。購入回数・最後に買った日・
  平均間隔は記録から計算し直す値として持つ（R-12）
- 写真の列（`photoUri`）は `photos/<UUID>.jpg` の相対パス。元の URI は保持しない。取り込みは向きを画素へ反映 → 縮小 →
  Exif を書かない JPEG（品質 85）を一時ファイルへ書いてから置き換える
- 外部キー: 記録を消すとおしらせを CASCADE で消す（R-12）。お店は行を消さず `archived` を立てる（R-16）ため、記録・メモ・
  お買い物からの参照は SET_NULL
- お買い物（`shopping_trip`）は同じ日・同じお店（お店なし同士も同一、`storeId IS :storeId` で照合）で 1 件
- いつものお店の候補は `archived = 0` を最近使った順（未使用は最後、次に作成の新しい順）
- スキーマ JSON は KSP 引数 `room.schemaLocation` で `mobile/schemas/` に出力して Git 管理する。版を上げるときは
  AutoMigration とマイグレーションテストを足す
- 音声認識は `SpeechEngine` で包み、テストでは `FakeSpeechEngine` に差し替える。ViewModel ごとに 1 つ作り
  `onCleared` で `destroy` する。無音のタイマー（5 秒）は認識を始める前に仕掛ける（結果がすぐ返っても取り消せるように）
- テストの置き場所: DB・ViewModel のテストは `src/test`、Compose の画面テストは `src/testDebug`（テスト用 Activity を
  宣言する ui-test-manifest を debug にだけ入れるため。release の単体テストでは動かない）。ViewModel テストは
  `MainDispatcherRule`（StandardTestDispatcher）と Turbine で目的の状態まで待ち、終了時に `viewModelScope` を止める。
  画面テストは `testDebug/.../testing/ComposeWait.kt` の `awaitCondition`（`waitForIdle` でメインスレッドを進めながら待つ。
  Robolectric の `waitUntil` は待つ間にメインスレッドの処理を進めない）で待ち、ボトムシートの中のボタンは座標のタップが
  届かないため `performSemanticsAction(SemanticsActions.OnClick)` で押す
- テスト用 DB（`inMemoryDatabase()`）は実行スレッドを DB ごとに持つ（共有プールに前のテストの処理が残って止まるのを避ける）。
  写真の読み書きのテストは `@GraphicsMode(NATIVE)` で動かす
- DAO テストは Robolectric（4.14.1 が targetSdk 36 未対応のため `robolectric.properties` で `sdk=35`）とインメモリ DB で行う

### UI/UX 方針

- 画面の配置は `docs/design/WIREFRAME.md`（主要画面・シートの配置と、その場のガイドで照らす場所の目印）
- 文言はすべて `strings.xml` に置く（日本語のみでも。PLAN §4.1）
- 各画面の `TopAppBar` は `windowInsets = WindowInsets(0)` とし、システムバーの余白は外側の `Scaffold`（下タブを持つ）の
  `innerPadding` で取る
- UI テストとその場のガイドの目印は `testTag` で付け、名前は各画面の `TestTags` に集める
- 押せる部分は最小 48dp、主要ボタンは 56dp 以上（PLAN §1.3-2）

## 非機能要件

- 性能・信頼性: 未実装（PLAN §8 の目標は BL-089 等で対応）
- セキュリティ／プライバシー: `AndroidManifest.xml` で宣言する権限は `RECORD_AUDIO` だけ（音声認識サービスを見つけるための
  `<queries>` の `android.speech.RecognitionService` も宣言）。INTERNET・位置情報・CAMERA・VIBRATE・SCHEDULE_EXACT_ALARM は
  使わない方針（PLAN §5）。録音は端末の音声認識サービスの中で行われ、本アプリは音声を保存しない。公開している画面は
  起動用の `MainActivity` だけ。FileProvider（`${applicationId}.fileprovider`、非公開・URI 権限の付与のみ）はカメラの一時ファイル
  `cacheDir/camera/` だけを渡す
- 署名鍵・`local.properties`・`secrets.properties`・`*.jks` / `*.keystore`・`.env*` は `.gitignore` で除外する

## 実装制約

### 技術制約

- アダプティブアイコンは aapt2 が `-v26` 付きのフォルダでしか解決しないため、minSdk 26 でも
  `mipmap-anydpi-v26` に置き、Lint の `ObsoleteSdkInt` を `mobile/lint.xml` で除外する
- detekt は sesami-wear の設定（`maxIssues: 0`、`MagicNumber` 無効、`LongMethod` 60）に加え、`@Preview` 関数を
  `UnusedPrivateMember` の対象外にする
- 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」が正本（ktlintCheck・detekt・lintDebug・
  testDebugUnitTest test・assembleDebug、markdownlint、記録ファイルの検証）

### 運用制約

- CI（GitHub Actions）と Dependabot は導入しない。Gradle の依存脆弱性スキャンも導入しない（sesami-wear に合わせた
  オーナー判断、2026-10-24）。ローカルの品質ゲートが唯一のゲート
- 外部からの Pull Request は受け付けない（オーナー判断、`CONTRIBUTING.local.md`）
- ライセンスは Apache-2.0。リポジトリ直下の `LICENSE` は apache.org 配布の本文と同一（付録の著作権行は雛形のまま）で、
  著作権表示は `NOTICE` に書く。各ソースファイル先頭へのライセンスヘッダーは付けない（PLAN §7）
- `PLAN.md` は本文を更新せず、冒頭の `markdownlint-disable-file` で静的解析の対象から外している
- リリースビルドは `scripts/release-build.bat`。Play への自動アップロードは行わない

## エージェント実装指示

- 初回実装時の出力要件: 本書の「実装済み機能要件」の各行を満たす構成を作り、品質ゲートをすべて成功させる。
  テーマの配色は `ThemeContrastTest` を通る値にする
- 追加実装時の出力要件: BACKLOG.md のタスク単位で実装し、完了したら本書の該当節（実装済み機能要件・設計方針・
  非機能要件・実装制約）を最新の実装に合わせて書き換える。未実装の内容を本書へ先回りして書かない
- 要件トレーサビリティ要件: 実装済み機能要件の表は PLAN の ID（F- / R- / W- / G- / H- / D-、または節番号）と
  実装箇所を対応付ける。PLAN と異なる判断をした場合は、その理由を本書と BACKLOG の `根拠` に残す
<!-- COPILOT_RECORDS:END -->
