<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- id: BL-151
  区分: 人手検証
  タスク内容: >-
    実機で端末の文字サイズを最大にし、着せ替えの文字の大きさを「とても大きい」にして、全画面で文字の切れ・重なりが無いかを確かめる（UX レビュー C6）。
    特に期限の画面の行のボタン（BL-142 の「期限なし」を含む）と上部のバーの設定・時刻のボタン（BL-143）、記録の画面のマスと上部のバー（BL-137 の「今日」「探す」）、
    期限を決めるシートの 2 行のチップ（BL-132）、データ画面の説明と折りたたみ（BL-134・BL-145）、設定の一覧の説明（BL-139）を見る
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: はみ出し・重なりが無い。あれば画面名・端末・文字の設定を記録している
  依存:
    - BL-132
    - BL-134
    - BL-137
    - BL-139
    - BL-142
    - BL-143
    - BL-145
  根拠: >-
    2026-11-13 の UX レビュー C6。スクリーンショットテストは幅 360dp と文字の 3 段階だけで、端末の文字サイズ最大との組み合わせは実機でしか確かめられないため。
    文字の並びを変えるタスクの後に確かめる
- id: BL-150
  区分: 人手検証
  タスク内容: >-
    実機の台所（換気扇・テレビ・子どもの声がある中）で話して追加を使い、5 秒の無音で止まる時間が短すぎないか、
    確認カードで直す・外す・戻す（BL-136 で変えた見た目）が片手でできるかを確かめる（UX レビュー C5）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 3 品を 1 回の発話で入れられる。止まるのが早すぎた・聞き取れなかった場面と認識された文字を記録している（BL-023 の素材にもなる）
  依存:
    - BL-136
  根拠: 2026-11-13 の UX レビュー C5。周りの音と音声認識の止まり方は実機の環境でしか確かめられないため
- id: BL-149
  区分: 人手検証
  タスク内容: >-
    実機で、片手（右手・左手それぞれ）でメモ画面の 🎤・○・スワイプ・「元に戻す」（画面下中央）・「設定」（右上）に親指が届くか、
    一覧をスクロールしているときに横へずれて「やめた」になる誤操作が起きないかを確かめる（UX レビュー C4）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 主な操作が持ち替えなしで届き、5 分使って誤操作が 0 回。届かない操作・誤操作があれば画面名と手（右・左）とともに記録している
  依存: []
  根拠: 2026-11-13 の UX レビュー C4。指の届く範囲とスクロール中の誤操作は端末の大きさと持ち方に左右され、実機でしか確かめられないため
- id: BL-148
  区分: 人手検証
  タスク内容: >-
    実機で、設定やその下の画面を開いた後に下タブを押すと、そのタブの画面へ戻るか（メモのタブで設定が復元されて出てこないか）を確かめる（UX レビュー C2）。
    BL-143 で設定の入口が 3 つのタブすべてに増えるため、メモ・記録・期限の各タブから設定を開いた場合をそれぞれ確かめる。
    設定を開いている間、下タブが何も選ばれていない見え方で迷わないかも見る
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 各タブから設定を開いた後に下タブを押すと、そのタブの画面（メモなら一覧）が出る。迷った点や設定が出てきた操作があれば画面名と手順を記録している
  依存:
    - BL-143
  根拠: >-
    2026-11-13 の UX レビュー C2。下タブの戻る履歴の保存・復元（IkumemoApp.kt の navigateToTab の saveState・restoreState）の実際の動きは実機の操作で確かめるため。
    不具合と分かったら、手順を添えて不具合のタスクを起票する
- id: BL-147
  区分: UX改善
  タスク内容: >-
    文字で追加でも「牛乳 2本」を品名「牛乳」と数量「2本」に分ける（UX レビュー B7、オーナー承認済み）。
    いまの状態。mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt の handleDataEvent（264〜267 行目）は AddByText の文字をそのまま
    repository.addItem(event.name) に渡し（mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt の addItem、74〜99 行目。数量は品目の既定値だけ）、「牛乳 2本」が品名になる。
    声では core の QuantityParser.split（core/src/main/kotlin/com/ikumemo/core/voice/QuantityParser.kt、NameAndQuantity を返す）で品名の後ろの数量を分けている（F-01）。
    どう変えるか（オーナーが承認した案）。AddByText のときだけ QuantityParser.split を通して品名と数量に分け、addItem(name, quantity) に渡す。
    区切り（「と」「、」）での複数件の分割はしない。「いつもの」チップと入力中の候補（AddProduct）は品名そのままなので通さない。
    分けた品名が既にメモにあるときは今の R-01 のとおり追加せずに光らせる（既存の項目の数量は書き換えない）。数量だけの入力（例「2本」）で品名が空になるときは分けずにそのまま入れる
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest に、「牛乳 2本」で品名 牛乳・数量 2本 の項目ができること、
    「牛乳」がメモにあれば追加されず光ること、数量だけの入力のふるまいのテストを足す。QuantityParser を変える必要があれば core/src/test/kotlin/com/ikumemo/core/voice/VoiceTextParserTest に足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*ShoppingListViewModelTest" --tests "*ShoppingRepositoryTest" --tests "*VoiceTextParserTest" が成功する。
    画面の見た目は変わらないためスクリーンショットの記録し直しは不要。
    使い方 mobile/src/main/assets/help/text.md に「「牛乳 2本」のように入れると、数も分かれて入ります」を足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-03 の行に数量の取り出しを足す
  依存:
    - BL-131
  根拠: >-
    2026-11-13 の UX レビュー B7（効果 中）。オーナーが 2026-11-13 に質問の案のとおりで承認した。
    既存の項目の数量を書き換えないのは既定（R-01 の「同じ物は追加しない」を変えないため）。同じ文字で追加のシートと text.md を触る BL-131 の後に行う
- id: BL-146
  区分: 機能追加
  タスク内容: >-
    メモの項目の編集シートに「メモから外す（やめた）」ボタンを足し、スワイプしなくても「やめた」にできるようにする（UX レビュー B6、オーナー承認済み）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。「買った」は行の ○ でもできるが、「やめた」は左へのスワイプ（と TalkBack の操作の一覧）だけ。kotlin/shoppinglist/EditItemSheet.kt の下（95〜99 行目）は「閉じる」ボタンだけ。
    kotlin/shoppinglist/ShoppingListViewModel.kt では ShoppingListEvent.Dismiss（274 行目）が writeUndoable で「やめた：品名 ／ 元に戻す」を出す。
    編集シートは kotlin/shoppinglist/ShoppingListScreen.kt の 138〜144 行目付近（HomeSheet.Edit）で開いている。
    どう変えるか（オーナーが承認した案）。編集シートの下に「メモから外す（やめた）」の TextButton（48dp 以上、文言は strings.xml）を、「閉じる」と同じ行に並べず離して置く。
    押すとシートを閉じ、スワイプと同じ Dismiss を送って「やめた：品名 ／ 元に戻す」を出す。シートで直した内容は保存しない（元に戻したときはシートを開く前の内容に戻る）。
    EditItemSheet の引数に外す処理を足し、呼び出し側で編集中の状態を閉じてから Dismiss を送る
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest に、編集シートの「メモから外す」を押すとシートが閉じて Dismiss のイベントが出ることのテストを、
    mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest に、編集中の項目を外すと元に戻すの申し出が出てシートが閉じるテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*ShoppingListScreenTest" --tests "*ShoppingListViewModelTest" --tests "*AccessibilityTest" が成功する。
    シートはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/edit.md と swipe.md に「直すシートの「メモから外す」でも「やめた」にできます」を足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-04、F-10・F-11・D-4・R-05、「PLAN §8 アクセシビリティ」の行を合わせる
  依存: []
  根拠: >-
    2026-11-13 の UX レビュー B6（効果 中）。オーナーが 2026-11-13 に質問の案のとおりで承認した。
    直した内容を保存しないのは既定（外す物に保存は要らず、元に戻すの結果が予想どおりになるため。品目と記録は変わらないので安全）
- id: BL-145
  区分: UX改善
  タスク内容: >-
    データの書き出しは「写真も含めて書き出す」1 つを表に出し、写真なし（JSON）と表計算用（CSV）は「ほかの形式で書き出す」の中に入れる（UX レビュー B5、オーナー承認済み）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt の DataContent（BL-134 で見出しを付けた後の「書き出す」の区切り）と、
    mobile/src/main/res/values/strings.xml の 158〜165 行目（data_export_* と data_import_detail）。
    いまの状態。「写真つき（ZIP）」「写真なし（JSON）」「表計算用（CSV）」の 3 行が同じ並びで、説明に ZIP・JSON・CSV の語が出る。
    どう変えるか（オーナーが承認した案）。主の行は「データを書き出す（写真も含む）」1 つ。その下に「ほかの形式で書き出す」（押すと開く折りたたみ。開いた状態は保存しない）を置き、
    中に写真なしと表計算用の 2 行を出す。説明から ZIP・JSON の語を外し、「いくメモで読み込めるファイル」「表計算ソフト（Excel など）で開けるファイル」と書く
    （data_import_detail の「ZIP・JSON」も同じ考えで書き直す）。書き出す中身・ファイル名・ExportKind は変えない
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest に、最初は写真つきの行だけが見え、「ほかの形式で書き出す」を押すと写真なしと表計算用の行が出て、押すと onExport が呼ばれるテストを足す
    （EXPORT_JSON・EXPORT_CSV を押している既存のテストは開いてから押すよう直す）。
    軽い検証 python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*DataViewModelTest" が成功する。
    設定の画面はスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/data-export.md（書き出しの 3 行の説明）と device-change.md の書き出しの記述を新しい行の名前に合わせ、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-72 の行を合わせる
  依存:
    - BL-134
  根拠: >-
    2026-11-13 の UX レビュー B5（効果 中）。オーナーが 2026-11-13 に質問の案のとおりで承認した。
    データの画面を組み替える BL-134 の後に行う（同じ DataContent の並びを触るため）
