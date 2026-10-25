<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- date: 2026-10-25 10:12
  summary: カレンダーの日付から買った物をあとから記録する（F-17）（M4）
  details:
    変更内容: >-
      RecordRepository に addRecord（品名・買った日・お店・数量・メモ・写真から記録を作る）を追加した。メモは変えず、
      初めての品名は品目に登録し、その日・お店のお買い物へ付け、品目の購入回数などを計算し直す（R-12）。品名が空なら記録しない。
      カレンダーの日の詳細の下（買い物が無い日も）に「買った物を記録する」を置き、AddRecordSheet（買った日は選んだ日が既定で
      シートの中でも直せる、品名が空のうちは「記録する」を押せない）を開く。記録したら記録した日・月へ移る。
      CalendarViewModel.onEvent が detekt の複雑度の上限に掛かったため、記録のシートの操作を onRecordEvent へ分けた。
      RecordRepository に時計を渡すようにした（記録した時刻と品目の作成時刻に使う）。DESIGN の進捗の古い記述（F-04・F-16 が未実装）を直した
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/AddRecordSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/RecordRepository.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordRepositoryTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 1 回目は detekt の CyclomaticComplexMethod（onEvent の複雑度 15）で失敗し、onRecordEvent へ分けて再実行して
      BUILD SUCCESSFUL（約 3 分 40 秒。単体テスト 183 件。追加は任意の日付に記録して購入回数が更新されメモは変わらない・
      初めての品名を品目に登録する・空の品名は記録しない・記録したらシートを閉じてその日へ移る 4 件）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-043
- date: 2026-10-25 10:03
  summary: 使われなくなった写真の掃除を 1 日 1 回動かす（R-14）（M4）
  details:
    変更内容: >-
      メモ・お買い物・記録のどれかが参照している写真の相対パスを返す PhotoDao を追加し、参照されていない写真を消す PhotoCleaner と、
      それを WorkManager で 1 日 1 回動かす PhotoCleanupWorker を追加した。予約は起動時に一意の定期作業として行い、既にあれば残す。
      端末を使っていない間・電池残量が少なくない時に限って動かす（「やめた」の元に戻すを待っている写真を消さないため）。
      編集シートで取り込んだばかりでまだ保存していない写真を消さないよう、書いてから 1 日未満の写真は残す
      （PhotoStore.deleteUnreferenced に書いた時刻の条件を追加。既定値は従来どおり条件なし）
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoCleaner.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoCleanupWorker.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/IkumemoDatabase.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/PhotoCleanerTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（約 3 分 30 秒。単体テスト 179 件。追加は参照されていない写真だけ消える・メモと記録で共有する写真を残す・
      取り込んだばかりの写真を残す 3 件）、Lint 成功、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-041
- date: 2026-10-25 09:46
  summary: 編集シートの各入力欄にマイクを追加（品名欄は分割しない）（M2）
  details:
    変更内容: >-
      FieldDictationViewModel（音声認識を 1 つ持ち、一度に 1 欄だけ聞く）と、入力欄の横に置く FieldMic を追加した。聞き取った文字は
      いちばん確からしい候補を分割せずにそのまま入れる（品名欄も「と」で分けない、R-32）。1 行の欄（品名・数量）は置き換え、
      複数行のメモは空でなければ改行して足す。聞いている間はマイクが止めるボタンに変わり、押すと何も入れずにやめる。
      マイクの許可が無ければその場で求め、断られたら文字での入力を案内する（R-44）。聞き取れなかった時は聞いていた欄から一度だけ知らせる。
      EditField（メモの編集シートとカレンダーの記録の編集シートで共通）にマイクを付け、ViewModel はアプリ全体で 1 つ持って
      LocalFieldDictation で渡す（渡さない画面ではマイクを出さない）。音声認識が無い端末ではマイクを出さない
    変更ファイル:
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/EditItemSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/FieldDictation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/voice/FieldDictationViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/FieldMicTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（約 2 分 40 秒。単体テスト 176 件。追加は品名を分けない・一度に 1 欄・聞き取れない時とやめた時は入れない・置き換えと追記 4 件、編集シートの 3 欄に声で入れて保存される・音声認識が無い端末ではマイクを出さない 2 件）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-022
- date: 2026-10-25 09:37
  summary: 品質ゲートを速くした（release 側の重複テストをやめる・遅いテストを分ける・ビルドキャッシュ）
  details:
    変更内容: >-
      単体テストのゲートを testDebugUnitTest test から testDebugUnitTest :core:test に変え、debug と同じ内容の mobile の release 側の
      テスト（とそのコンパイル）をやめた。時間のかかるテストに付ける JUnit のカテゴリ SlowTest を作り、2 万件の検索性能テストに付けて、
      -PslowTests の時だけ流すようにした（性能に関わる変更の時とマイルストーンの区切りに流す）。gradle.properties で Gradle のビルド
      キャッシュを有効にした。並列実行と常駐プロセスの維持はメモリを多く使うため入れない（オーナー判断）
    変更ファイル:
      - CLAUDE.md
      - CHANGELOG.md
      - README.md
      - docs/records/managed/DESIGN.md
      - gradle.properties
      - mobile/build.gradle.kts
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordSearchPerformanceTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/SlowTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      ./gradlew testDebugUnitTest -PslowTests --tests "*RecordSearchPerformanceTest"、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（新しい定義で約 2 分 30 秒。単体テスト 170 件〔mobile の debug 側 122 件・core 48 件、遅いテストは除外〕。
      遅いテストは -PslowTests で 1 件流れて成功。BL-083 の追加はバックアップの規則 2 件）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-094
