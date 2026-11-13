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

- 対象: 話して追加・スワイプで記録・カレンダーで振り返り・期限のお知らせを持つ買い物メモアプリ（Google Play で配布、
  ソースは GitHub で Apache-2.0 公開）
- 進捗: マイルストーン M0〜M8（準備・メモ・音声入力・候補とお店・カレンダーと写真・お知らせ・着せ替え・ウィジェットと
  ショートカット・初めての案内と使い方・スクリーンショットテスト・公開用ドキュメント）を実装済み。公開前の外部作業
  （Google フォーム・Play Console の申告・実機での確認・クローズドテスト）と site/ は `BACKLOG.md` を参照する。
  書き出し・読み込み（F-72）と「データをすべて消す」（F-76）まで実装済みで、M0〜M8 の機能は出そろった。ほかの実装済みの要件は下の表のとおり
- 前提環境: JDK 17、Android SDK（compileSdk / targetSdk 36、minSdk 26）、Gradle 8.13（Wrapper 同梱）

## 実装済み機能要件

| 要件 ID | 内容 | 実装 |
| --- | --- | --- |
| M0 雛形 | core / mobile の 2 モジュールでビルドできる | `settings.gradle.kts`、`core/`、`mobile/` |
| PLAN §4.5 | applicationId `com.ikumemo.mobile`、debug は `.debug` と `-debug`、debug のアプリ名「いくメモ（debug）」 | `mobile/build.gradle.kts`、`mobile/src/debug/res/values/strings.xml` |
| PLAN §4.2 | targetSdk 35 以降の画面端までの表示（edge-to-edge） | `MainActivity` の `enableEdgeToEdge()` と `Scaffold` の余白 |
| PLAN D-8・§2.7 | プリセットテーマ 4 種（ポップ／やさしい／クール／シンプル）× ライト／ダーク、テーマごとの角の丸み | `designsystem/ThemePreset.kt`、`IkumemoTheme.kt` |
| PLAN §1.3-7 | 文字と背景のコントラスト比 4.5:1 以上を自動テストで保証 | `designsystem/ContrastRatio.kt`、`ThemeContrastTest` |
| PLAN §1.3-6・F-65 | 文字の大きさ（普通 1.0／大きい 1.15／とても大きい 1.3）を端末の文字サイズに掛け、200% で頭打ち | `designsystem/TextSizeSetting.kt`、`IkumemoTheme` の `LocalDensity` 差し替え |
| PLAN §1.2・§2.7 | アプリアイコン（吹き出し＋お買い物かご）、アダプティブアイコンとテーマアイコン（monochrome） | `mipmap-anydpi-v26/ic_launcher.xml`、`drawable/ic_launcher_foreground.xml` |
| PLAN §4.3 | Room の 6 テーブル（お店・品目・メモ・お買い物・記録・お知らせ）と DAO、スキーマ JSON の Git 管理 | `data/db/`、`mobile/schemas/` |
| R-02・PLAN §4.4 | 品名の正規化（NFKC・小文字化・空白除去 → 漢字を読みへ → カタカナをひらがなへ）と自作の読み辞書 | `core/text/ProductNameNormalizer.kt`、`ReadingDictionary.kt`、`DefaultReadings.kt` |
| F-03 | 文字で追加（追加シート。開くと品名の欄にフォーカスしてキーボードを出す。追加後もシートを開いたままにして続けて入れられる。入れた文字は `QuantityParser.split` で品名の後ろの数量を分け、「牛乳 2本」は品名 牛乳・数量 2本 にする。区切りでの複数件の分割はせず、数量だけの入力は分けずにそのまま品名にする。「いつもの」チップと入力中の候補は品名そのまま） | `shoppinglist/ItemSheets.kt` の `AddItemSheet`、`ShoppingListViewModel` の `AddByText` |
| F-04 | 項目の編集シート（品名・数量・メモ・買うお店・写真。閉じると保存）。下に「閉じる」と、区切り線をはさんで別の行に「メモから外す（やめた）」（48dp 以上）。外すとシートを閉じてからスワイプと同じ「やめた」にし、シートで直した内容は保存しない（BL-146）。各欄のマイクは下の F-04・R-32 の行 | `shoppinglist/EditItemSheet.kt` |
| F-07・R-15 | 写真は「撮る」（標準カメラ＋FileProvider）と「選ぶ」（Photo Picker）。`filesDir/photos/` へ長辺 1600px・Exif 無しの JPEG で保存し（元の写真が読めない・消えた・権限が切れた・保存に失敗したときは落とさず取り込まない）、「買った」では同じファイルを記録から参照。メモの行に写真ありの印 | `data/PhotoStore.kt`、`EditItemSheet.kt` |
| R-14 | 使われなくなった写真の掃除。メモ・お買い物・記録のどれからも参照されていない写真を WorkManager で 1 日 1 回消す（端末を使っていない間・電池残量が少なくない時に限り、「元に戻す」を待つ写真を消さない。記録を消した後の元に戻す（F-16）も、写真のファイルは消さず参照が戻るだけのため同じ理由で足りる）。書いてから 1 日未満の写真は保存前の可能性があるため残す | `data/PhotoCleaner.kt`、`data/PhotoCleanupWorker.kt`、`PhotoDao` |
| R-01・R-02 | 同じ物がメモにあれば追加せず、既存の項目を一瞬光らせて「○○はもうメモにあります」 | `data/ShoppingRepository.kt`、`ShoppingListViewModel` |
| F-10・F-11・D-4・R-05 | 右へスワイプで「買った」、左へで「やめた」。幅の 40% で確定し、超えた瞬間に軽い振動。背景は色＋アイコン＋文字。「やめた」は編集シートの「メモから外す」でもできる（BL-146） | `shoppinglist/SwipeableItemRow.kt`、`shoppinglist/EditItemSheet.kt` |
| PLAN §8 アプリサイズ・起動 | release は R8 で縮小・難読化（`isMinifyEnabled`、アプリ固有の規則は `proguard-rules.pro` で今は無し）。署名なしの release APK は約 3.2MB（上限 15MB）。Baseline Profile は `src/main/baseline-prof.txt`（アプリのコード全体を対象にした広い規則。実機の Macrobenchmark で作り直すのは BL-100）を `profileinstaller` で反映し、ライブラリの規則と合わせて APK の `assets/dexopt/baseline.prof` に入る | `mobile/build.gradle.kts`、`src/main/baseline-prof.txt` |
| PLAN §8 アクセシビリティ | メモの行は読み上げ（TalkBack）の操作の一覧に「買った」「やめた」を出し、スワイプしなくても操作できる（`CustomAccessibilityAction`）。「やめた」は項目の編集シートの「メモから外す（やめた）」ボタンでもできる（BL-146）。アイコンだけのボタン・写真ありの印には説明を付け、文字と並ぶアイコンは説明なし（読み上げの重複を防ぐ）。「元に戻す」は読み上げ利用時に端末の推奨時間まで延ばす（R-04）。状態と操作の意味も読み上げで伝える（BL-115）。カレンダーの選ばれている日は `selected` で「選択中」と読ませ、困ったときの質問は開閉の状態（`stateDescription`「答えを表示中」「答えは閉じています」）と押したときの意味（`onClickLabel`「答えを開く」「答えを閉じる」）、おしらせの期限が近い順の行は `onClickLabel`「期限を決め直す」を付ける。文言は `strings.xml` に置く | `shoppinglist/SwipeableItemRow.kt`・`calendar/MonthGrid.kt`・`settings/FeedbackScreen.kt`・`reminder/ReminderScreen.kt` |
| F-13 | 行の左の ○ でも「買った」（○ を隠す設定は「操作」で切り替える） | `SwipeableItemRow.kt` の `ItemRowContent` |
| F-12・R-04 | 「買った：牛乳 ／ 元に戻す」を 5 秒表示（TalkBack 等の利用時は推奨時間まで延長）。元に戻せるのは最新の 1 件 | `ShoppingListScreen.kt` の `UndoEffect` |
| R-03・R-10・R-11・R-15 | 「やめた」は記録せず品目を残す。「買った」は端末の今日の日付で記録、同じ日の 2 回目も別記録、写真は同じファイルを参照 | `data/ShoppingRepository.kt` |
| R-12 | 購入回数・最後に買った日・平均間隔を残っている記録から計算し直す。記録を消すと紐づくお知らせも消す。消した記録を元に戻すと（F-16）、記録・お知らせ（状態・期限・後での日付）を同じ id で、お買い物が消えていれば同じ id で戻し、集計を計算し直す。お知らせが消えた・戻ったときは次の通知を予約し直す（R-24） | `core/suggest/PurchaseStats.kt`、`data/ProductStatsUpdater.kt` |
| F-16 | 記録の修正（購入日・お店・数量・メモ・写真。日付かお店が変わればお買い物を付け替え）と削除。日の詳細の記録を押すと編集シート。シートは払う・戻る・外を押す・「閉じる」のどれで閉じても保存する（閉じると保存、確認なし。消すときは保存しない）。「この記録を消す」は「閉じる」と同じ行に並べず、区切り線の下のシートの一番下に置く。消すときも確認は出さず、カレンダーの画面の下に「消しました：品名 ／ 元に戻す」を 5 秒（読み上げ利用時は端末の推奨まで延長）出し、押すと記録とお知らせを同じ id で戻して集計を計算し直し、次の通知を予約し直す（戻せるのは最新の 1 件、戻した記録の日を表示） | `data/RecordRepository.kt`（`deleteRecord`・`restoreRecord`・`DeletedRecord`）、`calendar/RecordEditSheet.kt`、`calendar/RecordUndo.kt` |
| F-17 | 後から記録。日の詳細の下（買い物が無い日も）の「買った物を記録する」から、品名・買った日（選んだ日が既定）・お店・数量・メモ・写真を入れて記録する。開くと品名の欄にフォーカスしてキーボードを出す（記録の編集シートは自動でフォーカスしない）。メモは変えず、初めての品名は品目に登録し、購入回数などを計算し直す（R-12）。品名が空のうちは「記録する」を押せない。記録したら記録した日へ移る | `RecordRepository.addRecord`、`calendar/AddRecordSheet.kt` |
| F-40・PLAN §2.7 | 月カレンダー（日曜始まり、日曜は赤・土曜は青の文字色＋曜日名、買い物の日にお店の色の 8dp のドット最大 3 つを数字から 2dp 離して置く、今日は枠線。今月以外を見ているときだけ月の見出しの横に「今日」を出し、押すと今月の今日を選ぶ `CalendarEvent.GoToToday`、BL-137） | `calendar/MonthGrid.kt`、`designsystem/CalendarColors.kt` |
| F-41 | 日の詳細（品名・数量・お店・メモ・写真のサムネイル、写真は押すと拡大）。買い物が無い日は「この日のお買い物はありません」 | `calendar/CalendarScreen.kt` |
| F-42 | 品名検索（正規化名か表示名に含む記録を新しい順に最大 200 件）、結果から該当日へ移る。入口は上部のバーの「🔍 探す」（アイコン＋文字、検索中は「✕ やめる」で読み上げは「探すのをやめる」、題名は 1 行で溢れたら省く、BL-137）。検索欄の右に編集シートの各欄と同じマイク（`FieldMic`、F-04 と同じ権限と認識の仕組み）を置き、聞き取った文字で検索語を置き換える（音声認識が使えない端末では出さない、BL-117） | `RecordRepository.search`、`CalendarViewModel`、`CalendarScreen.SearchPane` |
| PLAN §3 | 下タブ 3 つ（メモ・記録・期限。アイコン＋文字ラベル。上部のバーの題名も同じ。記録はカレンダーで買った物を見返す画面、期限は買った物の期限の一覧） | `IkumemoApp.kt`、`calendar/`、`reminder/` |
| F-50 | 期限の画面の「期限が近い順」の下に最近買った物（今日を含む 7 日以内・期限未設定）と「期限を決める」（3日後／1週間後／2週間後／1か月後／日付を選ぶ）。4 つの候補のチップは 2 行で、下の行に今日から数えた日付を曜日付きで出す（`reminder_due_choice_date`「11月20日（金）」。曜日は端末の言語によらず日本語、値は純粋な関数 `DueChoice.dateParts`。「日付を選ぶ」は 1 行、チップの高さは 48dp 以上）。最近買った物は最初の 3 件だけ出し、残りは「ほか N 件を見る」で全部出す（開いた状態は `rememberSaveable` だけで保存しない）。各行の「期限なし」でその記録を一覧から外し（記録は消さない）、「期限なしにしました：品名 ／ 元に戻す」を 5 秒出す。外した id は `ReminderPrefs` に置く。品目ごとに覚えて次から出さない仕組みは無い。記録画面からの設定は未実装 | `reminder/ReminderScreen.kt`、`RecentSection.kt`、`DueSheet.kt`、`ReminderViewModel.kt`、`data/ReminderRepository.kt`、`data/ReminderPrefs.kt` |
| F-53・R-22 | 期限が近い順の一覧（通知オフの帯の下、最近買った物より上。期限切れの有無によらず常にこの順）。期限切れは error 色＋「期限切れ」、今日・明日は文字で示す。「使い切った」「捨てた」で片付け、行を押すと決め直し | `reminder/ReminderScreen.kt` |
| R-29 | 通知が許可されていなければ「通知がオフのため、お知らせが届きません」＋「通知をオンにする」（端末の通知の設定を開く） | `ReminderScreen.kt` の `NotificationsOffBanner` |
| F-01・F-02・PLAN §4.4 | 音声テキストの分割・数量抽出・不要語除去・誤変換補正・N-best の選択 | `core/voice/VoiceTextParser.kt`、`QuantityParser.kt` |
| F-01・PLAN §3.2 | 🎤 話して追加（ホーム下部、64dp）→ 聞いている間は途中の文字を表示 → 確認カード（タップで直す、× で外す。外したカードは品名・数量に取り消し線を引いて onSurfaceVariant で薄くし、右のボタンを「↩ 戻す」（アイコン＋文字、48dp 以上）に替える。読み上げは「〇〇を外す」「〇〇を入れる」）→「Nつ追加する」（入れる件数を出す。0 件は「追加する」で押せない。画面に出る「つ」の件数表記は空白なしの「3つ」にそろえ、数字の前にも空白を入れない「の2つ」とする、オーナー判断 2026-11-13、BL-154・BL-155） | `voice/VoiceInputSheet.kt`、`VoiceInputViewModel.kt` |
| R-30・R-31 | 日本語固定・オンデバイス優先。何も話さなければ 5 秒で止め「うまく聞き取れませんでした」＋もう一度話す／文字で入れる | `voice/SpeechEngine.kt`、`VoiceInputViewModel.kt` |
| R-01（音声） | 確認カードでメモにある物に「メモにあります」の印を付け、既定で外す | `VoiceInputViewModel.showCards` |
| PLAN §2.10・§5 | マイクの許可は初めて使う時に理由を添えて求める。拒否されたら端末の設定を開くボタンと文字入力の案内。機能の無い端末ではマイクを出さない | `voice/VoiceInputSheet.kt`、`ShoppingListScreen.kt` の `AddButtons` |
| F-04・R-32 | 編集シート（メモの項目・カレンダーの記録）の各入力欄の横にマイク。聞き取った文字は分割せずに入れる（品名も分けない）。1 行の欄は置き換え、メモは改行して足す。聞くのは一度に 1 欄。音声認識が無い端末では出さない | `voice/FieldDictation.kt`、`shoppinglist/EditItemSheet.kt` の `EditField` |
| F-20・F-21 | 追加シートに入力中の候補（正規化名の前方一致を先、部分一致を後）と「いつもの」チップ（購入回数 → 最近買った順 → 正規化名、最大 12 件）。メモにある物は出さない。チップは押すたびに 1 件入る | `core/suggest/Suggestions.kt`、`ShoppingListViewModel.addSuggestions`、`ItemSheets.kt` の `AddItemSheet` |
| F-14・F-30・F-31 | メモ上部のお店チップ → いつものお店から選ぶシート（最近使った順の上位 5 件・ほかのお店・新しいお店・選ばない）。選んだお店は「買った」に付く | `shoppinglist/StorePicker.kt`、`data/StoreRepository.kt` |
| R-06 | 選んだ時か最後の「買った」から 3 時間、または日付が変わったら選んだお店を解除（表示は 1 分ごとに見直す） | `data/AppSettings.kt` の `StoreSelection`、`StoreRepository` |
| R-16 | お店の管理: お店は消さずに「使わない」印（確認を 1 回出す、記録のお店名は残す）、「ほかのお店とまとめる」で記録・メモ・お買い物を選んだお店へ移す | `settings/StoreManageScreen.kt`、`StoreRepository.archive` / `merge` |
| F-23（非表示のみ） | よく買う物の管理: 買った回数の多い順に並べ、品目ごとに「候補に出す」を切り替える（オフはいつもの・入力中の候補に出ない）。表記ゆれの統合は未実装（S） | `settings/ProductManageScreen.kt`、`ProductRepository.setHidden` |
| R-20〜R-23・R-27・R-28 | 通知日（前日・当日〔オフ可〕・後での翌日）、同じ日の 1 通へのまとめ、次の通知日時（電源オフ後はすぐ出す） | `core/reminder/ReminderSchedule.kt` |
| F-51・R-24・R-25 | 次の通知時刻を `setAndAllowWhileIdle` で 1 件だけ予約。起動時と再起動・時刻変更・タイムゾーン変更・アプリ更新で予約し直す。チャンネルは「期限のお知らせ」1 種。押すとお知らせのタブ | `reminder/ReminderScheduler.kt`、`ReminderReceivers.kt`、`ReminderNotifier.kt` |
| F-52・R-26・R-27 | 1 品目の通知だけ「使い切った」「また買う」（メモへ入れて使い切った）「後で」（翌日にもう一度） | `reminder/ReminderActions.kt` |
| PLAN §5・R-29 | POST_NOTIFICATIONS は Android 13 以降で初めて期限を決めた時に、見出し「通知を使います」と「期限の前の日の（お知らせの時刻、`ReminderUiState.notifyTime`）に、スマートフォンの通知でお知らせします。次の画面で『許可』を押してください。」の説明（「次へ」／「今はしない」、引数だけで描ける `NotificationRationaleDialog`）を出してから求める。案内からは 2 回まで求め、その後は端末の設定 | `reminder/NotificationPermission.kt` |
| F-60・F-63・F-65 | 着せ替え（各テーマの色で描いた見本のカードでテーマを選ぶ、暗い画面「端末に合わせる / 明るい / 暗い」、文字の大きさ 3 段階）。画面の並びは 文字の大きさ → テーマ → 暗い画面 → 背景の写真（F-61）の順で、文字を大きくしたい人がスクロールせずに見つけられるよう文字の大きさを一番上に置く。選んだらすぐ保存しアプリ全体へ反映 | `settings/AppearanceScreen.kt`、`data/AppearancePrefs.kt`、`MainActivity.kt` |
| F-61 | 背景の写真（着せ替えの「写真を選ぶ」「外す」、Photo Picker）。`filesDir/background/` へ写真と同じ形（長辺 1600px・Exif 無し）で取り込み、差し替え・解除で前の画像を消す。画面いっぱいに写真を描き、その上にテーマの背景色の半透明の膜（濃さ 0.3〜0.9、既定 0.6。スライダーを離すと保存）を敷く。暗い画面では膜を 0.15 濃くする（上限 0.95）。メモの行・上部のバー・下タブは不透明のまま写真を覆う。音声入力シート（他アプリの上に出す）とウィジェットには描かない。**ペンの色（BL-108）**：背景の写真があるときだけ、文字の色を おまかせ（テーマの色）・くろ・あお・あか・しろ から選べる（`InkColor`、保存キー `appearance_ink`、書き出しの設定にも入れる）。選ぶと写真の上に直接置く文字（`onBackground` と `LocalContentColor`）をその色にし、膜の色も合わせる（くろ・あお・あかは明るい膜 #FFFBFF、しろは暗い膜 #1C1B1F。いずれも文字との比が 4.5:1 以上）。メモの行・バー・下タブなどテーマの面の上の文字は変えない。**一覧の面（BL-130）**：背景の写真があるときだけ、記録（カレンダー）の月の表（`MonthHeader`・`MonthGrid`）、日の詳細（見出し・記録の行・「買った物を記録する」）、検索の欄と結果、期限（おしらせ）の各セクションの行（期限が近い順・最近買った物と「ほか N 件を見る」・空のときの案内・「おしらせが届かないとき」）に、テーマの surface を不透明に敷く（`ReadablePanel`。LazyColumn の行ごとに先頭・途中・末尾の角を分けて 1 枚に見せる。左右 8dp・角 12dp）。面の中の文字は onSurface に戻るため、ペンの色に関わらず CalendarColors・onSurfaceVariant・error・primary をテーマの面の上と同じに読める。期限のセクションの見出しは写真の上に残す。写真が無いときは面を敷かず見た目を変えない | `designsystem/BackgroundScrim.kt`、`designsystem/ReadablePanel.kt`、`AppThemeHost`、`AppearanceViewModel` |
| PLAN §3（一部） | メモ・記録・期限の 3 つのタブの上部のバーの右に「⚙ 設定」（文字付き、共通の `SettingsButton`。記録は「探す」の右）。項目は 8 つ（BL-143）。一番上の「使い方」は見出し無しで置き（H-01）、残りは見出し（`semantics` の heading）で区切る。「見た目と操作」に「着せ替え」「操作とお知らせ」「ホーム画面に置く」、「登録したもの」に「お店の管理」「よく買う物の管理」「データ」、「このアプリ」に「このアプリについて」。使い方以外の各行の下に 1 行の説明（onSurfaceVariant の bodyMedium、例「テーマ・文字の大きさ・背景の写真」）を出す。区切りは `SettingsSection`、説明は `SettingsItem.detail`（BL-139） | `settings/SettingsScreen.kt`、`settings/SettingsNavigation.kt`、`IkumemoApp.kt` |
| F-77（GitHub のみ） | ご意見・ご要望: 「このアプリについて」の中の行から開く（設定の一覧には置かない。使い方の「やってみる」からも開く）。よくある質問 4 つ（押すと答え）→ GitHub の Issue を開く。アプリの版・Android の版・機種名を画面に出しコピーできる。ご意見フォームは site/ の転送ページができるまで置かない（オーナー判断） | `settings/FeedbackScreen.kt` |
| PLAN §3・§5・§7 | このアプリについて: 版・名前の由来・ご意見・ご要望・ライセンス一覧（AboutLibraries）・プライバシーポリシー・GitHub・ストアのページ（評価のお願い） | `settings/AboutScreen.kt`、`settings/LicensesScreen.kt`、`settings/ExternalLink.kt` |
| F-13・R-05・PLAN §3 操作 | 「操作とお知らせ」の 1 画面に、○ の表示と振動のオン/オフ（初期値はどちらもオン）、続けてお知らせの 2 行を並べる。ViewModel は操作とお知らせで分けたまま、DataStore のキーも変えない。○ を消した行は左に余白を取る | `settings/OperationScreen.kt`、`data/OperationPrefs.kt`、`shoppinglist/SwipeableItemRow.kt` |
| R-20・R-21・PLAN §3 お知らせ | 通知の時刻（時計の形で選ぶ、24 時間表示）と期限当日のお知らせのオン/オフ（「操作とお知らせ」の中の行）。保存したら次の通知を予約し直す。期限の画面の一覧の上（通知オフの帯の下）に「🕘 お知らせの時刻 9:00」（設定値）を出し、押すと「操作とお知らせ」を開く | `settings/ReminderSettingsScreen.kt`（`ReminderSettingsRows`）、`reminder/ReminderScreen.kt` |
| W-01・R-42・R-44・R-45 | 1×1 ウィジェット「話して追加」（横 2 マス以上で文字を横並び）。押すと透明な専用 Activity で音声入力シートが重なって開き、追加したらトーストを出して閉じる。途中で閉じたら何も追加しない。音声入力が使えない端末では「書いて追加」でアプリの追加シートを開く。色は着せ替えのテーマと暗い画面の設定に合わせる | `widget/VoiceWidget.kt`、`widget/WidgetColors.kt`、`voice/VoiceSheetActivity.kt` |
| W-02・R-40・R-43・D-18 | メモ一覧ウィジェット（表示専用）。高さ 2 マスは見出し（メモの件数・期限が近い件数）と先頭の品名を横に数件＋「ほか N件」（幅が狭いと 2 件、広いと 3 件）、3 マス以上は選んだお店と品名の縦一覧（数量付き、幅が狭いと数量を下に）。メモ帳風の罫線（見出しの下にテーマ色の太め 2dp の線、行ごとにうすい 1dp の横線 `WidgetColors.rule`、大きい表示は左に 2dp の余白の縦線。レイアウトの指定は無かったため既定として選んだ、2026-10-25）。ウィジェットの色は本体と同じテーマの `background`・`onBackground`・`primary`、角の丸みも本体のテーマ（メモ一覧は large、1×1 は medium）に合わせる（BL-112）。見出しの右端に更新ボタン（48dp、押すと矢印が回る表示（`CircularProgressIndicator`、最低 0.8 秒）に変わり、描き直しが終わると戻る。内容と色は `provideContent` の中で流れてくる最新の値を使う（表示中のセッションが再利用されて `provideGlance` が呼び直されないと古いままになるため、BL-110）。`RefreshMemoAction` が `WidgetUpdater.refresh` を呼んで両ウィジェットを描き直す。空でも出す）。空なら「メモは空です」。行はその項目まで送って光らせ、「期限が近い」はお知らせ画面、ほかはメモ画面を開く。既定の大きさは 4×4（長押しで 3×2〜5×6、4×2 へも小さくできる。オーナー判断 2026-10-25） | `widget/MemoListWidget.kt`、`widget/MemoWidgetState.kt`、`res/xml/widget_memo_info.xml` |
| W-03 | 設定「ホーム画面に置く」。ウィジェット 2 種の見本（話して追加 1×1・メモ一覧 4×4）と「置く」（`requestPinAppWidget`）。置く大きさは指定できないため各ウィジェットの既定の大きさで置かれ、4×2 は置いた後に長押しで小さくできると説明だけ添える。ホーム画面アプリが対応していなければボタンの代わりに置き方を説明し、最後にアイコン長押しのショートカットを案内する。初めての案内の最後の「置く」は BL-080 で作る | `settings/HomeScreenScreen.kt`、`widget/WidgetPinner.kt` |
| F-74 | アイコン長押しのショートカット「話して追加」「文字で追加」「記録」「期限」（長い名前は「買った物の記録を開く」「期限の一覧を開く」）。各画面を `MainActivity.intent` で開く（「話して追加」はメモ画面で音声入力を始める）。debug 版は applicationId が違い静的な shortcuts.xml にはパッケージ名を固定で書くしかないため、起動時に動的なショートカットとして登録し直す | `AppShortcuts.kt`、`res/drawable/ic_shortcut_*.xml` |
| F-70・G-01 | 初めての案内。初回起動時（保存値を読み終えてから）だけ全画面で出す。「話すだけでメモ」「買ったらスワイプ」「後から見返せる」の 3 枚（3 枚目に「予定表やなんでもリマインダーではなく、買い物のためのメモです」を添える。使い方の冒頭 start.md も同じ）と、最後に「ホーム画面に置く」（W-03 の「置く」2 つと「後で」。非対応なら置き方の説明）。「飛ばす」は常に出し、飛ばす・後で のどちらでも見終えたことにする。権限は求めない。通知・ウィジェット・ショートカットから起動したときは挟まずにその画面を開く（次の通常の起動で出す）。1 枚目は「牛乳とたまご」が 2 つに分かれる手本を 3 回くり返して分かれた形で止まる（`VoiceSplitDemo`） | `guide/OnboardingScreen.kt`、`guide/GuideViewModel.kt`、`IkumemoApp.kt` |
| R-50・R-52・R-53・R-55 | その場のガイドを出すかの判定 `GuidePolicy.next`（きっかけを満たすガイドのうち未表示の最初の 1 つだけ。音声入力中・シート表示中・外から開いた直後・「ガイドを出さない」・初めての案内の前は出さない）。「ガイドを出さない」は表示済みと止める設定を 1 回で保存する（`GuidePrefs.disableAll`） | `guide/Guide.kt`、`data/GuidePrefs.kt` |
| G-07・G-08 | カレンダーとお知らせのその場のガイド（`ScreenGuides`）。G-07 は見ている月に買い物のあった日があるとき、その月の最初の買い物の日を照らす（検索中・記録のシート表示中は出さない。先に日付を押したら出さない）。G-08 は「期限なし」で外した記録を除いた最近買った物があるとき（初めての「買った」の後）、その 1 行目の「期限を決める」のボタンを照らす（期限の設定シート表示中は出さない。先に「期限を決める」を押したら出さない）。通知・ウィジェット・ショートカットから開いた画面にいる間は吹き出しを出さない（表示済みにもしないので、ほかの画面へ移ってから自分で開くと出る、R-52） | `guide/ScreenGuides.kt`、`CalendarViewModel`、`ReminderViewModel`、`MonthGrid.kt`、`ReminderScreen.kt` |
| H-01〜H-03 | 使い方。設定の一番上に「使い方」。目次（PLAN §2.12 の章立て。未実装の「LINE などで送る」は載せない）から 1 ページずつ文で説明し、通信なしで見られる（画面の絵はスクリーンショットテスト BL-062 が保留のため無い）。各ページの下に「ガイドをもう一度見る」（そのガイドを未表示に戻して画面を開く）と「やってみる」、目次の先頭に「初めての案内をもう一度見る」。メモが空の画面・音声が使えない機種の表示・お知らせ一覧の「お知らせが来ないときは」から関係するページへ移る。本文の正本は `mobile/src/main/assets/help/` の Markdown（見出し・段落・箇条書きと `<!-- try: 画面; guide: g02 -->` だけ）で、`HelpParser` で読む。Web 版 `docs/USER_GUIDE.md` は `scripts/make-user-guide.py` で作り、品質ゲートで一致を確かめる | `help/HelpContent.kt`、`help/HelpScreen.kt`、`assets/help/`、`scripts/make-user-guide.py` |
| G-02〜G-06・R-51・R-54・R-56 | メモ画面のその場のガイド。照らす場所（🎤・練習項目・一番上の項目の ○・「元に戻す」・お店チップ）の位置を `guideTarget` で集め、`GuideOverlay` が周りを暗くして吹き出し（「分かった」「ガイドを出さない」）を出す。暗くするのは見た目だけで、照らした部品はそのまま操作できる。きっかけは `MemoGuides.candidates`（空のメモ → G-02、項目が入った → G-03、G-03 の後 → G-04（○ を出す設定のときだけ）、初めての「買った」の「元に戻す」→ G-05、G-05 の後でお店未選択 → G-06）。G-03 の間はメモの一番上に「練習：にんじん」（ID -1、DB に入れない）を出し、スワイプ・○ で練習を終える（記録・品目・元に戻すに残さない）。先に 🎤 を押す・本物の項目を動かす・元に戻す・お店を選ぶ画面を開くと、そのガイドは出さない。シート表示中（音声入力を含む）とウィジェットの行から開いて光らせている間は出さない。読み上げ（タッチで探索）を使っているときは照らさず画面下のカードで出し「分かった」へ移動する。G-03 の間は練習項目を右へ少し動かして戻す手本を 2 回見せて止まる（`SwipeNudge`）。どちらの手本も、端末の「アニメーションを減らす」（`ANIMATOR_DURATION_SCALE` が 0）では動かさない | `guide/GuideOverlay.kt`、`guide/MemoGuides.kt`、`ShoppingListViewModel`、`ShoppingListScreen.kt` |
| R-41 | アプリが動いている間はメモ・お知らせ・お店・見た目の変化で描き直す。日付の変わり目と選んだお店の解除は省電力のアラーム（`set`）で描き直す。定期的な自動更新はしない | `widget/WidgetUpdater.kt` |
| F-72 | 設定の「データ」→「データを書き出す（写真も含む）」、折りたたみ「ほかの形式で書き出す」の中の「データを書き出す（写真なし）」「買った記録を表計算用に書き出す」、「データを読み込む」。ZIP は `ikumemo.json` と `photos/`・`background/` の画像、JSON は写真のパスを空にしたもの、CSV は買った記録だけ（UTF-8 の BOM 付き、`=` `+` `-` `@` で始まる欄は `'` を足して式にならないようにする。読み込みには使えない）。保存先と読み込むファイルは Storage Access Framework で利用者が選び、アプリは通信しない。読み込みは今のデータをすべて置き換える（確認を 1 回出す）。外から来たファイルとして、一時フォルダへ取り出したうえで `BackupValidator` が版・ID の重複・外部キー・写真のパス（`photos/` か `background/` 直下の安全な名前だけ）・日付の範囲を検査し、通ったときだけ 1 つのトランザクションで入れ替える（失敗しても今のデータは変わらない）。大きさの上限は全体 2GB・JSON 64MB・写真 1 枚 32MB・項目 10 万。ファイルに無い写真のパスは空にする。設定は見た目・操作・おしらせの時刻だけを入れ、ガイドの表示済みと選んでいるお店は入れない。読み込んだあとはお知らせを予約し直し、ウィジェットを描き直す。選んだファイルを開けない・読み書きできない（入出力の失敗、権限の取り消しなどの SecurityException、扱えない Uri の IllegalArgumentException）ときは落ちずに失敗を知らせる。画面は見出し「書き出す」「読み込む」「消す」で区切る。「書き出す」は写真も含む 1 行を表に出し、その下の「ほかの形式で書き出す」（押すと開閉、矢印の向きと読み上げの状態で示す。開いた状態は保存せず、写真なし・表計算用の処理中は閉じていても出す）を開くと写真なし・表計算用の 2 行が出る。説明には ZIP・JSON・CSV の語を出さず「いくメモで読み込めるファイル」「表計算ソフト（Excel など）で開けるファイル」と書く。結果の知らせは一番上（最初の見出しの上）に出す。処理中は押した行（`DataUiState.running` の `DataTask`）に `CircularProgressIndicator` と「書き出しています…」「読み込んでいます…」「消しています…」を出し、ほかの行は押せない | `data/backup/`（`BackupExporter`・`BackupImporter`・`BackupValidator`・`RecordCsv`・`BackupSettingsStore`）、`data/db/BackupDao.kt`、`settings/DataScreen.kt`、`settings/DataViewModel.kt` |
| F-76・R-43 | 設定の「データ」→「データをすべて消す」。確認を 1 回だけ出し（取り消せない操作の例外、§1.3-4）、DB（`clearAllTables`）・設定（DataStore）・写真と背景画像のファイルを消して初めて使うときの状態に戻す。消したあとはお知らせの通知と予約を片付け、ウィジェットを空の表示に更新する。行は読み込みの下に間（32dp）と区切り線を空けた「消す」の見出しの下に離し、行の題と確認ダイアログの「すべて消す」の文字を `colorScheme.error` にする（読み込みの確認は色を変えない） | `settings/DataScreen.kt`、`settings/DataViewModel.kt`、`data/DataEraser.kt`、`AppContainer.dataEraser` |
| F-71 | 自動バックアップ。クラウドからは写真（`filesDir/photos/`）だけを外し、DB・設定・背景画像は残す。端末間転送（Android 12 以降）は全部を移す。Android 11 以前は区分を分けられないため写真を外すだけ | `res/xml/data_extraction_rules.xml`、`res/xml/backup_rules.xml` |
| PLAN §4.1 | 手動 DI（`IkumemoApplication` が `AppContainer` を 1 つ持つ） | `AppContainer.kt`、`IkumemoApplication.kt` |
| PLAN §4.5・§6 | バージョンの正本 `scripts/version.properties`、`-PappVersionCode` / `-PappVersionName` での注入、リリースビルドスクリプト | `scripts/version.properties`、`scripts/release-build.ps1` / `.bat`、`mobile/build.gradle.kts` |
| PLAN §6 | リリース署名は手元の PC だけに置き、`local.properties` から読む。未設定なら署名なし | `mobile/build.gradle.kts` の `signingConfigs` |