- id: BL-144
  区分: UX改善
  タスク内容: >-
    カレンダーの記録の編集シートも、メモの編集シートと同じく「閉じたら保存」にする（UX レビュー B4、オーナー承認済み）。
    いまの状態。mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt の ModalBottomSheet（68〜71 行目）は onDismissRequest で CalendarEvent.CloseEditor を送り、
    下へ払う・戻る・外を押すと変更を捨てて閉じる（保存は DeleteAndSave の「直す」ボタン、118〜122 行目、strings.xml の calendar_record_save だけ）。
    メモの編集シート（mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/EditItemSheet.kt 75〜78 行目）は onDismissRequest = save で、閉じれば保存する（確認なし、PLAN §1.3-4）。
    どう変えるか（オーナーの決定）。onDismissRequest も save（CalendarEvent.SaveRecord）にし、「直す」ボタンは common_close「閉じる」に替えて、押すと保存して閉じる（メモの編集と同じ）。
    日付を変えた場合は今の saveRecord（calendar/CalendarViewModel.kt 234〜246 行目）のとおり保存後にその日へ移る。何も変えずに閉じたときは記録の中身が変わらないこと。
    「この記録を消す」を押したときに保存が走らないこと（消した後にシートが消えるとき onDismissRequest が呼ばれないことを確かめる）。testTag の RECORD_SAVE は名前を変えてもよい
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    記録の編集シートを払って閉じると内容が保存される。RecordEditSheet を直接描く Compose のテスト（mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/RecordEditSheetTest を新設）で、
    欄を変えて閉じるボタンを押すと SaveRecord が出ること、消すを押すと DeleteRecord だけが出ることを確かめ、
    mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest に変更なしの保存で記録が変わらないテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*RecordEditSheetTest" --tests "*CalendarViewModelTest" が成功する。
    シートはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/edit-record.md に「シートを閉じると保存されます」（edit.md と同じ書き方）を足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-16 の行に「閉じると保存」を足す
  依存: []
  根拠: >-
    2026-11-13 の UX レビュー B4（効果 大）。オーナーが 2026-11-13 に承認した。メモで覚えた閉じ方をカレンダーでもすると、直した内容が黙って消えるのを防ぐ。
    レビュー A4（BL-133）は保存ボタンを「保存する」にする案だったが、本タスクで「閉じる」になるため BL-133 では触らない
- id: BL-143
  区分: UX改善
  タスク内容: >-
    設定の項目を減らし、3 つのタブのどこからでも設定へ行けるようにする（UX レビュー B3、オーナー承認済み）。パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。設定の入口はメモ画面の右上（kotlin/shoppinglist/ShoppingListScreen.kt、HomeNavigation.onOpenSettings）だけで、カレンダー（kotlin/calendar/CalendarScreen.kt の CalendarTopBar、120〜142 行目）と
    お知らせ（kotlin/reminder/ReminderScreen.kt の ReminderContent の TopAppBar、102 行目）には無く、kotlin/IkumemoApp.kt の NavHost（178〜196 行目）は CalendarScreen・ReminderScreen に設定を開く処理を渡していない。
    設定（kotlin/settings/SettingsScreen.kt の SettingsItem、58〜74 行目）は 10 項目で、「操作」（kotlin/settings/OperationScreen.kt、スイッチ 2 つ）と
    「お知らせ」（kotlin/settings/ReminderSettingsScreen.kt、時刻とスイッチ 1 つ）が別の画面、「ご意見・ご要望」（FEEDBACK）と「このアプリについて」（ABOUT）が別の行。お知らせの時刻はお知らせ画面から辿れない。
    どう変えるか（オーナーが承認した案）。(1) カレンダーとお知らせの上部のバーの右に、メモ画面と同じ「⚙ 設定」（アイコン＋文字）を置き、押すと SettingsRoute を開く。
    (2) お知らせ画面の一覧の上（通知オフの帯の下）に、お知らせの設定へ直接行くボタン「お知らせの時刻 9:00」（時刻は設定値。ReminderUiState に時刻を持たせ、ReminderViewModel で kotlin/data/ReminderPrefs.kt から読む）を置き、
    押すと (3) の画面を開く。(3)「操作」と「お知らせ」を 1 つの画面「操作とお知らせ」にまとめる（○ を出す・振動・お知らせの時刻・期限の当日もお知らせ の 4 行。
    OperationContent と ReminderSettingsContent の中身を並べ、ViewModel は 2 つのまま使ってよい）。
    (4)「ご意見・ご要望」を設定の一覧から外し、「このアプリについて」（kotlin/settings/AboutScreen.kt）の中の行にする（FeedbackRoute と、使い方の「やってみる」の行き先 HelpScreens.FEEDBACK は残す）。
    使い方は一番上に残す（H-01）。結果として設定は 8 項目（使い方・着せ替え・操作とお知らせ・お店の管理・よく買う物の管理・ホーム画面に置く・データ・このアプリについて）
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    3 つのタブから設定が開け、お知らせ画面から時刻の設定へ 1 タップで行け、設定の一覧が 8 項目になっている。テストの testDebug/ は mobile/src/testDebug/kotlin/com/ikumemo/mobile/、test/ は mobile/src/test/kotlin/com/ikumemo/mobile/。
    testDebug/settings/AboutContentTest の settingsListsFeedbackAndAbout（settings_feedback の行を押している）を新しい並びに直し、このアプリについての中からご意見・ご要望が開けるテストを足す。
    testDebug/settings/OperationContentTest をまとめた画面に合わせて直す（時刻と当日のお知らせの行を含める）。
    testDebug/calendar/CalendarContentTest と testDebug/reminder/ReminderContentTest（BL-116 で作る）に設定のボタンと時刻のボタンのイベントのテストを、test/reminder/ReminderViewModelTest に時刻が状態に入るテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*AboutContentTest" --tests "*OperationContentTest" --tests "*CalendarContentTest" --tests "*ReminderContentTest" --tests "*ReminderViewModelTest" が成功する。
    上部のバーと一覧が変わるため、スクリーンショットテストの calendar と reminder を CLAUDE.md の手順で画面ごとに -Proborazzi.test.record=true で記録し直し、
    画像の差分（extra_large で題名とボタンが重ならないこと）を確かめてコミットに含める（memo は変わらないことを確かめる）。
    使い方 mobile/src/main/assets/help/ の 設定の「操作」（bought-button.md・swipe.md）、設定の「お知らせ」（notified.md）、設定の「ご意見・ご要望」（feedback.md・faq.md）を新しい場所に直し、
    python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の「PLAN §3（一部）」（設定の入口と項目の並び）、「F-13・R-05・PLAN §3 操作」、「R-20・R-21・PLAN §3 お知らせ」、F-77、「PLAN §3・§5・§7」（このアプリについて）の行を合わせ、
    docs/design/WIREFRAME.md の設定と上部のバーを直す
  依存:
    - BL-130
    - BL-141
  根拠: >-
    2026-11-13 の UX レビュー B3（効果 大）。オーナーが 2026-11-13 に、質問の案（設定ボタンを 3 つのタブすべてに置き、操作とお知らせを 1 画面にまとめて項目を減らす）のとおりで承認した。
    既定で決めた点。レビューと質問は「10 項目から 7 項目」としていたが、案に書かれた統合（操作＋お知らせ、ご意見・ご要望を このアプリについて の中へ）だけでは 8 項目になる。
    承認された統合を超えて項目を減らさない。まとめた画面の名前は、レビューの「使い方の設定」だと設定の一番上の「使い方」（ヘルプ）と紛らわしいため「操作とお知らせ」とした。
    上部のバーと一覧を触るため BL-141（題名）と BL-130（お知らせの一覧の面）の後に行う