- date: 2026-10-25 09:37
  summary: 自動バックアップの対象を設定（写真はクラウドから外し、端末間転送では移す）（M8）
  details:
    変更内容: >-
      マニフェストに dataExtractionRules（Android 12 以降）と fullBackupContent（Android 11 以前）を指定した。クラウドへのバックアップは
      1 アプリ 25MB を超えると全体が止まるため写真のフォルダ（filesDir/photos/）だけを外し、DB・設定（DataStore）・背景画像は残す。
      端末間転送は容量の制限が無いため全部を移す。Android 11 以前はクラウドと端末間転送を分けられないため写真を外すだけにした
      （写真の引き継ぎは書き出し F-72 で補う）。カメラの一時ファイルはキャッシュにあり、もともと対象外。
      写真の保存先の名前を変えたときに気づけるよう、規則の XML を読んで PhotoStore.PHOTOS_DIR と突き合わせるテストを足した
    変更ファイル:
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/xml/backup_rules.xml
      - mobile/src/main/res/xml/data_extraction_rules.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/BackupRulesTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      ./gradlew testDebugUnitTest -PslowTests --tests "*RecordSearchPerformanceTest"、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（新しい定義で約 2 分 30 秒。単体テスト 170 件〔mobile の debug 側 122 件・core 48 件、遅いテストは除外〕。
      遅いテストは -PslowTests で 1 件流れて成功。BL-083 の追加はバックアップの規則 2 件）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-083
- date: 2026-10-25 09:30
  summary: せっていに このアプリについて（ライセンス一覧つき）と ご意見・ご要望（よくある質問 → GitHub）を追加（M8）
  details:
    変更内容: >-
      このアプリについては、版・名前の由来（PLAN §1.2）と、ライセンス一覧（AboutLibraries 11.2.3 の Gradle プラグインがビルド時に一覧を作り、
      aboutlibraries-compose-m3 の LibrariesContainer で出す。Play Services に依存しない）・プライバシーポリシー・GitHub・ストアのページ
      （評価のお願い。debug 版でもストアに出す版の ID を開く）を並べた。ご意見・ご要望は、よくある質問 4 つ（押すと答えが開く）の後に、
      アプリの版・Android の版・機種名（個人を特定する情報は含めない）とコピーのボタン、「GitHub で送る」（Issue のテンプレート選択を開く）を置いた。
      ご意見フォームはオーナー判断（今は GitHub だけにする）により置かない。外部のページは ExternalLink に集め、開けるアプリが無ければ知らせる。
      せっていの画面遷移を settings/SettingsNavigation.kt へ移し、各項目が行き先のルートを持つようにした
    変更ファイル:
      - build.gradle.kts
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AboutScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ExternalLink.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/FeedbackScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/LicensesScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/ExternalLinkTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AboutContentTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト計 272 件。追加は外部のページの Intent・ストアは出す版の ID・ご意見に添える情報 3 件、
      せっていの項目、このアプリについての各行、ライセンス一覧の表示、よくある質問と GitHub のボタン 4 件）、Lint No issues found、
      markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-093