## 設計方針

### アーキテクチャ方針

- モジュール: `core`（純 Kotlin/JVM、Android 非依存、パッケージ `com.ikumemo.core`）と `mobile`（アプリ本体、
  `com.ikumemo.mobile`）。feature ごとの Gradle モジュール分割はしない（PLAN §4.5）
- `mobile` 内は画面ごとのパッケージに分ける。`settings/`（設定の枠と着せ替え）・`designsystem/`・`data/`（品目は `ProductRepository`）・`shoppinglist/`・`calendar/`・
  `reminder/`・`voice/`・`widget/`・`guide/`（初めての案内とその場のガイド）・`help/`（使い方）。PLAN §4.5 の構成と同じ
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
- 外部のページ（プライバシーポリシー・GitHub・ストア）は `ExternalLink` に集め、利用者が押した時だけブラウザで開く（アプリ自体は通信しない）
- 依存の版は sesami-wear と同じ版にそろえる（AGP 8.13.0、Kotlin 2.0.21、Compose BOM 2024.12.01 等。
  `gradle/libs.versions.toml`）。必要になったマイルストーンで追加し、使わない依存は入れない

### お知らせの計算方針（core/reminder）

- 通知の予約は「次の 1 件」だけ。`ReminderSchedule.nextNotificationAt` が、未来の通知日は設定時刻（初期値 9:00）、今日は
  通知時刻前なら設定時刻、過ぎていて最後に通知した日が今日でなければ「いま」を返す（R-28）