- id: BL-142
  区分: UX改善
  タスク内容: >-
    お知らせ（BL-141 の後は「期限」）の画面で、「期限が近い順」を上に出し、「最近買った物」は数件だけ出して「期限なし」で一覧から外せるようにする（UX レビュー B2、オーナー承認済み）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。kotlin/reminder/ReminderScreen.kt の ReminderList（117〜159 行目）は 通知オフの帯 →「最近買った物」→「期限が近い順」→「お知らせが来ないときは」の順。
    最近買った物は 7 日以内に買って期限を決めていない記録すべて（kotlin/data/ReminderRepository.kt の observeRecentWithoutReminder、kotlin/data/db/Daos.kt 173〜178 行目）で、
    まとめ買いをすると数十行になり、期限切れの物が下へ押し出される。トイレットペーパーなど期限の要らない物も 7 日間並び続け、消す手段が無い。
    どう変えるか（オーナーが承認した案）。(1) 並びを 通知オフの帯 →「期限が近い順」→「最近買った物」→「お知らせが来ないときは」にする（期限切れの有無によらず常にこの順）。
    (2)「最近買った物」は最初の 3 件だけ出し、残りがあれば「ほか N 件を見る」を出す（押すと全部出す。開いた状態は rememberSaveable 程度でよく保存しない）。
    (3) RecentRow（201〜230 行目）に「期限なし」（TextButton、48dp 以上）を足し、押すとその記録を一覧から外し、「期限なしにしました：品名 ／ 元に戻す」を 5 秒出す（メモの UndoEffect と同じ考え方）。
    記録は消さない。外した記録の id は DB を変えずに kotlin/data/ReminderPrefs.kt（DataStore）へ保存し、7 日を過ぎて一覧に出なくなった id は保存のたびに掃除する。
    品目ごとに覚えて次から出さない仕組みは作らない（承認された案に含まれないため）。
    G-08 のガイド（最近買った物の一番上の「期限を決める」を照らす）は外した記録を除いた一覧の 1 行目で動くようにする（ReminderViewModel の guide の条件も外した記録を除いて判定する）
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    testDebug/reminder/ReminderContentTest（BL-116 で作る。mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/）に、期限が近い順が最近買った物より上にあること、
    4 件以上で 3 件と「ほか N 件を見る」が出て押すと全部出ること、「期限なし」でイベントが出ることのテストを、
    mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest に、外した記録が一覧から消えること・元に戻すと戻ること・古い id が掃除されることのテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*ReminderContentTest" --tests "*ReminderViewModelTest" が成功する。
    見た目が変わるため、スクリーンショットテストの reminder を CLAUDE.md の手順で -Proborazzi.test.record=true で記録し直し、画像の差分を確かめてコミットに含める。
    設定の書き出し・読み込み（kotlin/data/backup/BackupSettingsStore.kt）に外した id を含めるかを決めて EXECUTE.md に書く（7 日で消える一時的な値のため含めないのが既定）。
    使い方 mobile/src/main/assets/help/due.md（下の「お知らせ」の「最近買った物」で、の手順）に並びと「期限なし」を足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-50・F-53・G-07・G-08 の行と「設定の保存方針（DataStore）」に外した記録の保存を足す
  依存:
    - BL-116
    - BL-141
  根拠: >-
    2026-11-13 の UX レビュー B2（効果 大）。オーナーが 2026-11-13 に、質問の案（期限が近い順を先、最近買った物は数件だけ、期限なしで外せる）のとおりで承認した。
    既定で決めた点。最初に出す件数はレビューの「3〜5 件」の少ない方の 3 件。並びは期限切れの有無で入れ替えず常に期限が近い順を先（並びが変わると迷うため）。
    外した記録の保存は DB の版を上げずに DataStore に置く（IkumemoDatabase は version 1 で、マイグレーションを避ける）。押し間違いに備えて元に戻すを出す（アプリ全体の「間違えても戻せる」に合わせる）。
    いずれも記録を消さず、期限を決める操作も残るため安全。BL-141 が同じ due.md の手順を書き換えるため、その後に行う
- id: BL-141
  区分: UX改善
  タスク内容: >-
    下タブの「カレンダー」「お知らせ」を中身に合わせて「記録」「期限」に変え、予定・用事を扱うアプリではないことを初めての案内と使い方に短く書く（UX レビュー B1、オーナー承認済み）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/、strings.xml は mobile/src/main/res/values/strings.xml。
    いまの状態。下タブ（kotlin/IkumemoApp.kt の TopLevelTab、72〜95 行目）と各画面の上部のバーの題名は strings.xml の tab_calendar「カレンダー」・tab_reminder「お知らせ」（6・7 行目）。
    「カレンダー」は買った物の記録の振り返り、「お知らせ」は買った物の期限の通知だけで、予定・用事を入れる機能は無い。「お知らせ」は運営からのお知らせの意味でもよく使われる。
    どう変えるか（オーナーの決定）。tab_calendar を「記録」、tab_reminder を「期限」にする（上部のバーの題名も同じ文字列なので一緒に変わる）。アイコンは今のまま。
    アイコン長押しのショートカット（shortcut_calendar・shortcut_calendar_long・shortcut_reminder・shortcut_reminder_long、193〜196 行目、kotlin/AppShortcuts.kt）、
    設定の「ホーム画面に置く」の説明（home_screen_shortcuts、188 行目）、通知の本文 notification_due_body「押すと、お知らせの一覧を開きます」（112 行目）もタブの新しい名前に合わせる。
    初めての案内の 3 枚目（onboarding_look_back_title・onboarding_look_back_body、151〜152 行目）と使い方の冒頭（mobile/src/main/assets/help/start.md）に
    「予定表やなんでもリマインダーではなく、買い物のためのメモです」を短く添える。
    通知そのものを指す「お知らせ」（通知のチャンネル名、お知らせの時刻、「お知らせが来ないときは」）は変えない。使い方の目次の章「お知らせ」（index.md）は期限と通知の両方の章なので「期限とお知らせ」にする。
    予定・用事のメモは今後も扱わない（オーナーの決定）。テストの目印（TopLevelTabTags）と route の名前は変えない
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    下タブ・上部のバー・ショートカット・初めての案内・使い方が新しい名前になっている。テストの test/ は mobile/src/test/kotlin/com/ikumemo/mobile/、testDebug/ は mobile/src/testDebug/kotlin/com/ikumemo/mobile/。
    名前を文字で確かめている既存のテスト test/AppShortcutsTest（22 行目）、testDebug/help/HelpScreenTest（40 行目の title）、test/help/HelpContentTest（35 行目の目次の章）を直し、
    testDebug/guide/OnboardingTest に 3 枚目の文言のテストがあれば合わせる。
    軽い検証 python scripts/quality-gates.py --quick --tests "*AppShortcutsTest" --tests "*HelpContentTest" --tests "*HelpScreenTest" --tests "*OnboardingTest" が成功する。
    上部のバーの題名が変わるため、スクリーンショットテストの calendar と reminder を CLAUDE.md の手順で画面ごとに -Proborazzi.test.record=true で記録し直し、
    画像の差分を確かめてコミットに含める（memo は変わらないことを確かめる）。
    使い方 mobile/src/main/assets/help/ を「カレンダー」「お知らせ」で Grep し（2026-11-13 時点で start.md・calendar.md・due.md・shortcuts.md・add-later.md・edit-record.md・search.md・index.md・notified.md）、
    タブと画面を指す箇所を新しい名前にして python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の「PLAN §3」の下タブの行、F-74（ショートカット）、F-70・G-01（初めての案内）の行と「UI/UX 方針」の表記の例を合わせ、docs/design/WIREFRAME.md のタブの名前も直す
  依存: []
  根拠: >-
    2026-11-13 の UX レビュー B1（効果 大）。オーナーが 2026-11-13 に、下タブの名前を「記録」「期限」に変え、予定・用事のメモは今後も扱わない前提で進めると決めた。
    通知を指す「お知らせ」まで変えると通知の設定・よくある質問の意味が変わるため、タブと画面の名前だけを変えることを既定とした。
    strings.xml を広く触るため、ボタンの言葉をそろえる BL-133 より先に行う
- id: BL-140
  区分: UX改善
  タスク内容: >-
    初めて期限を決めた直後に出す、通知の許可を求める説明を具体的にする（UX レビュー A11）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/reminder/NotificationPermission.kt の Dialog（58〜75 行目）と、
    mobile/src/main/res/values/strings.xml の 100〜102 行目（reminder_permission_rationale・reminder_permission_allow・reminder_permission_later）。
    いまの状態。見出しなしで「期限が近づいたら、お知らせします。」と「お知らせを受け取る／今はしない」だけで、この後に端末の許可ダイアログが出ることも、いつ届くかも書いていない。
    どう変えるか。見出し「通知を使います」、本文「期限の前の日の 9:00 に、スマートフォンの通知でお知らせします。次の画面で『許可』を押してください。」
    （時刻は設定値。BL-143 で ReminderUiState に持たせた時刻を使う）。ボタンは「次へ」と「今はしない」。説明を出す条件（Android 13 以降・初めて期限を決めたとき・案内からは 2 回まで）は変えない
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    説明のダイアログを引数（時刻・押したときの処理）だけで描ける composable に分け、mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/NotificationRationaleTest（新設）で、
    見出し・設定した時刻を含む本文・「次へ」を押すと許可を求める処理が呼ばれることを確かめる。
    軽い検証 python scripts/quality-gates.py --quick --tests "*NotificationRationaleTest" --tests "*ReminderViewModelTest" が成功する。
    ダイアログはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/ を「許可」「通知」で Grep し（2026-11-13 時点では due.md・not-arriving.md に通知の許可を求める流れの記述は無い）、
    due.md に「初めて決めたときに、通知の許可を求めます」を足して python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の「PLAN §5・R-29」の行（説明の文言）を合わせる
  依存:
    - BL-143
  根拠: >-
    2026-11-13 の UX レビュー A11（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    お知らせ画面の状態へ通知の時刻を持たせる BL-143 の後に行い、時刻の読み込みを二重に作らない