- date: 2026-10-25 09:03
  summary: せっていに お店の管理（使わない・まとめる）と よく買う物の管理（候補に出す／出さない）を追加（M6）
  details:
    変更内容: >-
      お店の管理は使っているお店を最近使った順に並べ、「使わない」（確認を 1 回出し、候補から消して記録のお店名は残す、R-16）と
      「ほかのお店とまとめる」（まとめ先を選び、記録・メモ・お買い物を移す）を StoreRepository の archive / merge で行う。
      よく買う物の管理は全部の品目を買った回数 → 最近買った日 → 品名の順に並べ、「候補に出す」のスイッチで ProductEntity.hidden を
      切り替える（F-23 の非表示。表記ゆれの統合は S のため含めない）。確認の状態は rememberSaveable で持ち、画面を回しても残す
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ProductRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ManageViewModels.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ProductManageScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/StoreManageScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/ManageViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/ManageContentTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト計 262 件。追加は使わない印と記録のお店名、まとめると記録が移ること、品目の並びと非表示で候補から消えること 3 件、
      確認を経て操作が届くこと・スイッチの切り替え 2 件）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-092
- date: 2026-10-25 08:54
  summary: せっていに そうさ（○ の表示・振動）と おしらせ（時刻・当日のおしらせ）の画面を追加（M6）
  details:
    変更内容: >-
      せっていの項目に「そうさ」「おしらせ」を足した。そうさは ○ の表示（F-13）と振動（R-05）のオン/オフで、OperationPrefs（DataStore）に
      保存し、ShoppingListViewModel の状態から LocalOperationSettings でメモの行へ渡す（○ を消した行は左に余白、振動オフなら
      performHapticFeedback を呼ばない）。おしらせは通知の時刻（TimePicker のダイアログ、24 時間表示）と期限当日のおしらせのオン/オフで、
      保存のたびに ReminderScheduler.reschedule で次の通知を予約し直す（続けて変えても古い値で上書きしないよう Mutex で 1 つずつ保存）。
      切り替えの行は SettingsSwitchRow として共通にした。元の BL-082 は大きいため BL-082・BL-092・BL-093 に分けた
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/OperationPrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/OperationScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/OperationViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ReminderSettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ReminderSettingsViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/OperationViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/OperationContentTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト計 254 件。追加は設定の保存と作り直し後の復元・続けて変えたときの保存と予約し直し 2 件、
      切り替えが届くこと・○ を消すと行から消えること 2 件）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-082
- date: 2026-10-25 08:42
  summary: メモ一覧ウィジェット（W-02、4×2〜4×4、表示専用）と、データ変更・日付の変わり目での描き直しを追加（M7）
  details:
    変更内容: >-
      Glance の MemoListWidget を追加した（SizeMode.Responsive で幅 180/250dp × 高さ 110/180dp の 4 つ）。高さ 2 マスでは見出し
      （いくメモ・🛒メモ N件・🔔期限が近い N件〔0 件なら出さない〕）と先頭の品名を「・」付きで横に 2〜3 件と「ほかN件」、3 マス以上では
      選んだお店と品名の縦一覧（数量付き、幅が狭いと数量を品名の下）を出す。空なら「メモは空です」。行を押すとメモ画面でその項目まで
      送って一瞬光らせ（R-40、項目が無ければメモ画面を開くだけ）、期限が近い件数はおしらせ画面、ほかはメモ画面を開く。
      MainActivity の開く先を ScreenRequest（画面と項目 ID）で受け取り、開く先ごとに data の URI を変えて PendingIntent の上書きを防ぐ。
      WidgetUpdater がアプリの動いている間はメモ・おしらせ・お店・見た目の変化で 2 種のウィジェットを描き直し（R-41・R-43）、
      日付の変わり目と選んだお店の解除時刻（StoreSelection.expiresAt）には省電力のアラームで描き直す。ウィジェットを置いていなければ
      何もしない。受信処理の共通部品 runAsync を ReceiverAsync.kt へ移した
    変更ファイル:
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/ReceiverAsync.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/ScreenRequest.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppSettings.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderReceivers.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoWidgetState.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetUpdater.kt
      - mobile/src/main/res/drawable/widget_memo_preview_background.xml
      - mobile/src/main/res/layout/widget_memo_preview.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/xml/widget_memo_info.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/ScreenRequestTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoWidgetStateTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト計 248 件。追加はウィジェットの表示 5 件〔4×2 の横並びと「ほか N件」、幅が狭いときの件数、
      4×4 のお店と縦一覧、行・期限が近い件数・全体のタップ先、空の表示〕、内容の集計 2 件、開く先の受け渡し 3 件、項目を光らせる 1 件）、
      Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-071