- 1 通の見出しは先頭の品名（期限の近い順 → 品名順）と「ほか N つ」。種類は急ぐ順（期限切れ → 今日 → 明日）で決める
- 期限を決め直したら状態を未対応へ戻し「後で」を消す。`ReminderRepository` は書き換えのたびに `onChanged` を呼ぶ
- 「後で」は `snoozedUntil` に翌日を入れるだけで期限は変えない。期限を過ぎた分は通知せず一覧に残す（R-22）

### 設定の保存方針（DataStore）

- 端末内の設定は DataStore Preferences（ファイル名 `ikumemo_settings`、`AppSettings.createDataStore` で 1 つだけ作る）に置き、
  用途ごとのクラスで共有する。お店の選択は `AppSettings`、お知らせ（通知時刻 `reminder_time_minutes`・当日のオン/オフ・最後に通知した日・
  許可を求めた回数）は `ReminderPrefs`。品目ごとに持たない値だけを入れる
- 「期限なし」で最近買った物から外した記録の id は `ReminderPrefs` の `reminder_hidden_recent_ids`（文字列の集合）に置く。
  DB の版を上げないため DataStore に置き、保存のたびに最近買った物（7 日以内・期限未設定）に無い id を掃除する。
  7 日で消える一時的な値のため、設定の書き出し・読み込み（`BackupSettingsStore`）には含めない