- id: BL-139
  区分: UX改善
  タスク内容: >-
    設定の一覧に見出しの区切りと各行の 1 行の説明を付ける（UX レビュー A10。項目と行き先は BL-143 の結果のまま変えない）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt の SettingsItem（58〜74 行目）と SettingsScreen（76〜100 行目）、mobile/src/main/res/values/strings.xml。
    いまの状態。項目が説明なしで平たく並び（2026-11-13 時点で 10 項目、BL-143 の後は 8 項目）、「操作」「データ」などは名前から中身が想像しにくい。
    どう変えるか。一番上の「使い方」は見出しを付けずにそのまま一番上に置く（H-01）。残りを見出しで区切る。
    「見た目と操作」に 着せ替え（テーマ・文字の大きさ・背景の写真）・操作とお知らせ（○ ボタン・振動・お知らせの時刻）・ホーム画面に置く、
    「登録したもの」に お店の管理・よく買う物の管理・データ（書き出し・読み込み）、「このアプリ」に このアプリについて（ご意見・ご要望を含む）。
    各行の下に括弧内の内容を 1 行の説明として出す（onSurfaceVariant の bodyMedium、SettingsSwitchRow と同じ作り。文言は strings.xml）。見出しは semantics の heading を付ける
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AboutContentTest の SettingsScreen を描くテスト（BL-143 で直したもの）か新しい SettingsScreenTest に、
    見出しと説明が出ること、各行を押すと今までどおりの SettingsItem が開くことのテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*AboutContentTest"（SettingsScreenTest を作ったらそれも）が成功する。
    設定の画面はスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/ を「設定の」で Grep し、項目の場所の説明が新しい区切りと食い違う記述があれば直して python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の「PLAN §3（一部）」の行（設定の項目の並び）に見出しと説明を足す
  依存:
    - BL-143
  根拠: >-
    2026-11-13 の UX レビュー A10（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    既定で決めた点。レビューの区切りは「困ったとき」に使い方を入れていたが、DESIGN の H-01 と BL-143（使い方は上に残す）に合わせ、使い方は見出し無しで一番上に残す（項目と行き先を変えないため安全）。
    項目を減らす BL-143 の後に行う
- id: BL-138
  区分: UX改善
  タスク内容: >-
    設定の「着せ替え」で、文字の大きさを一番上に移す（UX レビュー A9）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt の AppearanceContent（72〜120 行目）。
    いまの並びは テーマ（81〜95 行目）→ 背景の写真（BackgroundSection、96 行目）→ 暗い画面（97〜107 行目）→ 文字の大きさ（108〜118 行目）で、
    文字を大きくしたい人ほど小さい文字のままスクロールして一番下を探すことになる。
    どう変えるか。文字の大きさ → テーマ → 暗い画面 → 背景の写真 の順にする（背景の写真は選ぶと膜の濃さ・ペンの色が増えるので最後）。各部品の中身と testTag は変えない
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AppearanceContentTest に、文字の大きさの選択肢がテーマの見本より上にある（表示位置の比較）テストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*AppearanceContentTest" が成功する。
    着せ替えの画面はスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/ の text-size.md・theme.md・background.md・dark.md には並び順の記述が無い（2026-11-13 に確認）ため変えない。
    docs/records/managed/DESIGN.md の「F-60・F-63・F-65」の行に並び順を足す
  依存: []
  根拠: 2026-11-13 の UX レビュー A9（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた
- id: BL-137
  区分: UX改善
  タスク内容: >-
    カレンダー（BL-141 の後は「記録」）を見やすくする。買い物の印を大きく、今月以外を見ているときは「今日」へ戻るボタンを出し、右上の「探す」を文字付きにする（UX レビュー A8）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。kotlin/calendar/MonthGrid.kt の DayCell（188〜192 行目）で、買い物のあった日の印は直径 6dp の点（最大 3 つ、お店ごとの色）。
    MonthHeader（49〜75 行目）は前の月・次の月の矢印だけで、別の月から今日へ戻る手段が無い。
    kotlin/calendar/CalendarScreen.kt の CalendarTopBar（120〜142 行目）の「探す」は虫眼鏡のアイコンだけ（文字は読み上げ用の contentDescription のみ）で、メモ画面の「設定」が文字を添えている（PLAN §2.7）のと不ぞろい。
    どう変えるか。点を 8dp にし、日付の数字との間を 2dp 以上空ける（マスの最小 48dp は保つ）。見ている月が今月でないときだけ、月の見出しの横に「今日」ボタン（文字、48dp 以上）を出し、
    押すと今月の今日を選ぶ（CalendarEvent に新しいイベントを足し、CalendarViewModel の showMonth を使う）。
    「探す」はアイコン＋文字の TextButton にする（検索中は calendar_search_close の意味の文字）。BL-143 で同じ上部のバーに「⚙ 設定」が入っているので、
    文字の大きさ「とても大きい」でも題名と 2 つのボタンが重ならない並びにする
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest（BL-116 で作る）に、今月では「今日」が出ず、別の月では出て押すとイベントが出ることのテストを、
    mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest に今日へ戻るイベントで今月の今日が選ばれるテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*CalendarContentTest" --tests "*CalendarViewModelTest" が成功する。
    見た目が変わるため、スクリーンショットテストの calendar を CLAUDE.md の手順で -Proborazzi.test.record=true で記録し直し、
    画像の差分（点の大きさ・探すの文字、extra_large ではみ出しが無いこと）を確かめてコミットに含める。
    使い方 mobile/src/main/assets/help/calendar.md に「今日」ボタンを足し、search.md の「右上の「探す」」が新しい見た目と合うか確かめて、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-40（点の大きさ・今日へ戻る）と F-42 の行（探すの入口）を合わせる
  依存:
    - BL-130
    - BL-143
  根拠: >-
    2026-11-13 の UX レビュー A8（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    MonthGrid を触る BL-130 と、CalendarTopBar に設定のボタンを足す BL-143 の後に行う。BL-117（検索欄の音声入力）は検索の入口と search.md を触るため本タスクの後にした
- id: BL-136
  区分: UX改善
  タスク内容: >-
    話して追加の確認カードで、外したカードが外れていると一目で分かるようにする（UX レビュー A7）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt の ConfirmCard（198〜244 行目）と、ConfirmCards の「追加する」ボタン（181〜187 行目）、
    mobile/src/main/res/values/strings.xml の voice_add_all（15 行目）。
    いまの状態。× を押すとカードの背景が surfaceVariant に変わるだけでアイコンは同じ ×。もう一度 × で戻るが見た目からは分からない。メモにある物は最初から外れた状態で出る（DESIGN の R-01（音声））。
    どう変えるか。外したカードは品名と数量に取り消し線を引き、文字を薄く（onSurfaceVariant）する。右のボタンは、入れているときは ×、外したときは「↩ 戻す」（アイコン＋文字、48dp 以上）に替える。
    contentDescription（voice_exclude_card・voice_include_card）は今のまま。「追加する」ボタンに入れる件数を出す（例「3 つ追加する」。件数を引数に取る文字列を strings.xml に置く。0 件のときは今どおり押せない）
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest に、外したカードに「戻す」が出ること、追加ボタンの文字が入れる件数に合うことのテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*VoiceInputContentTest" --tests "*VoiceInputViewModelTest" が成功する。
    音声入力のシートはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/voice.md の「押すと直せて、× で外せます」と「「追加する」でメモに入ります」を新しい見た目に合わせ（外したカードは「戻す」で戻せる）、
    python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の「F-01・PLAN §3.2」の行（確認カード）を合わせる
  依存: []
  根拠: 2026-11-13 の UX レビュー A7（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた
- id: BL-135
  区分: UX改善
  タスク内容: >-
    カレンダーの記録を消したら「元に戻す」を出し、押し間違えても記録と期限のお知らせを戻せるようにする（UX レビュー A6）。パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。kotlin/calendar/RecordEditSheet.kt の DeleteAndSave（106〜124 行目）の「この記録を消す」は確認も取り消しも無く、
    kotlin/calendar/CalendarViewModel.kt の deleteRecord（263〜268 行目）が kotlin/data/RecordRepository.kt の deleteRecord で即座に消す。
    記録に付いたお知らせは外部キーの CASCADE で一緒に消え、購入回数・最後に買った日・平均間隔は計算し直される（DESIGN の R-12）。消すボタンと保存のボタンが同じ行の左右にある。
    どう変えるか。メモの「買った」「やめた」と同じ考え方（確認は出さず取り消しで救う、PLAN §1.3-4。kotlin/shoppinglist/ShoppingListViewModel.kt の writeUndoable・UndoOffer と
    kotlin/shoppinglist/ShoppingListScreen.kt の UndoEffect を参考にする）で、消した後にカレンダーの画面に「消しました：品名 ／ 元に戻す」を 5 秒（読み上げ利用時は UndoEffect と同じく延長）出す。
    元に戻すと、記録を同じ id で、紐づいていたお知らせ（状態・期限・後での日付）も同じ id で戻し、品目の集計を計算し直し、次の通知を予約し直す（reminder/ReminderScheduler.kt）。
    写真のファイルは消さない（参照が戻るだけ。R-14 の写真の掃除 data/PhotoCleaner.kt は端末を使っていない間だけ動き、書いて 1 日未満の写真を残す仕組みのため、元に戻すを待つ間に消えないことを確かめ、足りなければ対策する）。
    あわせて「この記録を消す」をシートの一番下へ離して置き、閉じるボタン（BL-144 で「閉じる」になる）と同じ行に並べない
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    記録を消すと元に戻すが出て、押すと記録・お知らせ・集計が消す前と同じになる。テストの test/ は mobile/src/test/kotlin/com/ikumemo/mobile/。
    test/data/RecordRepositoryTest に「消して戻すと記録とお知らせが同じ id で戻り、集計が元に戻る」テストを、
    test/calendar/CalendarViewModelTest に「消すと元に戻すの申し出が出て、押すと戻る・時間が過ぎると消える」テストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*RecordRepositoryTest" --tests "*CalendarViewModelTest" --tests "*CalendarContentTest" が成功する。
    カレンダーの画面に元に戻すの表示を足すため、スクリーンショットテストの calendar を CLAUDE.md の手順で比べ、差分が出たら意図したものか確かめて -Proborazzi.test.record=true で記録し直し、画像をコミットに含める。
    使い方 mobile/src/main/assets/help/edit-record.md の「消した記録は元に戻せません」を直し、undo.md（今は「買った」「やめた」だけ）に記録を消したときの元に戻すを足して、
    python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-16・R-12 の行（写真の扱いを変えたら R-14 も）に、記録の削除の元に戻すを足す
  依存:
    - BL-144
  根拠: >-
    2026-11-13 の UX レビュー A6（効果 大）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    同じ DeleteAndSave を触る BL-144（閉じたら保存）の後に行う