- date: 2026-10-25 07:45
  summary: 1×1 ウィジェット「話して追加」（W-01）と、ホーム画面に重ねて開く音声入力シートを追加（M7）
  details:
    変更内容: >-
      Glance（glance-appwidget 1.2.0）で VoiceWidget を追加した。1×1 では 🎤 と「話して」、横 2 マス以上では「🎤 話して追加」を横に並べる
      （SizeMode.Responsive）。押すと透明なテーマの VoiceSheetActivity（exported=false、履歴に残さない）が開き、BL-021 の音声入力シートを
      そのまま使う。確認カードから追加したら「Nつ メモに入れました」のトーストを出して閉じ、途中で閉じたら何も追加しない（R-42）。
      音声入力が使えない端末では「書いて追加」としてアプリの追加シートを開く（R-44・R-45）。色はきせかえのテーマと暗い画面の設定から
      昼と夜の ColorProvider を作る（WidgetColors）。MainActivity に EXTRA_OPEN_TAB と intent(context, screen) を加え、ウィジェットから
      追加シートや音声入力を開けるようにした。テーマの適用は AppThemeHost に切り出して両方の Activity で使う。
      テストのため AppContainer に DataStore と音声認識の差し替え口を設け、TestIkumemoApplication で差し替える。負荷が高いときに
      Turbine の既定の待ち時間（3 秒）を超える間欠的な失敗があったため、ViewModel テストの待ち時間を 15 秒にそろえた
    変更ファイル:
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceSheetActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/VoiceWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetColors.kt
      - mobile/src/main/res/drawable/ic_widget_keyboard.xml
      - mobile/src/main/res/drawable/ic_widget_mic.xml
      - mobile/src/main/res/drawable/widget_preview_background.xml
      - mobile/src/main/res/layout/widget_voice_preview.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/values/themes.xml
      - mobile/src/main/res/values-night/themes.xml
      - mobile/src/main/res/xml/widget_voice_info.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/AddSuggestionsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestIkumemoApplication.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceSheetActivityTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト計 226 件。追加は画面テスト 4 件で、ウィジェットのタップ先〔音声が使えるとき・使えないとき〕、
      確認カードからの追加とトースト、途中で閉じると何も追加されないこと）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-070
- date: 2026-10-25 05:16
  summary: きせかえ画面（テーマのプレビュー付きカード・暗い画面・文字の大きさ）とせっていの入口を追加（M6）
  details:
    変更内容: >-
      AppearancePrefs（DataStore）・AppearanceViewModel・AppearanceScreen を追加した。テーマは各テーマの色で描いた小さな画面の見本の
      カードから選び（F-60）、暗い画面は「端末に合わせる / 明るい / 暗い」（F-63）、文字の大きさは「ふつう / 大きい / とても大きい」（F-65、
      見本の文字もその大きさで出す）。選んだらすぐ保存し、MainActivity が保存値を読んでアプリ全体の IkumemoTheme に反映する
      （読み終えるまで描かず、起動直後に既定のテーマが一瞬見えるのを防ぐ）。保存値は storageKey で持ち、未知の値は既定値へ戻す。
      メモ画面の右上に「⚙ せってい」（文字付き）を置き、せってい画面（今は「きせかえ」だけ。ほかの項目は BL-082）と戻るボタン付きの枠を作った
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppearancePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/DarkMode.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AppearanceContentTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（ViewModel テスト 4 件。既定値、選んだ値の保存と作り直し後の復元、未知の保存値の既定値への戻し、暗い画面の判定。
      画面テスト 1 件でカードとチップの選択が届く）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-060
- date: 2026-10-25 05:08
  summary: 期限の通知の予約・表示・ボタン操作と、再起動・時刻変更での予約し直しを追加（M5）
  details:
    変更内容: >-
      ReminderScheduler を追加し、core/reminder で求めた「次の通知時刻」を AlarmManager.setAndAllowWhileIdle で 1 件だけ予約する
      （正確なアラームは使わず SCHEDULE_EXACT_ALARM を宣言しない）。発火したらその日の分を 1 通にまとめて出し、最後に通知した日を
      DataStore に残す。アプリの起動時と、再起動・時刻変更・タイムゾーン変更・アプリ更新の受信（ReminderBootReceiver、exported=true）で、
      その日の分をまだ出していなければすぐ出して予約し直す（R-24・R-28）。期限を変えるたびに ReminderRepository から予約し直す。
      通知チャンネルは「期限のおしらせ」1 種（R-25）。1 品目のときだけ「使い切った」「また買う」「あとで」のボタンを出し（R-27）、
      また買うはメモへ入れて使い切ったにし（R-26）、あとでは翌日にもう一度（R-23）。通知を押すとおしらせのタブを開く。
      PendingIntent はすべて FLAG_IMMUTABLE。POST_NOTIFICATIONS は Android 13 以降で初めて期限を決めた時に理由を添えて求め、
      通知オフの案内からは 2 回まで求め、その後は端末の設定を開く（R-29）。おしらせの設定（通知時刻・当日のオン/オフ）と記録は
      ReminderPrefs に分け、設定ファイル（DataStore）は 1 つを共有する。単体テストで本物の Application の起動時処理が裏で走って
      テストを乱したため、robolectric.properties で起動時の処理を行わないテスト用 Application を指定した
    変更ファイル:
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/drawable/ic_notification.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppSettings.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ReminderPrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScheduler.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderNotifier.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderReceivers.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderActions.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/NotificationPermission.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderViewModel.kt
      - mobile/src/test/resources/robolectric.properties
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestIkumemoApplication.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderSchedulerTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（Robolectric テスト 10 件。期限の前日 9:00 の予約、時刻の設定と予約の取り消し、同じ日の 1 通へのまとめと
      ボタン無し、1 品目のボタン 3 つ、電源オフ後に 1 回だけ出す、時刻前は出さない、許可が無ければ出さない、また買う、あとでと使い切った、
      マニフェストに正確なアラームの権限が無く再起動等の受信がある）、Lint No issues found、markdownlint 0 issues、記録ファイル OK。
      実機での確認は BL-053（人手検証）
    関連ID:
      - BL-052