- 選んでいるお店は `selected_store_id`・`selected_store_at`・`last_bought_at`（epoch ミリ秒）で持ち、解除の判定は読むたびに行う。
  「買った」に付けるときに解除の条件を満たしていれば保存値も消す
- ガイドは `GuidePrefs`（`guide_onboarding_done`・`guide_disabled`・`guide_shown` はガイドの `storageKey`「g02」〜の集合）。
  知らないキーは読み飛ばし、新しいガイドは `GuideId` に足すだけで既存の利用者にも 1 回だけ出る（R-55）
- テストは `testSettings()`（テストごとに別の一時ファイル）で作る

### 表記ゆれ方針（core/text）

- 同じ物の判定は `ProductNameNormalizer.normalize()` の結果の一致だけで行う（数量は比べない、R-02）
- 読み辞書は長い語から優先して置き換える（「糸蒟蒻」は「いと＋こんにゃく」ではなく「いとこんにゃく」）。辞書に無い漢字は
  そのまま残す
- 別の物が同じ読みになる語（「酒」と「鮭」など）は辞書に入れない。同じ物の別表記（「卵」「玉子」）だけを同じ読みにする
- 辞書は自作で、既存の辞書データや他社の商品リストは写さない（PLAN §4.4）

### 音声テキストの分割方針（core/voice）