- id: BL-134
  区分: UX改善
  タスク内容: >-
    設定の「データ」画面で、「データをすべて消す」を書き出しから離して目立たせ、処理中と結果が分かるようにする（UX レビュー A5）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt の DataContent（77〜117 行目）・DataRow（119〜143 行目）・ConfirmDialog（169〜190 行目）、
    settings/DataViewModel.kt（DataUiState の busy・result）、mobile/src/main/res/values/strings.xml の 158〜179 行目。
    いまの状態。書き出し 3 つ・読み込み・すべて消すの 5 行が同じ見た目で続き、「データをすべて消す」に色も区切りも無い。確認ダイアログの「すべて消す」も普通の文字ボタン。
    処理中は行が押せなくなるだけで進み具合の表示が無い（busy は enabled にしか使っていない）。結果の文字は一覧の一番下に出る。
    どう変えるか。見出し「書き出す」「読み込む」「消す」で 3 つに区切り、写真つきの書き出しを一番上に置く。「データをすべて消す」は区切り線の下に離し、文字を colorScheme.error にする。
    確認ダイアログの「すべて消す」も error 色にする（読み込みの確認は色を変えない）。処理中は押した行に CircularProgressIndicator と「書き出しています…」などの文字（strings.xml）を出す
    （どの行を押したかを DataUiState に持たせる）。結果は一覧の一番上（最初の見出しの上）に出す
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest に、見出し 3 つの順、処理中に進み具合の表示が出ること、結果が一番上に出ることのテストを、
    mobile/src/test/kotlin/com/ikumemo/mobile/settings/DataViewModelTest に処理中の行の状態のテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*DataViewModelTest" が成功する。
    設定の画面はスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/data-export.md に区切り・消す行の位置に関わる記述があれば直し（2026-11-13 時点では書き出しの各行の説明だけで、消す操作の記述は無い）、
    変えたら python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-72 と F-76 の行に、区切り・消す行の色・処理中の表示・結果の位置を足す
  依存: []
  根拠: >-
    2026-11-13 の UX レビュー A5（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    レビューは結果の出し方を「スナックバー、または一覧の一番上」としていた。設定の画面にはスナックバーを出す Scaffold が無く、文字が大きい設定でも画面の外へ出ないため、一番上を既定とした
- id: BL-133
  区分: UX改善
  タスク内容: >-
    ダイアログのボタンの言葉をそろえる（UX レビュー A4。文言だけで動きは変えない）。パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/、strings.xml は mobile/src/main/res/values/strings.xml。
    いまの状態（2026-11-13 時点の行番号）。日付・時刻の確定が common_done「できた」（kotlin/reminder/DueSheet.kt 79 行目、kotlin/calendar/RecordEditSheet.kt の RecordDatePicker 139 行目、
    kotlin/settings/ReminderSettingsScreen.kt の TimePickerDialog 119 行目）、取り消しが common_close「閉じる」（DueSheet.kt 84 行目、RecordEditSheet.kt 142 行目）と
    reminder_settings_cancel「やめる」（ReminderSettingsScreen.kt 123 行目、kotlin/settings/DataScreen.kt 188 行目、kotlin/settings/StoreManageScreen.kt 130・166 行目）でばらばら。
    ダイアログの「やめる」はメモの「やめた」（買うのをやめる）と紛らわしい。
    どう変えるか。ダイアログの確定ボタンは新しい文字列「決める」（例 common_decide）、取り消しはすべて common_back「戻る」にする（レビューの推奨。「やめた」との混同を避けるため）。
    確定は右・取り消しは左の並び（Material の既定）を保つ。使われなくなった reminder_settings_cancel は消す。
    対象外。音声の確認カードを直すときの「できた」（kotlin/voice/VoiceInputSheet.kt 269 行目。ダイアログではなくカードの編集の終わり）、
    シートを閉じる「閉じる」（ItemSheets.kt・EditItemSheet.kt、BL-144 の後の記録の編集シート）、通知の許可の「今はしない」（BL-140 で扱う）。
    記録の編集シートの「直す」はレビューでは「保存する」案だが、BL-144（閉じたら保存）で「閉じる」になるため触らない。
    BL-134・BL-135・BL-143 の後は行の位置が変わっているので、実装時に common_done・common_close・reminder_settings_cancel で Grep して探し直す
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    上の対象のダイアログがすべて「決める」と「戻る」になっている。ボタンの文字で探している既存のテストがあれば直す（2026-11-13 時点では無い）。
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest に、確認ダイアログの取り消しが「戻る」であることのテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*ManageContentTest" --tests "*OperationContentTest" が成功する
    （BL-143 でまとめた設定の画面のテストの名前が変わっていたら、その名前で流す）。
    ダイアログはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/ を「できた」「やめる」「閉じる」で Grep し、ダイアログのボタンを指す記述があれば直して python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す（2026-11-13 時点では該当なし）。
    docs/records/managed/DESIGN.md の「UI/UX 方針」に、ダイアログの確定は「決める」・取り消しは「戻る」の決まりを 1 行足す
  依存:
    - BL-134
    - BL-135
    - BL-141
    - BL-143
  根拠: >-
    2026-11-13 の UX レビュー A4（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    既定で決めた点。取り消しはレビューの推奨の「戻る」。音声カードの「できた」とシートの「閉じる」はダイアログではないため対象外。
    レビューが挙げていない StoreManageScreen.kt の 2 つのダイアログも同じ「やめる」を使っているため対象に含めた。
    同じダイアログを触る BL-134（データの確認）・BL-135（記録の編集シート）・BL-143（時刻のダイアログの移動）と、strings.xml を広く触る BL-141 の後に行う
- id: BL-132
  区分: UX改善
  タスク内容: >-
    お知らせの「期限を決める」シートの候補（3日後・1週間後・2週間後・1か月後）に、実際の日付を添える（UX レビュー A3）。
    対象は mobile/src/main/kotlin/com/ikumemo/mobile/reminder/DueSheet.kt の 52〜59 行目（DueChoice ごとの AssistChip。日付は choice.from(today)）と、
    mobile/src/main/res/values/strings.xml の 87〜90 行目（reminder_due_3days など）。いまは「1週間後」などの文字だけで、何日になるかは決めた後の一覧で分かる。
    どう変えるか。チップの文字を 2 行にし、下の行に日付を曜日付きで出す（例「1週間後」と「11月20日（金）」）。日付の書式は strings.xml に新しい文字列として置き、
    曜日は端末の言語によらず日本語にする（calendar/CalendarScreen.kt の dayTitle と同じ作り）。「日付を選ぶ」のチップは 1 行のまま。押せる高さ 48dp 以上を保つ
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    4 つの候補に今日から数えた日付が出る。日付の文字に使う値（月・日・曜日）を作る処理を純粋な関数にし、
    mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest か新しい DueChoiceTest で月をまたぐ日付（例 1 月 30 日の 1 か月後）を確かめる。
    DueSheet を直接描く Compose のテスト（mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/DueSheetTest を新設）でチップの文字を確かめる。
    軽い検証 python scripts/quality-gates.py --quick --tests "*DueSheetTest" --tests "*ReminderViewModelTest"（DueChoiceTest を作ったらそれも）が成功する。
    シートはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方 mobile/src/main/assets/help/due.md の「3 日後・1 週間後・2 週間後・1 か月後から選べます」に日付も一緒に出ることを足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-50 の行の候補の説明に日付を添えることを足す
  依存: []
  根拠: 2026-11-13 の UX レビュー A3（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた
