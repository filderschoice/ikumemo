<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用未対応事項記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.backlog.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
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
- id: BL-122
  区分: 品質ゲート
  タスク内容: >-
    .claude/settings.json に、ループで繰り返す読み取り・検証のコマンドの許可と、禁止操作の拒否を足す提案。
    案は permissions.allow へ ./gradlew の品質ゲート（ktlintCheck・detekt・lintDebug・testDebugUnitTest・:core:test・assembleDebug・--stop）、
    python scripts/validate-records.py、python scripts/make-user-guide.py --check、python scripts/quality-gates.py（BL-119）、npx markdownlint-cli2、git status・git diff・git log を足し、
    permissions.deny へ git push（全形）と --no-verify を含む git commit を足す。あわせて PreToolUse フックで git push と --no-verify を止めるかを決める
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: オーナーが許可・拒否の一覧とフックの採否を決め、採用分が .claude/settings.json に反映され、反映後のループで権限の確認が出ないことを確かめている
  依存: []
  根拠: >-
    2026-11-13 にオーナーが、上の許可と git push・--no-verify の拒否を入れること、フックでもこの 2 つを止めること、
    許可の候補に python scripts/scan-secrets.py と python scripts/quality-gates.py を含めることを承認した。
    オーナーへの質問は「上の許可と拒否を .claude/settings.json へ入れてよいか、git push と --no-verify をフックでも止めるか」。
    権限の変更は .github/copilot-instructions.md「確認が必要な場合」の高リスク操作（権限変更）に当たるため要確認とした。
    現在の .claude/settings.json は記録ファイル 3 つの Edit と docs/records 配下の Read だけを許可し、hooks は無い。禁止操作（guardrails 12.2）は文書の規定だけで、仕組みでは止めていない
- id: BL-120
  区分: 品質ゲート
  タスク内容: >-
    scripts/validate-records.py の検査を FORMAT.md の定めに合わせて足す。
    (1) BACKLOG.md の id の重複、(2) 区分 が FORMAT.md の表の 6 値（不具合・UX改善・機能追加・性能・品質ゲート・人手検証）のどれか、
    (3) 担当 が空でない、(4) EXECUTE.md の必須キー（date・summary・details の 変更内容・変更ファイル（配列）・検証コマンド・検証結果）と date の形式 YYYY-MM-DD HH:mm、
    (5) BACKLOG.md の id が EXECUTE.md の 関連ID で完了済みとして使われた番号と重ならない（採番の衝突の検出）。
    既存の記録で新しい検査に落ちるものがあれば、マーカー内の該当項目を FORMAT.md に沿って直す（直せないものは根拠付きで報告する）。
    モジュールの先頭の docstring の「検査する内容」の一覧も更新する
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    python scripts/validate-records.py が今の記録で成功し、わざと壊した一時コピー（重複 id・区分の誤り・date の形式違い）で失敗することを確かめている
    （一時コピーはスクラッチの場所に作り、コミットしない）。markdownlint が Summary 0 issues
  依存: []
  根拠: >-
    scripts/validate-records.py は YAML の読み込み・LF・BACKLOG の必須キーと優先度・状態・依存だけを検査し（94〜110 行目の main）、EXECUTE.md の必須キーは見ていない。
    FORMAT.md は区分の許容値（6 値）と EXECUTE の必須キーを定めている。採番の衝突確認は毎回手作業（今回も BL-112 が EXECUTE.md にだけ残っていた）
- id: BL-118
  区分: 人手検証
  タスク内容: >-
    実機で BL-113・BL-115 の結果を確かめる。写真の「選ぶ」で選んだ直後に元の写真をギャラリーから消す・クラウドの写真を選ぶなどで読めない写真を渡しても落ちないこと、
    TalkBack でカレンダーの日が「選択中」と読まれること、困ったときの質問が開閉の状態と一緒に読まれること、おしらせの行を押す操作が「期限を決め直す」と読まれること
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 上の 4 点を実機で確かめ、読まれ方が不自然な箇所があれば画面名と読まれた文言を記録している
  依存:
    - BL-115
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
  依存: []
  根拠: >-
    2026-11-13 にオーナーが、カレンダーの検索欄に音声入力を付けることを承認した。
    オーナーへの質問は「カレンダーの検索欄に音声入力（マイク）を付けてよいか」。DESIGN.md の F-42 の行が「検索欄の音声入力は未実装」と明記しており、
    部品（FieldMic）は EditItemSheet.kt 118 行目で既に使われているため小さく足せるが、画面に新しい操作を足す製品判断のため要確認とした