- 区切りは「と」「、」「。」「・」「それから」「あと」「それと」と空白。NFKC の後で照合する
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
- カレンダーの曜日の色とお店のドットの色は `CalendarColors`（テーマに依らない固定値）。全テーマの surface・background の上で
  曜日の文字 4.5:1、ドット 3:1 以上を `CalendarColorsTest` で確かめる。お店の色はお店の ID から 6 色を順に割り当てる（F-32 は v1.x）
- 角の丸み: ポップ・やさしい 12/20/28dp、クール 4/8/12dp、シンプル 8/12/16dp（small/medium/large）
- 初期テーマはポップ（PLAN に既定値の指定が無いため D-8 の並びの先頭）。暗い画面の初期値は「端末に合わせる」、文字の大きさは「普通」
- 見た目の設定は `AppearancePrefs`（`appearance_theme`・`appearance_dark_mode`・`appearance_text_size`）。`MainActivity` は保存値を
  読み終えてから描く（起動直後に既定のテーマが一瞬見えるのを防ぐ）
- テーマ・暗い画面・文字の大きさは `storageKey`（`pop` / `gentle` / `cool` / `simple`、`system` / `light` / `dark`、
  `normal` / `large` / `extra_large`）で保存し、未知の値は既定値へ戻す
- フォントは端末標準を使い、同梱しない（PLAN §2.7）
- 操作の設定は `OperationPrefs`（`operation_show_bought_button`・`operation_vibration`）。メモの行へは `LocalOperationSettings` で渡す
- 設定の切り替えは `SettingsSwitchRow`（行全体を `toggleable` にし、スイッチ自体は押せなくして読み上げの重複を防ぐ）
- ウィジェットは Glance（`glance-appwidget`）。アプリと同じパレットの昼・夜の色を `ColorProvider` で渡し、背景画像は使わない
- ウィジェットやショートカットから開く画面は `MainActivity.intent(context, screen)` の `EXTRA_OPEN_TAB`（項目は `itemIntent` の
  `EXTRA_ITEM_ID`）で指定し、`ScreenRequest` として受け取る。開くときは下タブと同じ切り替え方
  （開始画面まで戻し、タブごとの状態を保存・復元する `navigateToTab`）にする（メモの上に積むと、メモのタブを押したときに
  保存した履歴が戻ってその画面へ戻ってしまう）。開く先ごとに data の URI（`ikumemo://open/...`）を変え、
  PendingIntent が上書きし合わないようにする