- date: 2026-10-25 04:54
  summary: おしらせ画面（最近買った物の期限設定・期限が近い順の一覧・使い切った／捨てた・通知オフの案内）を追加（M5）
  details:
    変更内容: >-
      ReminderRepository・ReminderViewModel・ReminderScreen・DueSheet を追加した。上部に「最近買った物」（きょうを含む 7 日以内・期限未設定）を
      並べ、各行の「期限を決める」から「3日後 / 1週間後 / 2週間後 / 1か月後 / 日付を選ぶ」で期限を決める（F-50）。下に期限が近い順の一覧を出し、
      期限切れは目立つ色と「期限切れ」の文字、きょう・あしたは文字で示す（F-53・R-22）。各行の「使い切った」「捨てた」で片付け、行を押すと期限を
      決め直せる（決め直すと「あとで」は取り消す）。通知が許可されていなければ「通知がオフのため、おしらせが届きません」と「通知をオンにする」
      （端末の通知の設定を開く）を出す（R-29、画面に戻るたびに確かめる）。書き換え後に呼ぶ onChanged を用意した（通知の予約の再登録は BL-052）。
      Lint の AutoboxingStateCreation のヒントに合わせ、記録の編集の日付を mutableLongStateOf にした
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ReminderRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/DueSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（ViewModel テスト 5 件。最近買った物は 7 日以内だけ、期限の設定で一覧へ移り期限順に並ぶ、使い切った・捨てたが
      DB に残り一覧から消える、1 か月後の計算、通知オフの表示）、Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-051
- date: 2026-10-25 04:46
  summary: core/reminder に通知日の計算と同じ日のまとめを追加（M5）
  details:
    変更内容: >-
      ReminderSchedule を追加した。1 品目につき期限の前日と期限当日（設定でオフ可）の最大 2 回、「あとで」を押したら翌日にもう一度
      （期限は変えない）知らせる（R-20・R-23）。同じ日の分は期限の近い順にまとめて 1 通にし、見出し用に先頭の品名と残りの件数、
      急ぐ順（期限切れ → きょう → あした）の種類を返す。対象が 1 品目のときだけボタンを出す（R-21・R-27）。次の通知日時は 1 件だけ返し、
      きょうの分をまだ出していなければ通知時刻を過ぎていてもすぐ出す（R-28）。過ぎた日の分は出さない（R-22）。
      一覧の並び・期限切れ・期限が近い（前日・当日・期限切れ）の判定は ReminderList に置いた
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/reminder/ReminderSchedule.kt
      - core/src/test/kotlin/com/ikumemo/core/reminder/ReminderScheduleTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト 14 件。前日と当日、当日オフ、月末・年末・うるう年の前日、同じ日のまとめ、1 品目のボタン、
      種類の混在、あとで、通知時刻前・電源オフ後の即時・当日分の通知後、時刻の設定、期限切れ、期限が近い判定）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-050