- id: BL-116
  区分: 品質ゲート
  タスク内容: >-
    カレンダーとおしらせの画面に、スクリーンショット以外の Compose のテスト（Robolectric）を足す。
    mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt と reminder/ReminderContentTest.kt を新設し、
    公開されている CalendarContent（calendar/CalendarScreen.kt 81 行目）と ReminderContent（reminder/ReminderScreen.kt 91 行目）に状態を渡して、
    日を押すと日を選ぶイベント（CalendarEvent の該当するもの）が出る、買い物の無い日の文言、検索結果を押すとその日へ移るイベント、おしらせの「使い切った」「捨てた」のイベント、
    通知オフの帯（ReminderTestTags.NOTIFICATIONS_OFF）の表示を確かめる。既存の settings/DataContentTest.kt・shoppinglist/ShoppingListScreenTest.kt の書き方に合わせる。本体のコードは変えない
  優先度: P3
  状態: 未着手
  担当: AIエージェント
  完了条件: 新しい 2 つのテストクラスが ./gradlew testDebugUnitTest で通り、CLAUDE.md の Gradle 系の 5 ゲートがすべて成功する
  依存:
    - BL-115
  根拠: >-
    mobile/src/testDebug/ の Compose のテストは guide・help・settings・shoppinglist・voice・designsystem にあるが、calendar と reminder の画面は
    MainScreensScreenshotTest.kt（遅いテスト扱いで通常は流さない）にしか無く、通常の品質ゲートでは画面の操作が確かめられていない。
    BL-115 が同じ画面の意味づけ（semantics）を変えるため、その後に書く
- id: BL-115
  区分: UX改善
  タスク内容: >-
    読み上げ（TalkBack）で状態と操作の意味が伝わらない 3 か所を補う。見た目は変えない。
    (1) calendar/MonthGrid.kt の DayCell（147 行目付近）に選ばれている日の状態（Modifier.semantics の selected、または selectable）を付ける、
    (2) settings/FeedbackScreen.kt の困ったときの質問（72 行目付近の clickable）に開いている・閉じているの状態（stateDescription）と押したときの意味（onClickLabel）を付ける、
    (3) reminder/ReminderScreen.kt の PendingRow（234 行目付近の clickable）に onClickLabel「期限を決め直す」を付ける。
    文言は mobile/src/main/res/values/strings.xml に足し、直書きしない。testDebug の shoppinglist/AccessibilityTest.kt にこの 3 点の検査を足す
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: >-
    AccessibilityTest に足した検査が通り、CLAUDE.md の Gradle 系の 5 ゲートがすべて成功する。スクリーンショットテストの calendar と reminder を画面ごとに比べて差分が無い。
    DESIGN.md の「PLAN §8 アクセシビリティ」の行に足した内容を反映している
  依存: []
  根拠: >-
    MonthGrid.kt の DayCell は選択中を背景色（secondaryContainer）だけで示し、semantics には contentDescription しか付けていない（160〜175 行目）。
    FeedbackScreen.kt の質問の開閉は opened の状態で答えを出し入れするだけで状態の説明が無い。ReminderScreen.kt の PendingRow の clickable は onClickLabel が無く「ダブルタップで有効にします」としか読まれない。
    リポジトリで selectable や stateDescription を使っているのは settings/AppearanceScreen.kt だけ（grep で確認）
- id: BL-114
  区分: 不具合
  タスク内容: >-
    データの書き出し・読み込み（settings/DataViewModel.kt）で、IOException 以外の例外でアプリが落ちないようにし、DataViewModel の単体テストを新設する。
    ContentResolver.openOutputStream・openInputStream が投げうる SecurityException と IllegalArgumentException を、それぞれ EXPORT_FAILED・IMPORT_FAILED に対応させる。
    mobile/src/test/kotlin/com/ikumemo/mobile/settings/DataViewModelTest.kt を新設し、(1) 処理中（busy）に重ねて押しても 2 回目を無視する、(2) openOutput が null・例外のときに EXPORT_FAILED、
    (3) openInput が null・例外のときに IMPORT_FAILED、(4) 成功時の EXPORTED・IMPORTED・ERASED を確かめる（DataEraser・BackupExporter・BackupImporter は既存のテストの TestDatabase を使うか偽物にする）
  優先度: P2
  状態: 未着手
  担当: AIエージェント
  完了条件: DataViewModelTest が通り、CLAUDE.md の Gradle 系の 5 ゲートがすべて成功する
  依存: []
  根拠: >-
    DataViewModel.kt の export・import は IOException だけを捕まえている（87・99 行目）。開き方は AppViewModelFactory.kt 63〜64 行目の resolver.openOutputStream・openInputStream で、
    Android の ContentResolver はファイルの選択元のアプリや権限の取り消しによって SecurityException を投げうる。viewModelScope の中で捕まえない例外はアプリを落とす。
    DataViewModel を参照するテストは mobile/src/test と testDebug のどちらにも無い（grep で確認）
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
    写真の上に直接置かれた文字が読めるか、しろ（暗い膜）でテーマの面（行・バー）の中の文字が読めなくならないかを見る
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 各ペンの色で、全画面の文字が読める。読めない箇所があれば画面名と色を記録している
  依存: []
  根拠: 写真の明るさと各画面の文字の置かれ方の組み合わせは実機でしか確かめられないため。単体テストでは文字と膜の色の比（4.5:1 以上）だけを見ている
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
  タスク内容: ユーザビリティテスト（対象ペルソナ 3〜5 人、説明なしで「話して追加 → 買った → カレンダーで確認」）（PLAN §10 M8・§11）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: 主要タスクが説明なしで完了し、ガイドで詰まらないことを観察で確認している
  依存: []
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
  タスク内容: おしらせの実機確認（メーカー独自の省電力、再起動後、電源オフ後の R-28、通知のボタン操作）
  優先度: P2
  状態: 未着手
  担当: ユーザー
  完了条件: Pixel と他メーカー端末で期限前日の通知が届き、再起動・電源オフ後も漏れない
  依存: []
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