- id: BL-131
  区分: UX改善
  タスク内容: >-
    メモの「文字で追加」シートとカレンダーの「買った物を記録する」シートを開いたら、品名の入力欄にフォーカスを当ててキーボードを出す（UX レビュー A2）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。kotlin/shoppinglist/ItemSheets.kt の AddItemSheet（53〜61 行目の OutlinedTextField、testTag は TestTags.ADD_SHEET_INPUT）と、
    kotlin/calendar/AddRecordSheet.kt の 72 行目（EditField、testTag は CalendarTestTags.ADD_RECORD_NAME。EditField は kotlin/shoppinglist/EditItemSheet.kt 104〜121 行目で共用）は、
    どちらもフォーカスを当てていない（FocusRequester を使っているのは kotlin/guide/GuideOverlay.kt だけ）。開いた後に入力欄を押さないとキーボードが出ない。
    どう変えるか。シートが開いたら FocusRequester.requestFocus() で品名の欄にフォーカスを当て、キーボードを出す。ショートカット・ウィジェットの「文字で追加」から開いたときも同じ。
    EditField に引数を足す場合、記録の編集シートとメモの編集シート（自動でフォーカスしない）の動きは変えない
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    2 つのシートを開くと品名の欄がフォーカスされている。
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest に文字で追加のシートを開くと ADD_SHEET_INPUT が assertIsFocused になるテストを、
    AddRecordSheet を直接描くテスト（mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/AddRecordSheetTest を新設）に ADD_RECORD_NAME が assertIsFocused になるテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*ShoppingListScreenTest" --tests "*AddRecordSheetTest" が成功する。
    シートはスクリーンショットテストの対象外のため記録し直しは不要。
    使い方の mobile/src/main/assets/help/text.md と add-later.md には入力欄を押す手順が書かれていない（2026-11-13 に確認）ため変えない。
    docs/records/managed/DESIGN.md の F-03 と F-17 の行に、開くと品名の欄にフォーカスしてキーボードを出すことを足す
  依存: []
  根拠: 2026-11-13 の UX レビュー A2（効果 中）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。追加のたびに 1 タップ減る
- id: BL-130
  区分: UX改善
  タスク内容: >-
    背景の写真を設定しているとき、カレンダーとお知らせ（BL-141 の後は「記録」「期限」）の文字が写真に溶けないよう、写真の上に直接描いている一覧にテーマの面を敷く（UX レビュー A1、推奨の案 1）。
    パスの kotlin/ は mobile/src/main/kotlin/com/ikumemo/mobile/。
    いまの状態。kotlin/designsystem/AppThemeHost.kt の 49〜56 行目はペンの色（InkColor）で onBackground と LocalContentColor だけを差し替える。
    一方、kotlin/calendar/MonthGrid.kt の weekdayColor（136〜145 行目）は平日が colorScheme.onSurface、日曜・土曜が CalendarColors（テーマの明暗だけで決まる）で、DayCell の日付（183〜187 行目）もその色。
    kotlin/calendar/CalendarScreen.kt の RecordRowView（248〜287 行目）と検索結果の補足は onSurfaceVariant、
    kotlin/reminder/ReminderScreen.kt の RecentRow・PendingRow（201〜272 行目）は「○月○日に買った」と期限の文字が onSurfaceVariant と error、「使い切った」「捨てた」は TextButton の primary。
    これらの行は不透明な面を持たず写真と膜（designsystem/BackgroundScrim.kt）の上に直接描かれるため、明るいテーマ＋ペン「しろ」（暗い膜）や暗いテーマ＋「くろ・あお・あか」（明るい膜）で読めなくなる。
    メモの行だけは surface を敷いている（kotlin/shoppinglist/SwipeableItemRow.kt）。
    どう変えるか。LocalHasBackgroundImage が true のとき、カレンダーの月の表（MonthHeader と MonthGrid）、日の詳細（見出し・記録の行・「買った物を記録する」）、検索の欄と結果、
    お知らせの各セクションの行を、テーマの surface を不透明に敷いた Surface かカードで囲む（写真は余白と見出しの周りにだけ見える形）。
    背景の写真が無いときは敷かず、今の見た目を変えない。ペンの色の仕組み（InkColor）は変えない
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    背景の写真があるとき、カレンダーとお知らせの日付・曜日・補足・期限・ボタンの文字が surface の上に描かれ、写真が無いときの見た目は変わらない。
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/ の calendar/CalendarContentTest と reminder/ReminderContentTest（BL-116 で作る）に、
    LocalHasBackgroundImage を true にして描くと面（新しい testTag）が出ること、false では出ないことのテストを足す。
    軽い検証 python scripts/quality-gates.py --quick --tests "*CalendarContentTest" --tests "*ReminderContentTest" --tests "*ThemeContrastTest" --tests "*InkColorTest" が成功する。
    スクリーンショットテスト（背景の写真なしで撮っている）の calendar と reminder を CLAUDE.md の手順で画面ごとに比べ、差分が無いことを確かめる
    （差分が出たら意図したものか確かめて -Proborazzi.test.record=true で記録し直し、画像をコミットに含める）。
    使い方 mobile/src/main/assets/help/background.md に、カレンダーとお知らせの一覧は写真の上に面を敷いて読みやすくすることを足し、python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直す。
    docs/records/managed/DESIGN.md の F-61 の行（「メモの行・バー・下タブなどテーマの面の上の文字は変えない」の箇所）に、カレンダーとお知らせの一覧にも面を敷くことを足す
  依存:
    - BL-116
    - BL-142
  根拠: >-
    2026-11-13 の UX レビュー A1（効果 大）。オーナーが 2026-11-13 に A の 11 件の採用を決めた。
    レビューの 2 案のうち、ペンの色ごとに onSurface・CalendarColors・primary・error を選び直す案 2 は色の組み合わせが増え、
    コントラストの自動テスト（ThemeContrastTest はテーマの面の上しか測らない）で守りにくいため、レビューが推奨する案 1（面を敷く）を既定とした。
    お知らせ画面の並びを変える BL-142 の後に行う（同じ ReminderList を触るため）
- id: BL-128
  区分: 品質ゲート
  タスク内容: >-
    Gradle のビルドキャッシュ（gradle.properties の org.gradle.caching=true）に、ktlint の誤った結果が残る事故への対策を決めて入れる。
    BL-119 の確認で core に一時的に置いて消した書式違反のファイル（ZzGateTmp.kt）の違反が、ファイルを消した後も
    :core:runKtlintCheckOverMainSourceSet の FROM-CACHE で復元され、ktlint のゲートが失敗し続けた（BL-113 の実施中に発見）。
    案は (1) scripts/quality-gates.py の失敗の表示で、違反の出たファイルが存在しないときに「キャッシュの古い結果の疑い」と出し、
    ./gradlew <該当タスク> --rerun の対処を示す、(2) 自律ループの手順に「一時的なファイルで ktlint を試したら、消した後に該当タスクを --rerun で流す」を足す。
    どちらにするかは実装時に決め、根拠を記録する
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    選んだ対策が入り、存在しないファイルの違反がキャッシュから出たときに、原因と対処が利用者に分かる。python scripts/quality-gates.py が成功する
  依存: []
  根拠: >-
    2026-11-13 の BL-113 で、quality-gates.py の ktlint が 2 回続けて存在しないファイル ZzGateTmp.kt の違反で失敗した。
    core/build/intermediates/ktLint を消しても再現し、gradle.log で runKtlintCheckOverMainSourceSet が FROM-CACHE だったため、
    入力が元に戻った時点のキャッシュの項目に違反入りの結果が保存されたと推測（ktlint の Gradle プラグインの増分処理が原因かは未確認）。
    ./gradlew :core:runKtlintCheckOverMainSourceSet --rerun で解消した
- id: BL-127
  区分: 品質ゲート
  タスク内容: >-
    スクリーンショットテスト（Roborazzi）を画面ごとに比べる・記録し直す手順を、リポジトリのスキル .claude/skills/screenshot-verify/SKILL.md として新設する。
    内容は (1) 流す前に ./gradlew --stop、(2) 画面ごとに 1 回ずつ流す（memo・calendar・reminder。3 画面まとめるとメモリ不足）コマンド
    ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"、
    (3) 失敗時に mobile/build/outputs/roborazzi/ 配下の差分画像の場所を示し、画像は縮小・必要な画面だけ読む（ユーザー共通スキル verification-cost-control に従う）、
    (4) 見た目を意図して変えたときだけ -Proborazzi.test.record=true で記録し直し、git diff --stat mobile/screenshots/ で変わった画像を列挙してコミットに含める、
    (5) 見た目に関わる変更かどうかの判断基準（designsystem/・*Screen.kt・strings.xml の変更など）。
    frontmatter の description は日本語で「スクリーンショットを比べて」「Roborazzi」「見た目の回帰」等の依頼で使う旨を書き、model は sonnet を指定する
    （定型の手順だが、差分が意図したものかの判断を含むため。ユーザー共通スキル skill-model-pinning の分類に従う）。
    CLAUDE.md「本リポジトリの品質ゲート定義」のスクリーンショットの段落は、詳細をスキルへ寄せて 1〜2 行の参照に縮める（コマンドの正本は CLAUDE.md に残す）
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    .claude/skills/screenshot-verify/SKILL.md があり、CLAUDE.md から参照されている。
    npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml が Summary 0 issues（Linting の件数が git ls-files の *.md 件数と一致）、python scripts/validate-records.py が成功。
    手順のコマンドで memo 画面の比較を 1 回流し、終了コード 0 を確かめて記録している（Gradle は通常のコマンドで流す）
  依存: []
  根拠: >-
    手順が CLAUDE.md の品質ゲート定義の箇条書きに 3 行で埋もれており、差分画像の置き場所・画像の読み方・記録し直しの判断が書かれていない。
    スクリーンショットテストは mobile/src/testDebug/kotlin/com/ikumemo/mobile/screenshot/MainScreensScreenshotTest.kt、正解画像は mobile/screenshots/{memo,calendar,reminder}。
    リポジトリのスキルは .claude/skills/ に 4 つ（autonomous-loop・dependabot-alert-triage・docs-consistency-review・pr-create）で、該当するものが無い