- date: 2026-10-25 04:42
  summary: カレンダー（月表示・お店色のドット・日の詳細・品名検索・記録の編集シート）を追加（M4）
  details:
    変更内容: >-
      CalendarViewModel と CalendarScreen を追加した。日曜始まりの月表示で、日曜は赤・土曜は青の文字色と曜日名、
      買い物のあった日にお店ごとの色のドット（最大 3 つ）を出す（F-40）。日付を押すとその日の記録（品名・数量・お店・メモ・写真の
      サムネイル。写真は押すと大きく見られる）を下に出す（F-41）。右上の「さがす」で品名検索（品目の正規化名か表示名を含む物を新しい順に
      最大 200 件）し、結果から該当日へ移る（F-42）。日の詳細の記録を押すと記録の編集シート（買った日・お店・数量・メモ・写真を直す、
      記録を消す、F-16）を開く。曜日の色とお店のドットは CalendarColors に置き、全テーマの surface・background で文字 4.5:1・
      図形 3:1 以上を単体テストで確かめる。編集シートの部品（お店の選択・写真・入力欄）を記録の編集でも使えるようにした。
      detekt の上限は設定を変えずに部品を分けて収める方針をオーナーに確認した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/RecordRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/StoreRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/CalendarColors.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/EditItemSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordSearchPerformanceTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/CalendarColorsTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（ViewModel テスト 4 件。日付選択で記録とお店名が出る、検索結果から別の月の該当日へ移る、月の切り替え、
      記録の修正と削除。DB テストで記録 2 万件の品名検索と月の記録の取得がそれぞれ 1 秒未満。色のコントラスト 2 件）、
      Lint No issues found、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-042
- date: 2026-10-25 03:38
  summary: 写真の添付（撮る・選ぶ・縮小と Exif の除去・記録への引き継ぎ）と編集シートの買うお店を追加（M4）
  details:
    変更内容: >-
      PhotoStore を追加した。Photo Picker（権限不要）と標準カメラアプリ（TakePicture＋FileProvider、CAMERA 権限は宣言しない）から
      受け取った写真を filesDir/photos/ へ長辺 1600px に縮小して保存し、Exif の向きを画素へ反映したうえで Exif の無い JPEG にする
      （撮影位置を残さない、F-07・PLAN §4.3）。元の URI は保持せず、カメラの一時ファイル（cacheDir/camera/）は取り込み後に消す。
      保存は一時ファイルに書いてから置き換える。DB には photos/ からの相対パスを持ち、「買った」では同じファイルを記録から参照する（R-15）。
      編集シートを EditItemSheet.kt に分け、買うお店（上位 5 件となし）と写真（サムネイル・撮る・選ぶ・外す）を加え、メモの行に写真ありの印を出す。
      表示は Coil 2.7.0、Exif の読み取りは Lint の指摘（古い Android の既知の不具合）に従い androidx.exifinterface 1.3.7 を使う。
      ShoppingRepository の関数数を detekt の上限に収めるため、品目の処理を ProductRepository に分けた。
      テストが間欠的に止まる事象があったため、テスト用 DB の実行スレッドを DB ごとに持たせた（既定の共有プールに前のテストの処理が残るのを避ける）
    変更ファイル:
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/xml/file_paths.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ProductRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/EditItemSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ItemSheets.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModel.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/PhotoStoreTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/PhotoSizingTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/ShoppingRepositoryTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（写真のテスト 7 件。3200×2400 が 1600×1200 になり GPS の Exif が消える、小さい写真は拡大しない、
      向きの反映、読めないファイルの無視、参照されない写真の削除、縮小の計算、記録への同じファイルの引き継ぎ）、Lint No issues found、
      markdownlint 0 issues、記録ファイル OK。マニフェストに CAMERA・保存領域の権限は無い
    関連ID:
      - BL-040
- date: 2026-10-25 02:54
  summary: 追加シートに入力中の候補（表記ゆれを吸収）と「いつもの」チップを追加（M3）
  details:
    変更内容: >-
      追加シートの入力を ViewModel で持ち、core/suggest の Suggestions で表記ゆれを吸収した入力中の候補（F-20）と、よく買う物の
      「いつもの」チップ（F-21）を出すようにした。メモにある物は出さず、チップは押すたびに 1 件ずつ入る。品質ゲートの detekt で
      ShoppingRepository の関数数が上限に達したため、どこからも呼ばれていない getItem を削除した（修正コミット 30f1ca9）。
      本記録は BL-032 のコミット時に誤って BL-031 の記録を重ねて追記していたものを、コミットの内容から書き直した（2026-10-25）
    変更ファイル:
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ItemSheets.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/AddSuggestionsTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 修正コミット 30f1ca9 の後に BUILD SUCCESSFUL（追加のテスト 3 件。表記ゆれでも同じ候補が出る、いつものを続けて入れられる、
      文字で追加すると入力が空になる）。BL-032 のコミット 8a0b60b の時点では detekt の関数数超過で失敗していた
    関連ID:
      - BL-032