### データ設計方針

- 「買った」「やめた」「元に戻す」は `ShoppingRepository` の 1 トランザクションで行う。元に戻すときはメモの項目を同じ ID・
  並び順で入れ直し（同じ品目がすでにメモにあれば入れない）、「買った」の記録と使われなくなったお買い物を消して集計し直す

- Room（`IkumemoDatabase`、版 1）。テーブルは `store`・`product`・`shopping_item`・`shopping_trip`・`purchase_record`・
  `reminder` の 6 つ。列は PLAN §4.3 のとおりで、ID は自動採番の `Long`
- 型の保存形式: `LocalDate` は epochDay、`Instant` は epoch ミリ秒、お知らせの状態（`PENDING` / `USED_UP` / `DISCARDED`）は列挙名
- 品目は `normalizedName`（core/text の正規化名）を一意にし、同じ物の判定に使う（R-02）。購入回数・最後に買った日・
  平均間隔は記録から計算し直す値として持つ（R-12）
- 写真の列（`photoUri`）は `photos/<UUID>.jpg` の相対パス。元の URI は保持しない。取り込みは向きを画素へ反映 → 縮小 →
  Exif を書かない JPEG（品質 85）を一時ファイルへ書いてから置き換える
- 外部キー: 記録を消すとお知らせを CASCADE で消す（R-12）。お店は行を消さず `archived` を立てる（R-16）ため、記録・メモ・
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
- スクリーンショットテスト（Roborazzi 1.38）は `testDebug/.../screenshot/MainScreensScreenshotTest`。メモ・カレンダー・お知らせを
  4 テーマ × ライト/ダーク × 文字の大きさ 3 段階（72 枚、360×640dp・mdpi）で記録し、正解画像を `mobile/screenshots/<画面>/` に置く。
  アプリの外側の Scaffold の代わりにテーマの背景色の `Surface` を敷いて描く。遅いテスト（`SlowTest`）扱いで、メモリが足りないため画面ごとに流す