- id: BL-126
  区分: 品質ゲート
  タスク内容: >-
    変更の点検を受け持つサブエージェント定義 .claude/agents/change-reviewer.md を新設する。
    入力は「作業ブランチの直近 1 コミット（または指定範囲）の差分と、対象の BACKLOG の id」。読むのは git diff・該当 BACKLOG 項目・DESIGN.md の関係する節だけ。
    点検の観点は (1) BACKLOG の完了条件を満たしているか、(2) DESIGN.md・EXECUTE.md・CHANGELOG.md の書き分け（.github/copilot-instructions.md「記録の書き分け」）、
    (3) 暗黙の仕様変更が無いか、(4) 文字列の直書き・contentDescription の抜け・タッチ領域 48dp 未満、(5) 秘密情報・個人情報の混入、(6) テストが変更に対応しているか。
    結果は「重大／要修正／提案」の 3 段で、ファイルパスと行番号を付けて返す。ファイルは編集しない（tools は Read・Grep・Glob・Bash のみ。Bash は git diff・git log・git show に限ると本文に書く）。
    frontmatter に model sonnet を指定する（読み取り中心で判断を含むため）。
    autonomous-loop の委任の節（BL-121 で追加）に、実装担当の完了後・コミット前に呼べることを 1 行追記する
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    .claude/agents/change-reviewer.md があり、frontmatter（name・description・tools・model）が Claude Code のサブエージェント定義の形式に沿っている。
    直近のコミット（例 ba45e98）を対象に 1 回呼んで、指摘が 3 段の形式で返ることを確かめ、結果の要約を CHANGELOG.md に書いている。
    markdownlint が Summary 0 issues、python scripts/validate-records.py が成功
  依存: []
  根拠: >-
    .claude/agents/ が存在せず、サブエージェント定義が 1 つも無い。点検は現在オーケストレータが毎回プロンプトで指示しており、観点が回ごとにぶれる。
    観点 (4) はアプリ側の既存の方針（DESIGN.md「PLAN §8 アクセシビリティ」の行、AccessibilityTest）に合わせたもの
- id: BL-125
  区分: 品質ゲート
  タスク内容: >-
    品質ゲートを流して失敗だけを短く要約するサブエージェント定義 .claude/agents/quality-gate-runner.md を新設する。
    本文の手順は BL-119 で作る scripts/quality-gates.py を 1 回、通常のコマンド（バックグラウンドにしない、タイムアウト 10 分）で流し、
    失敗したゲートごとに「ゲート名・失敗したファイルと行・エラーの要旨（各 3 行以内）」を返すこと。ログ全文は返さない。修正はしない（tools は Bash・Read・Grep）。
    同時に複数のサブエージェントで Gradle を流さない（PC のメモリが少ない、CLAUDE.md）ことを本文の最初に書く。
    frontmatter に model haiku を指定する（コマンドを流して出力を要約する定型作業のため）
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    .claude/agents/quality-gate-runner.md があり、作業ブランチの現状で 1 回呼んで、全ゲートの成否が表で返ることを確かめている。
    markdownlint が Summary 0 issues、python scripts/validate-records.py が成功
  依存: []
  根拠: >-
    Gradle の 5 ゲートの出力は長く、実装担当の文脈を圧迫する。CLAUDE.md「本リポジトリの品質ゲート定義」は Gradle をバックグラウンドで流すと強制停止されると定めており、
    流し方の注意を毎回プロンプトで渡している。.claude/agents/ は存在しない
- id: BL-123
  区分: 品質ゲート
  タスク内容: >-
    rules/guardrails-unified.v1.md の 12 節（自律ループ実行モード統制）に、サブエージェントへの委任の統制を 12.7 として足す提案。
    案は (1) 委任先も 12.1〜12.6 に従い、委任の指示でこれを緩めない、(2) コミットは 1 タスク 1 コミットを保ち、委任先が並行して同じ作業ツリーへコミットしない、
    (3) 委任先が要確認を自己解決しない（結果として返し、オーケストレータが BACKLOG に残す）、(4) 委任先の起動・結果は実施記録または CHANGELOG で追える、
    (5) 12.1 の「秘密情報スキャン」の具体的な手順の正本を CLAUDE.md（BL-119 で定める）とする参照を足す。
    変更する場合は同ファイル 10 節に従い CHANGELOG.md を更新する
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: オーナーが案の採否を決め、採用する場合はガバナンス担当の承認を得たうえで本文が更新され、markdownlint と validate-records が成功している
  依存: []
  根拠: >-
    2026-11-13 にオーナーが、タスクに書いた 5 点の案で 12.7 を足すことを承認した。
    オーナーへの質問は「ガードレール 12 節にサブエージェント委任の統制（12.7）を足してよいか、足すなら上の 5 点でよいか」。
    同ファイル 10 節が重大変更に承認を求めるため要確認とした。現在の 12 節は単一エージェントの実行だけを想定し（rules/guardrails-unified.v1.md 12.1〜12.6）、
    12.1 の秘密情報スキャンには具体的な手順がリポジトリのどこにも書かれていない（CLAUDE.md・CONTRIBUTING.md・.claude/skills/ を検索して確認）
- id: BL-118
  区分: 人手検証
  タスク内容: >-
    実機で BL-113・BL-115 の結果を確かめる。写真の「選ぶ」で選んだ直後に元の写真をギャラリーから消す・クラウドの写真を選ぶなどで読めない写真を渡しても落ちないこと、
    TalkBack でカレンダーの日が「選択中」と読まれること、困ったときの質問が開閉の状態と一緒に読まれること、おしらせの行を押す操作が「期限を決め直す」と読まれること
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 上の 4 点を実機で確かめ、読まれ方が不自然な箇所があれば画面名と読まれた文言を記録している
  依存: []
  根拠: 読めない写真の発生のさせ方と TalkBack の読み上げは実機でしか確かめられないため
- id: BL-117
  区分: 機能追加
  タスク内容: >-
    カレンダーの品名検索の欄に音声入力（マイク）を付ける（PLAN F-42 の未実装分）。編集シートの各欄で使っている FieldMic（mobile/src/main/kotlin/com/ikumemo/mobile/voice/FieldDictation.kt）を
    calendar/CalendarScreen.kt の検索欄（SearchPane 付近、文言は strings.xml の calendar_search_hint）の trailingIcon に付け、認識した文字で検索する。
    使い方の正本 mobile/src/main/assets/help/ の該当ページと docs/USER_GUIDE.md（python scripts/make-user-guide.py で再生成）、DESIGN.md の F-42 の行を更新する
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    検索欄にマイクがあり、話した品名で検索結果が出る。CalendarViewModel か Compose のテストで、マイクの結果が検索語に入ることを確かめている。
    CLAUDE.md の品質ゲートがすべて成功し、見た目が変わるためスクリーンショットテストの calendar を比べている
  依存:
    - BL-137
  根拠: >-
    2026-11-13 にオーナーが、カレンダーの検索欄に音声入力を付けることを承認した。
    オーナーへの質問は「カレンダーの検索欄に音声入力（マイク）を付けてよいか」。DESIGN.md の F-42 の行が「検索欄の音声入力は未実装」と明記しており、
    部品（FieldMic）は EditItemSheet.kt 118 行目で既に使われているため小さく足せるが、画面に新しい操作を足す製品判断のため要確認とした。
    2026-11-13 の UX レビューの取り込みで、検索の入口（探すのボタン）と使い方の search.md を変える BL-137 の後に行うよう依存を足した
- id: BL-116
  区分: 品質ゲート
  タスク内容: >-
    カレンダーとおしらせの画面に、スクリーンショット以外の Compose のテスト（Robolectric）を足す。
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt と reminder/ReminderContentTest.kt を新設し、
    公開されている CalendarContent（calendar/CalendarScreen.kt 81 行目）と ReminderContent（reminder/ReminderScreen.kt 91 行目）に状態を渡して、
    日を押すと日を選ぶイベント（CalendarEvent の該当するもの）が出る、買い物の無い日の文言、検索結果を押すとその日へ移るイベント、おしらせの「使い切った」「捨てた」のイベント、
    通知オフの帯（ReminderTestTags.NOTIFICATIONS_OFF）の表示を確かめる。既存の settings/DataContentTest.kt・shoppinglist/ShoppingListScreenTest.kt の書き方に合わせる。本体のコードは変えない。
    後続の BL-142・BL-130・BL-143・BL-137 がこの 2 つのテストクラスへテストを足すため、状態のサンプルの作り方と描き方を後から足しやすい形にする
  優先度: P1
  状態: 未着手
  担当: AIエージェント
  完了条件: 新しい 2 つのテストクラスが ./gradlew testDebugUnitTest で通り、CLAUDE.md の Gradle 系の 5 ゲートがすべて成功する
  依存: []
  根拠: >-
    mobile/src/testDebug/ の Compose のテストは guide・help・settings・shoppinglist・voice・designsystem にあるが、calendar と reminder の画面は
    MainScreensScreenshotTest.kt（遅いテスト扱いで通常は流さない）にしか無く、通常の品質ゲートでは画面の操作が確かめられていない。
    BL-115 が同じ画面の意味づけ（semantics）を変えるため、その後に書く。
    2026-11-13 の UX レビューの取り込みで P3 から P1 に上げた。同じ 2 画面の並びと見た目を変える P1 のタスク（BL-142・BL-130・BL-143）の前に画面の操作のテストを用意し、
    変えたときの退行を通常のゲートで見つけられるようにするため（P3 のままだと依存する P1 のタスクが P2 のタスクの後まで待つ）