- date: 2026-10-25 02:50
  summary: いつものお店（お店チップ・選ぶシート・登録・自動解除・使わない印と統合）を追加（M3）
  details:
    変更内容: >-
      メモの上部にお店チップ（買うお店を選ぶ／○○ で買う）を置き、いつものお店から選ぶシート（最近使った順の上位 5 件、ほかのお店、
      新しいお店の登録、選ばない）を追加した（F-14・F-30・F-31）。選んだお店は「買った」の記録とお買い物に付き、お店の最近使った日時を更新する。
      選んだお店は DataStore（AppSettings）に保存し、選んだ時か最後の「買った」から 3 時間たつか日付が変わったら解除する（R-06）。
      表示も 1 分ごとに見直す。お店の登録は同じ名前（表記ゆれを含む）があればそれを使い、使わない印があれば外す。
      お店の管理用に、行を残して使わない印を付ける archive と、記録・メモ・お買い物を移して 1 つにまとめる merge を用意した（R-16。画面は BL-082）。
      DataStore Preferences 1.1.7 を導入した。detekt の分岐数の上限に合わせ、ViewModel のイベント処理を画面だけの操作と書き換えの操作に分けた
    変更ファイル:
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppSettings.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/StoreRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/StorePicker.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/StoreRepositoryTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（お店のテスト 8 件。選んだお店が「買った」に記録される、3 時間で解除、「買った」で 3 時間を数え直す、
      日付が変わると解除、同じ名前の再利用、最近使った順、使わない印でも過去の記録のお店名が残る、統合でお買い物がまとまる）、
      markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-031
- date: 2026-10-25 02:32
  summary: core/suggest に「いつもの」の並び順と入力中の候補の照合を追加（M3）
  details:
    変更内容: >-
      Suggestions を追加した。いつものは 1 回以上買った物を購入回数の多い順 → 最近買った順 → 正規化名の順に並べ、
      入力中の候補は正規化名（R-02）が入力で始まる物を先に、含む物を後に並べる（F-20・F-21）。どちらも候補に出さない印の物と
      メモにある物を除く。DB の品目に依存しないよう ProductSummary を core に置いた
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/suggest/Suggestions.kt
      - core/src/test/kotlin/com/ikumemo/core/suggest/SuggestionsTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（単体テスト 6 件。並び順、候補に出さない物とメモにある物の除外、表記ゆれの吸収、前方一致の優先）、
      markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-030
- date: 2026-10-25 02:29
  summary: 音声入力部品（話して追加・確認カード・マイクの許可・聞き取れないときの案内）を追加（M2）
  details:
    変更内容: >-
      SpeechEngine（Android 標準の SpeechRecognizer を包む。日本語固定・オンデバイス優先・途中経過あり・候補 5 件）と
      VoiceInputViewModel・VoiceInputSheet を追加し、ホームに 🎤 話して追加（64dp）を置いた。認識結果は core/voice で分け、
      確認カードで直す・外すをしてから「追加する」で入れる。メモにある物は「メモにあります」の印を付けて既定で外す（R-01）。
      何も話さなければ 5 秒で止めて「うまく聞き取れませんでした」と「もう一度話す」「文字で入れる」を出す（R-31）。
      マイクの許可は初めて使うときに理由（録音は保存しない）を添えて求め、拒否されたら端末の設定を開くボタンと文字入力を案内する。
      音声認識の機能が無い端末ではマイクを出さず文字入力を案内する。AndroidManifest に RECORD_AUDIO と
      RecognitionService の queries を宣言した。確認画面のボタンは文字が大きい設定でもはみ出さないよう「追加する」を全幅にした。
      5 秒のタイマーを認識開始の後に仕掛けていたため、結果がすぐ返ると「聞き取れませんでした」で上書きされる誤りを見つけ、開始前に仕掛ける順へ直した。
      画面テストは、Robolectric の waitUntil が待つ間にメインスレッドの処理を進めないため waitForIdle で進めながら待つ awaitCondition を
      testDebug に共通化し、ボトムシート（別ウィンドウ）の中のボタンは座標のタップが届かないため意味操作でクリック処理を呼ぶ形にした
    変更ファイル:
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/SpeechEngine.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/FakeSpeechEngine.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/testing/ComposeWait.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、画面テストの 3 回連続実行（--rerun-tasks）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（ViewModel テスト 8 件・画面テスト 2 件。「牛乳2本とたまご」から 2 枚の確認カードが並び追加できる、
      メモにある物の除外、カードの修正と除外、5 秒の無音、認識エラー、機能の無い端末、途中で閉じたときに何も追加しない）、
      画面テストは 3 回とも成功、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-021