- テーマの外の色（カレンダーの曜日・お店の点）は、端末の設定ではなく `LocalDarkTheme`（`IkumemoTheme` が着せ替えの「暗い画面」を反映して渡す）で明暗を決める
- Gradle は Kotlin のコンパイルを Gradle の中で行い（`kotlin.compiler.execution.strategy=in-process`）、並列の作業を 2 つまでにする（PC のメモリが 8GB のため）
- 単体テストの Application は `TestIkumemoApplication`（`robolectric.properties` で指定。起動時の通知の確認などを行わない）
- テスト用 DB（`inMemoryDatabase()`）は実行スレッドを DB ごとに持つ（共有プールに前のテストの処理が残って止まるのを避ける）。
  写真の読み書きのテストは `@GraphicsMode(NATIVE)` で動かす
- DAO テストは Robolectric（4.14.1 が targetSdk 36 未対応のため `robolectric.properties` で `sdk=35`）とインメモリ DB で行う

### UI/UX 方針

- 表示する言葉の表記: 想定ユーザーは大人のため、漢字で書ける語は漢字で書く（「お知らせ」「設定」「着せ替え」「操作」「買い物メモ」
  「戻る」「明日」「今日」など。2026-10-25 に平仮名から改めた）。残す平仮名は意図のある名前と語感だけ（「いつもの」「いつものお店」
  「やめた」「いくメモ」テーマ名「やさしい」、品名の例「たまご」）。PLAN.md の用語集（§2.10）の平仮名優先の方針はこの表記が上書きする