- id: BL-111
  区分: 人手検証
  タスク内容: >-
    実機で、1×1「話して追加」で足したメモが 4×4 に反映されること（BL-110）と、更新ボタンを押したときに回る表示が出ることを確かめる。
    ホーム画面に置いたまま、しばらく経ってからも足して確かめる
  優先度: P1
  状態: 未着手
  担当: ユーザー
  完了条件: 話して足したメモが、更新ボタンを押さなくても、または押せば 4×4 に出る。更新ボタンを押すと矢印が回って戻る
  依存: []
  根拠: ホーム画面のウィジェットのセッションの挙動は実機でしか確かめられないため
- id: BL-109
  区分: 人手検証
  タスク内容: >-
    実機で背景の写真とペンの色（BL-108）を確かめる。明るい写真・暗い写真の両方で、おまかせ・くろ・あお・あか・しろを選び、メモ・カレンダー・おしらせ・設定の各画面で
    写真の上に直接置かれた文字が読めるか、しろ（暗い膜）でテーマの面（行・バー）の中の文字が読めなくならないかを見る。
    UX レビュー C1 として、BL-130（カレンダーとお知らせの一覧に面を敷く）の後に、明るいテーマ＋しろ・暗いテーマ＋くろ で
    カレンダー（BL-141 の後は記録）の日付・曜日と、お知らせ（同じく期限）の説明・「使い切った」が読めるかを特に見る。BL-130 の前に確かめた場合は、読めなかった箇所を前の状態として記録しておく
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: >-
    各ペンの色で、全画面の文字が読める。読めない箇所があれば画面名と色を記録している。
    BL-130 の後、上の 2 つの組み合わせでカレンダーとお知らせの文字が腕を伸ばした距離で全部読める
  依存:
    - BL-130
  根拠: >-
    写真の明るさと各画面の文字の置かれ方の組み合わせは実機でしか確かめられないため。単体テストでは文字と膜の色の比（4.5:1 以上）だけを見ている。
    2026-11-13 の UX レビュー C1 は同じ確認のため、新しく作らずここへ足した
- id: BL-106
  区分: 人手検証
  タスク内容: >-
    実機でメモ一覧ウィジェットの更新ボタン（BL-105）を確かめる。4×4・4×2・幅が狭い状態で見出し（件数・期限が近い件数・更新ボタン）が収まるか、
    メモを足した直後に押すと反映されるか、読み上げ（TalkBack）で「メモ一覧を更新」と読まれるかを見る
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 3 つの大きさで更新ボタンが欠けずに押せ、メモ帳風の罫線（BL-107）が自然に見え、色と角の丸みが本体のテーマと合っており（BL-112）、押すと最新のメモに描き直され、TalkBack で読み上げられる
  依存: []
  根拠: ホーム画面のウィジェットの描画と TalkBack は実機でしか確かめられないため。単体テストではボタンの有無だけを見ている
- id: BL-100
  区分: 人手検証
  タスク内容: >-
    実機（中級機）で Baseline Profile を Macrobenchmark で作り直し（:baselineprofile モジュールの追加）、コールドスタート 1.5 秒以内（PLAN §8）を計測する。
    いまの mobile/src/main/baseline-prof.txt はアプリのコード全体を対象にした広い規則
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: 実機で作った規則に置き換わり、中級機でのコールドスタートが 1.5 秒以内であることを計測値で確認している
  依存: []
  根拠: Baseline Profile の作成と起動時間の計測には実機・エミュレータが要るため、BL-089 では広い規則で先に入れた（2026-10-25）
- id: BL-091
  区分: 人手検証
  タスク内容: M10 製品版公開。Play 製品版リリースと GitHub Releases v1.0.0 の公開（PLAN §10 M10・§10.3 公開の判定基準）
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: PLAN §10.3 の 9 項目を満たし、製品版が公開され v1.0.0 のタグとリリースノートが公開されている
  依存:
    - BL-090
  根拠: Play Console の操作と git push はエージェントが実行できない外部操作のため人手検証として扱う
- id: BL-090
  区分: 人手検証
  タスク内容: M9 クローズドテスト（12 人・14 日以上、案内は docs/CLOSED_TEST.md）とフィードバック反映の判断
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: クローズドテストを 14 日以上実施し、重大不具合ゼロである
  依存:
    - BL-088
  根拠: テスターの募集と Play Console の操作はエージェントが実行できない外部操作のため
- id: BL-088
  区分: 人手検証
  タスク内容: >-
    ユーザビリティテスト（対象ペルソナ 3〜5 人、説明なしで「話して追加 → 買った → 記録のタブ（BL-141 の前はカレンダー）で確認」）（PLAN §10 M8・§11）。
    あわせて UX レビュー C3 として、メモ画面のその場のガイドが G-02 → G-03 → G-04 → G-05 → G-06 と続く間にうっとうしく感じないか、
    「ガイドを出さない」を誤って押しやすくないかを聞く
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: >-
    主要タスクが説明なしで完了し、ガイドで詰まらないことを観察で確認している。ガイドについて 3 人中 2 人以上が「助かった」と答える（下回ったら感想を記録している）
  依存:
    - BL-141
  根拠: >-
    2026-11-13 の UX レビュー C3 は観察する人とペルソナが同じため、新しく作らずここへ足した。
    下タブの名前を変える BL-141 の後に、変えた後の名前で迷わないかも含めて見る
- id: BL-087
  区分: 人手検証
  タスク内容: 公開準備の外部作業。Google フォーム作成（D-20・D-21）、Play Console の申告（データセーフティ・コンテンツレーティング・広告・対象年齢）、ストア名の再検索と J-PlatPat での商標確認（PLAN §1.2・§6・§7.3）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: フォームの URL が決まり、Play Console の各申告が完了し、名前・商標の確認結果が記録されている
  依存: []
  根拠: 外部サービスのアカウント操作が必要で、エージェントが実行できないため
- id: BL-086
  区分: 機能追加
  タスク内容: site/（アプリ紹介ページ）とご意見フォームへの転送ページ site/feedback/ を sesami-wear の site/ をテンプレートに作成する（PLAN §7.2・§7.3）
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: site/ 配下に紹介ページと転送ページがあり、転送先 URL が 1 か所の設定で差し替えられる
  依存:
    - BL-087
  根拠: 転送先の Google フォーム URL が BL-087 で決まるまで完成できないため依存に置く
- id: BL-073
  区分: 人手検証
  タスク内容: >-
    複数のホーム画面アプリ（Pixel・Galaxy 等）でウィジェットの配置・サイズ変更・ダークモード・タップ先を実機確認する（PLAN §11）。
    せってい「ホーム画面に置く」からの追加（W-03、メモ一覧は 4×4 で置かれ長押しで 4×2 へ小さくできるか）と、
    アイコン長押しのショートカット 4 種（F-74）も確かめる
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 2 種のウィジェットが各ホーム画面アプリで崩れずに表示され、タップ先が PLAN §2.11 のとおり開く
  依存: []
- id: BL-053
  区分: 人手検証
  タスク内容: >-
    おしらせの実機確認（メーカー独自の省電力、再起動後、電源オフ後の R-28、通知のボタン操作）。
    あわせて UX レビュー C7 として、通知が設定の時刻に届くか、通知の「使い切った」「また買う」「後で」の意味が説明なしで通知だけで伝わるかを見る
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: >-
    Pixel と他メーカー端末で期限前日の通知が設定の時刻に届き、再起動・電源オフ後も漏れない。
    通知のボタンを説明なしで正しく選べる（迷ったボタンがあれば記録している）
  依存: []
  根拠: 2026-11-13 の UX レビュー C7 は通知の実機確認と同じ場面のため、新しく作らずここへ足した
- id: BL-023
  区分: 人手検証
  タスク内容: 実機で話した認識結果を集め、音声分割の単体テストケースへ追加する素材を用意する（PLAN §4.4・§11）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 複数機種の認識結果の文字列が記録され、テストケースへ反映する依頼が出ている
  依存: []
- id: BL-009
  区分: 人手検証
  タスク内容: GitHub リポジトリの設定（main 保護と PR 必須、Secret scanning と Push protection の有効化、Private vulnerability reporting の有効化、Issue テンプレートが使うラベル「type 要望／不具合／質問」「from フォーム／レビュー／GitHub」「status 検討中／対応予定／見送り」の作成）（PLAN §7・§7.3）
  優先度: P3
  状態: 未着手
  担当: ユーザー
  完了条件: 各設定が GitHub 上で有効になり、.github/ISSUE_TEMPLATE/ のラベル（type：不具合 等。表記は半角コロンと空白を含むテンプレートの記載どおり）が存在する
  依存: []
  根拠: GitHub 上の設定変更は外部操作でエージェントが実行できないため。CI（GitHub Actions）と Dependabot はオーナー指示により sesami-wear に合わせて導入しない（2026-10-24）
- id: BL-008
  区分: 人手検証
  タスク内容: M0 の雛形を実機へインストールし、起動・アプリ名「いくメモ（debug）」・画面端までの表示を確認する
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 実機でデバッグ版が起動し、Play 版と別のアプリとして並ぶ
  依存: []
```
<!-- COPILOT_RECORDS:END -->