- date: 2026-10-25 01:49
  summary: core/voice に音声テキストの分割・数量抽出・不要語除去・誤変換補正を追加（M2）
  details:
    変更内容: >-
      VoiceTextParser を追加した。「と」「、」「。」「それから」「あと」「それと」と空白で分け、品名の中の「と」（いとこんにゃく・とうふ・
      なっとう等の既定の辞書の読みと過去に入れた品名、ひとつ等の読みの数量）は最長一致で守る。「を買わなきゃ」「が欲しい」
      「買っといて」などの不要な言葉は取り除き、区切りとしても扱う（「買っといて」の「と」で分けないため）。
      QuantityParser は品名の後ろの「算用数字・漢数字＋助数詞」「いっこ・ひとつ・にほん」などの読みを取り出し、
      数量を算用数字＋助数詞へそろえる（いっこ・一個・1個はすべて 1個）。「牛乳日本」「牛乳二本」は 牛乳 2本 に補正する。
      ひらがなの助数詞は数字が付くときだけ数量として扱い、「いちご」「ねこ」を誤って分けない。
      認識候補（N-best）から過去の品名と一致する品物が最も多い候補を選ぶ parseBest も用意した
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/voice/VoiceTextParser.kt
      - core/src/main/kotlin/com/ikumemo/core/voice/QuantityParser.kt
      - core/src/test/kotlin/com/ikumemo/core/voice/VoiceTextParserTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（音声分割の単体テスト 14 件。区切り語・品名内の「と」・漢数字と助数詞・不要語・「牛乳日本」の補正・
      N-best の選択を含む）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-020
- date: 2026-10-25 01:43
  summary: 記録の修正・削除と品目の集計の再計算を追加（M1）
  details:
    変更内容: >-
      RecordRepository を追加した。記録の購入日・お店・数量・メモ・写真を修正でき、購入日かお店が変わったときはその日・お店の
      お買い物へ付け替える。記録を消すと紐づくおしらせも消え（外部キーの CASCADE）、使われなくなったお買い物を消し、
      品目の購入回数・最後に買った日・平均間隔を計算し直す（F-16・R-12）。修正・削除の画面（記録の編集シート）は日の詳細と一緒に作るため
      BL-042 へ含めた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/RecordRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordRepositoryTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（DB テスト 3 件。修正での平均間隔の再計算、削除でのおしらせとお買い物の削除と集計の再計算、
      お店の変更でのお買い物の付け替え）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-014
- date: 2026-10-25 01:39
  summary: スワイプで買った／やめた、○ で買った、元に戻すを追加（M1 完了）
  details:
    変更内容: >-
      メモの行を SwipeToDismissBox にし、右へで「買った」、左へで「やめた」とした（D-4・F-10・F-11）。確定は幅の 40%（R-05）で、
      しきい値を超えた瞬間に performHapticFeedback で知らせる。行の左の ○ でも「買った」にできる（F-13）。
      「買った」は端末の今日の日付で記録し（R-10）、同じ日・同じお店のお買い物にまとめ、同じ日の 2 回目も別の記録にする（R-11）。
      写真は同じファイルを記録へ引き継ぐ（R-15）。「やめた」は記録せず品目は残す（R-03）。
      操作後に「買った：牛乳 ／ 元に戻す」を 5 秒表示し（TalkBack 等の利用時は推奨時間まで延長）、元に戻せるのは最新の 1 件だけ（F-12・R-04）。
      元に戻すとメモの同じ位置へ戻し、記録と使われなくなったお買い物を消して購入回数等を計算し直す。
      購入回数・最後に買った日・平均間隔の計算は core/suggest の PurchaseStats に置いた（R-12）。
      ViewModel の操作は PLAN §4.1 の UiState / Event に合わせて onEvent に集約した（detekt の TooManyFunctions 対策を兼ねる）。
      Compose の画面テストは ui-test-manifest が debug にだけ入るため src/testDebug に置いた（release の単体テストでは動かないため）。
      追加シートの「追加する」を座標で押すテストはシートの動作中に外れて不安定だったため、キーボードの完了操作で追加する形にした
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/suggest/PurchaseStats.kt
      - core/src/test/kotlin/com/ikumemo/core/suggest/PurchaseStatsTest.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ProductStatsUpdater.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/ShoppingRepositoryTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/testing/TestDatabase.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest test assembleDebug、画面テストの 3 回連続実行（--rerun-tasks）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（画面テスト 3 件で「文字で追加 → 右スワイプで記録 → 元に戻す」「左スワイプでやめた」「○ で買った」を確認、
      3 回とも成功）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-013
- date: 2026-10-25 01:13
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
- date: 2026-10-25 00:55
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
- date: 2026-10-25 00:52
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
- date: 2026-10-24 19:40
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
- date: 2026-10-24 19:10
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
- date: 2026-10-24 18:30
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