- 下タブと画面の名前は中身に合わせて「メモ」「記録」「期限」（2026-11-13 に「カレンダー」「お知らせ」から改めた。BL-141）。
  予定表や用事のリマインダーと誤解されないためで、予定・用事のメモは扱わない。通知そのもの（通知のチャンネル名・お知らせの時刻・
  「お知らせが来ないときは」・設定の「お知らせ」）は「お知らせ」のまま。テストの目印（`TopLevelTabTags`）と route の名前は変えていない
- 画面の配置は `docs/design/WIREFRAME.md`（主要画面・シートの配置と、その場のガイドで照らす場所の目印）
- 文言はすべて `strings.xml` に置く（日本語のみでも。PLAN §4.1）
- ダイアログのボタンは、日付・時刻を選ぶ確定が「決める」（`common_decide`）、取り消しはすべて「戻る」（`common_back`）。
  メモの「やめた」と紛らわしい「やめる」は使わない（BL-133）。操作の名前を確定に書く確認（「すべて消す」「読み込む」など）はそのまま。
  確定は右・取り消しは左の Material の既定の並び
- 各画面の `TopAppBar` は `windowInsets = WindowInsets(0)` とし、システムバーの余白は外側の `Scaffold`（下タブを持つ）の
  `innerPadding` で取る
- UI テストとその場のガイドの目印は `testTag` で付け、名前は各画面の `TestTags` に集める
- 押せる部分は最小 48dp、主要ボタンは 56dp 以上（PLAN §1.3-2）

## 非機能要件

- 性能・信頼性: 記録 2 万件で品名検索と月の記録の取得がそれぞれ 1 秒未満（`RecordSearchPerformanceTest`）。
  そのほかの目標（PLAN §8）のうち、起動時間は実機での計測（BL-100）で確かめる
- セキュリティ／プライバシー: `AndroidManifest.xml` で宣言する権限は `RECORD_AUDIO`・`POST_NOTIFICATIONS`・
  `RECEIVE_BOOT_COMPLETED`（音声認識サービスを見つけるための
  `<queries>` の `android.speech.RecognitionService` も宣言）。INTERNET・位置情報・CAMERA・VIBRATE・SCHEDULE_EXACT_ALARM は
  使わない方針（PLAN §5）。録音は端末の音声認識サービスの中で行われ、本アプリは音声を保存しない。公開している画面は
  起動用の `MainActivity` と、OS からの通知（再起動・時刻変更・タイムゾーン変更・アプリ更新）を受ける `ReminderBootReceiver` だけ。
  通知時刻とボタンの受信は `exported=false`。PendingIntent はすべて `FLAG_IMMUTABLE`。
  FileProvider（`${applicationId}.fileprovider`、非公開・URI 権限の付与のみ）はカメラの一時ファイル `cacheDir/camera/` だけを渡す
- 署名鍵・`local.properties`・`secrets.properties`・`*.jks` / `*.keystore`・`.env*` は `.gitignore` で除外する

## 実装制約

### 技術制約

- アダプティブアイコンは aapt2 が `-v26` 付きのフォルダでしか解決しないため、minSdk 26 でも
  `mipmap-anydpi-v26` に置き、Lint の `ObsoleteSdkInt` を `mobile/lint.xml` で除外する
- detekt は sesami-wear の設定（`maxIssues: 0`、`MagicNumber` 無効、`LongMethod` 60）に加え、`@Preview` 関数を
  `UnusedPrivateMember` の対象外にする
- 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」が正本（ktlintCheck・detekt・lintDebug・
  testDebugUnitTest :core:test・assembleDebug、markdownlint、記録ファイルの検証、使い方の Web 版、秘密情報スキャン）。
  `scripts/quality-gates.py` が表と同じコマンドを順に流して合否を短くまとめ、`scripts/scan-secrets.py` がコミット前の
  秘密情報スキャン（変更行と未追跡のファイル、署名鍵のファイル名。値は表示しない）を行う。自律ループの各イテレーションは
  `--quick --tests` の軽い検証（ktlint・detekt・コンパイル・指定したテストだけ）、フル検証はループの最後と止める前に流す。時間のかかるテストは
  `@Category(SlowTest::class)` を付け、`-PslowTests` の時だけ流す。Gradle のビルドキャッシュを使う
- `.claude/settings.json` は、記録ファイル 3 つの編集に加え、品質ゲートのコマンド（`./gradlew` の各ゲートと `--stop`、
  `python scripts/` の検証・秘密情報スキャン、`npx markdownlint-cli2`）と読み取り系の git（status・diff・log・
  `branch --show-current`）を Bash と PowerShell の両方で許可する。`git add` / `git commit` は許可しない（通常の対話ではユーザーが実行する）。
  `git push` と `--no-verify` は 2 段で止める。1 段目は `permissions.deny`（`git push *`・`git * --no-verify`・`git commit -n *`）、
  2 段目は PreToolUse フック `scripts/guard-git-hook.py`（Bash・PowerShell の両方。字句に分けて区切りごとにコマンドの位置の語だけを見て
  （先頭の変数代入と sudo・env などの前置きは読み飛ばす）、git の大域オプションを読み飛ばしてサブコマンドを判定し、`git -C path push` や
  `bash -c` の中も見る。引用符の中の語は 1 語として扱い、引数の中の git と bash のヒアドキュメントの本文は見ないため、
  コミットメッセージや grep の検索語の push では止めない）。
  フックは止めるときだけ終了コード 2 と日本語の理由を返し、入力が読めないなどフック自身の異常では終了コード 0 で通す。
  判定の例は `python scripts/guard-git-hook.py --self-test` で確かめる

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
