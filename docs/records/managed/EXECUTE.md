<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- date: 2026-11-15 21:11
  summary: ご意見・ご要望の画面にご意見フォームで送るボタンを足し、GitHub を従にした
  details:
    関連ID:
      - BL-206
    変更内容: >-
      ご意見・ご要望の画面で、フォームで送るボタンを先に目立つ形（Button）で置き、GitHub で送るは下の OutlinedButton にした。画面の説明文はフォームと GitHub それぞれに合う文へ分けた。
      ボタンは転送ページ https://filderschoice.github.io/ikumemo/feedback/ にクエリ info（版・Android の版・機種名）を Uri.Builder でエンコードして付け、外部のブラウザで開く。
      URL の組み立ては feedbackFormUrl に分けた。Google フォームの URL と entry 番号はアプリに書かず、INTERNET 権限も足していない。
      使い方（help/feedback.md）・docs/USER_GUIDE.md・SUPPORT.md・FEEDBACK.md を新しいボタンに合わせた。公開前は 404 になる（BL-207 で公開）。
      見た目が変わったため、スクリーンショットテストにご意見画面があれば記録し直しが要る（本タスクでは記録し直していない）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ExternalLink.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/FeedbackScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/feedback.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/ExternalLinkTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AboutContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/AccessibilityTest.kt
      - docs/USER_GUIDE.md
      - docs/SUPPORT.md
      - docs/FEEDBACK.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests AboutContentTest --tests AccessibilityTest --tests ExternalLinkTest
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定 3 クラスのテスト・markdownlint・記録ファイル・使い方の Web 版・秘密情報スキャンが成功。Android Lint・全テスト・assembleDebug・スクリーンショットテストは流していない（軽い検証）
- date: 2026-11-15 21:10
  summary: アプリ紹介ページ（site/）とご意見フォームへの転送ページ（site/feedback/）を作った
  details:
    関連ID:
      - BL-086
    変更内容: >-
      sesami-wear の site/ を雛形に、HTML と CSS だけの紹介ページ（文言は STORE_LISTING.md と PLAN §1.2 から。ストアのリンクは「準備中」）と app.json・仮アイコン・README を作った。
      転送ページはクエリ info を decode 済みの値から encodeURIComponent で作り直し、フォームの entry.10635078 に付けて location.replace で移る。
      info が無ければ事前入力なしのフォームへ移り、JavaScript が無い場合はリンクから開ける。フォームの URL と entry 番号は script 先頭の定数 2 つに集約し、
      info はページに表示せず URL の組み立てにだけ使う。アクセス解析・外部スクリプト・外部フォント・Cookie は使っていない。
      公開手順（git subtree push と Pages の設定）とフォームの差し替え方を site/README.md に、転送ページの説明を docs/FEEDBACK.md に書いた。
      ブラウザでの動作確認と公開は BL-207（人手検証）
    変更ファイル:
      - site/index.html
      - site/feedback/index.html
      - site/style.css
      - site/app.json
      - site/assets/icon.svg
      - site/.nojekyll
      - site/README.md
      - docs/FEEDBACK.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --skip-gradle
    検証結果: >-
      成功 - markdownlint・記録ファイル・使い方の Web 版・秘密情報スキャンが成功（Gradle 系は対象外）。ブラウザでの表示と転送の確認は流していない（BL-207）
- date: 2026-11-15 17:46
  summary: BL-205 の点検の指摘を直し、古いコメントを実装に合わせ、短い表記の境界値テストと 350dp の根拠を残した
  details:
    関連ID:
      - BL-205
    変更内容: >-
      MemoListWidget.kt の古いコメント（COMPACT_BELOW 未満だけを 2 マスとみなす等）を compactHeader の判定を参照する書き方へ直した（挙動は変えていない）。
      単体テストに、compactHeader の境界値（149・150・349・350dp × 期限が近い件の有無）と、高さ 180dp の候補（NARROW_LARGE・LARGE）で期限が近い件があると「🔔2」になるテストを足し、
      入力を WIDE_SMALL・WIDE_LARGE に替えた 2 つのテストに 4 マス相当と分かるコメントを足した。
      350dp の境目は変えない。根拠は、4 マスが 350dp 未満の端末では全文がもともと収まらず「期限が近い…」で数字が切れるため、「🔔N」にする方が件数が見えること
      （実機 Pixel 8 Pro では 3 マス約 300dp で全文が切れ、4 マス約 410dp で収まった）。この限界を DESIGN.md の W-02 行と BL-106 の残りに書いた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。Android Lint・全テスト・assembleDebug・実機確認は流していない
- date: 2026-11-15 17:40
  summary: メモ一覧ウィジェットで、期限が近い件があるとき幅 2・3 マスでも期限が近い件数が見えるようにした
  details:
    関連ID:
      - BL-205
    変更内容: >-
      実機で幅 2 マス（約 205dp）は NARROW（180dp）が選ばれて短い表記に入らず、幅 3 マス（約 300dp）は SMALL（250dp）で件数の数字が切れていた。
      Responsive の候補へ幅 350dp の WIDE_SMALL・WIDE_LARGE を足して幅 4 マス（約 410dp）と区別し、見出しを短い表記にする判定を
      compactHeader(width, dueSoon) にした。幅 150dp 未満は従来どおり常に短く、期限が近い件があるときは幅 350dp 未満も「🔔N」にする（メモの件数は省く）。
      期限が近い件が無いときの表示と幅 4 マスの全文は変えていない。DESIGN.md の W-02 行を実装に合わせて直した。
      単体テストで、幅 180・200・250dp で「🔔2」と更新ボタンが残ること、350dp で全文、期限が近い件が無いと 180・250dp は全文のままを確かめた。
      スクリーンショットテストは流していない（ウィジェットは対象外とみられるが未確認）。実機確認は BL-106 の残りに追記した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。Android Lint・全テスト・assembleDebug・スクリーンショットテスト・実機確認は流していない
- date: 2026-11-15 17:33
  summary: 暗いテーマで背景の写真を置いたおまかせ（ペンの色なし）のとき、写真の上に直接置いた文字が黒になる不具合を直した
  details:
    関連ID:
      - BL-204
    変更内容: >-
      AppThemeHost.kt の写真ありの分岐で、LocalContentColor を ink が null のとき onBackground にした（resolvePhotoInk を再利用）。
      従来は既定の黒のままで、暗い膜の上の案内文・お店チップ・設定の項目名が読めなかった。
      面の中の文字は ReadablePanel などが onSurface を明示しているため変わらず、ペンの色ありの見え方も変えていない。
      明るいテーマ × おまかせでも onBackground になる（明るいテーマの onBackground は暗い色のため、見え方の差は黒から暗い色への微差。実機確認は BL-199 の残りで人手検証）。
      テストは、全テーマ・明暗で onBackground が background に対し 4.5:1 以上であり、おまかせでは onBackground が選ばれることを足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/PhotoInkTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*PhotoInkTest" --tests "*PhotoTextButtonColorsTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。Android Lint・全テスト・assembleDebug・スクリーンショットテスト・実機確認は流していない
- date: 2026-11-15 15:45
  summary: 幅 3 マスのメモ一覧ウィジェットを従来の全文の見出しに戻し、短い表記は幅 2 マス相当だけにした（BL-203 の点検の指摘）
  details:
    関連ID:
      - BL-203
      - BL-202
    変更内容: >-
      BL-203 の短い表記の判定が wide（幅 250dp 以上）の否定だったため、Responsive の候補が 180 と 250dp の 2 段では約 180〜249dp（幅 3 マス）も短い表記になっていた。
      2026-11-14 の実機（Pixel 8 Pro・Nova）では 3×2 で全文が収まっていたため、Responsive の候補へ幅 120dp（TWO_CELL_SMALL・TWO_CELL_LARGE）を足し、
      見出しの短い表記は LocalSize の幅が 150dp 未満（COMPACT_BELOW）のときだけにした。
      根拠は、ランチャーの一般的な寸法で 2 マスが約 110〜140dp、3 マスが約 180dp 前後であること（2 マスの実寸は未確認のため、中間の 150dp を閾値にした。実機確認は BL-106）。
      行の並べ方・品名の数・数量の位置は従来の wide（250dp 以上）の判定のままで変えていない。
      テストは、幅 2 マス相当で期限が近いときに「🛒」が出ないこと、幅 180・200・250dp で全文（「🛒メモ 5件」「🔔期限が近い 2件」）が出て「🔔2」が出ないことを足した。
      BL-202 の記録は変えていない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。memo のスクリーンショットの verify は差分なし（61719a8 の後、オーケストレータが実施）。Android Lint・全テスト・assembleDebug・実機確認は流していない
- date: 2026-11-15 15:38
  summary: 幅 2 マスのメモ一覧ウィジェットで、期限が近い件数を「🔔N」と短くして見えるようにした
  details:
    関連ID:
      - BL-203
    変更内容: >-
      MemoListWidget.kt の MemoHeader に wide（LocalSize の幅が SMALL 以上か）を渡し、狭いときは期限が近い件数を「🔔N」、メモの件数を「🛒N」と短くした。
      幅が狭く期限が近い件があるときは、メモの件数を省いて「🔔N」を優先する（既定値。件数は下の品名の並びでも分かるため）。
      期限が近い件の欄は contentDescription で「期限が近い N件」、メモの件数は「メモ N件」と読み上げる。更新ボタンは残したまま。
      幅の広い表示の文言は変えていない。単体テストで狭いサイズの表記と読み上げを確かめた。実機確認は BL-106 に残した。
      ウィジェットは Roborazzi の対象外のため、記録し直す正解画像は無い
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。Android Lint・全テスト・assembleDebug・実機確認は流していない
- date: 2026-11-15 15:28
  summary: 背景の写真とペンの色があるとき、メモが空のときの「使い方を見る」をペンの色で描くようにした
  details:
    関連ID:
      - BL-202
    変更内容: >-
      ShoppingListScreen.kt の EmptyMemo の使い方ボタン（EMPTY_HELP）と、音声入力の無い端末向けの使い方ボタン（NO_VOICE_HELP）に
      colors = photoTextButtonColors() を付けた（BL-188 で「文字で追加」だけに付けて漏れていた）。
      写真とペンの色があるときはペンの色、無いときは既定の文字ボタンの色になることを PhotoTextButtonColorsTest で確かめた。
      DESIGN.md の F-61 に使い方ボタンも加えた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/designsystem/PhotoTextButtonColorsTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*PhotoTextButtonColorsTest" --tests "*PhotoTextButtonContrastTest" --tests "*ShoppingListScreenTest"
    検証結果: >-
      成功 - ktlint・detekt・コンパイル・指定テストと文書系ゲートが成功。Android Lint・全テスト・assembleDebug・スクリーンショットは流していない
- date: 2026-11-15 14:37
  summary: 点検の指摘に合わせ、使い方の膜の濃さの説明と DESIGN の F-61 を BL-188・189 の実装に揃えた
  details:
    関連ID:
      - BL-188
      - BL-189
    変更内容: >-
      BL-189 で明るい画面の膜も選んだ濃さより 0.1 濃くしたのに、使い方（背景の写真）が「暗い画面では自動で濃くなります」のままだったため、
      「実際の膜は、写真の上の文字が読めるよう選んだ濃さより少し濃くなります（暗い画面ではさらに濃くなります）」に直し、docs/USER_GUIDE.md を作り直した。
      DESIGN.md の F-61 に、写真の上の「文字で追加」をペンの色で描くこと（BL-188、photoTextButtonColors）を足した。
      BL-199 に、明るいテーマの膜の +0.1 がおまかせ・くろ・しろにも掛かるため、おまかせ・くろでも写真が透けて見えることの確認点を足した
    変更ファイル:
      - mobile/src/main/assets/help/background.md
      - docs/USER_GUIDE.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/make-user-guide.py の後に python scripts/quality-gates.py --skip-gradle
    検証結果: >-
      成功 - 文書系のゲート（markdownlint・記録・使い方の Web 版・秘密情報）が成功。Gradle 系は文言だけの変更のため流していない
- date: 2026-11-15 14:28
  summary: メモ画面のスクリーンショット（24 枚）を、BL-194・196・200・201 で意図して変えた見た目に合わせて記録し直した
  details:
    関連ID:
      - BL-194
      - BL-196
      - BL-200
      - BL-201
    変更内容: >-
      メモ画面の見出し行に「えらんで消す」（BL-194）、お店チップの行の右端に送るアイコン（BL-196）が加わったため、memo の正解画像 24 枚を記録し直した。
      比べる過程で、お店チップが標準の文字でも 2 行に折り返す回帰（BL-200 で修正）と、文字が最大のとき見出しが折り返す回帰（BL-201 で修正）を見つけ、直してから記録した。
      記録後の差分は「えらんで消す」と送るアイコンだけで、見出しとお店チップは標準・大きい・とても大きいのいずれも 1 行に収まることを画像で確かめた。
      BL-187〜189 は背景の写真があるときだけ見た目が変わり、スクリーンショットのテストは写真なしのため、calendar・reminder は比べるだけで一致した
    変更ファイル:
      - mobile/screenshots/memo/（24 枚）
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true --tests "*MainScreensScreenshotTest.memo*" の後、
      -Proborazzi.test.verify=true で memo・calendar・reminder を 1 画面ずつ
    検証結果: >-
      成功 - 記録し直した memo と、比べるだけの calendar・reminder がいずれも終了コード 0
- date: 2026-11-15 14:23
  summary: 文字が「とても大きい」のとき、見出し「買い物メモ」が「買い物メ／モ」と 2 行に折り返す回帰を、「えらんで消す」の幅に上限を置いて直した
  details:
    関連ID:
      - BL-201
    変更内容: >-
      BL-194 で見出し行へ「えらんで消す」の文字ボタンを足した後、文字が最大のとき、ボタンが文字の幅いっぱいまで取って見出しの幅が足りなくなり、見出しが折り返していた。
      SelectStartButton の幅に上限 128dp を置き、横の余白を 12dp から 8dp に減らし、文字を 2 行まで折り返せるようにした（高さの下限 48dp は維持、設定ボタンは触らない）。
      案の根拠 1 は、見出しを 1 行に固定して縮めると文字が欠けるため、押せる範囲を保てて文字が欠けない「ボタン側を折り返す」を選んだ。
      根拠 2 は、標準の文字ではボタンの文字が約 84dp で上限の内側に収まり、見た目が変わらないこと。大きいの文字も収まる見込みだが未確認（スクリーンショットで確かめる）。
      既存の SelectModeScreenTest が通ることを確かめた。メモ画面のスクリーンショット（特に cool_dark_extra_large）の確認・記録し直しはオーケストレータが行う。実機での見え方は BL-198 に確認点を足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SelectionEvent.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SelectModeScreenTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。スクリーンショットのテストは未実施
- date: 2026-11-15 14:17
  summary: メモ画面で、送るアイコンを足した後に 1 行で収まる店名のお店チップが 2 行へ折り返す回帰を直した
  details:
    関連ID:
      - BL-200
    変更内容: >-
      BL-196 でお店チップの行へ送るアイコンを足したとき、fill=false の重み付きチップと重み付きの Spacer が余りの幅を半分ずつ分け、
      行に空きがあっても店名が「○○スーパ／ー で買う」と折り返していた。チップを重み 1 の Box（左寄せ）で包み、アイコン（重み無し）を先に測らせて残りの幅をすべて Box へ渡す形にした。
      チップは Box の中で中身の幅になり、アイコンは右端に残る。文字「とても大きい」で幅が足りないときの折り返しは従来どおり。
      既存の ShareScreenTest・ShoppingListScreenTest が通ることを確かめた。見た目は変わる（チップが 1 行に戻る）ため、メモ画面のスクリーンショットの記録し直しはオーケストレータが行う。
      実機での見え方（大きい文字でチップとアイコンが重ならないか）は BL-198 に既に確認点がある
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShareButton.kt
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ShareScreenTest" --tests "*ShoppingListScreenTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。スクリーンショットのテストは未実施
- date: 2026-11-15 14:10
  summary: 暗い写真の上で、ペンの色あか・あおの文字が背景の灰色に埋もれないよう、あかを暗い赤にし、明るいテーマの膜を少し濃くした
  details:
    関連ID:
      - BL-189
    変更内容: >-
      InkColor.RED の文字色を #B3261E から #851812 に変えた（青 #1A3FA0 は据え置き）。BackgroundScrim に LIGHT_BOOST（0.1）を足し、明るいテーマでも膜の実際の濃さを選んだ濃さ＋0.1
      にした（既定 0.6 は 0.7。上限 MAX_EFFECTIVE 0.95 は共通）。暗いテーマは従来どおり 0.15 を足す。単体テスト InkOnPhotoContrastTest を足し、暗い写真（灰色 0.1）と明るい写真（0.9）、
      明るい・暗いテーマ、膜の濃さ（既定・最大）、くろ・あお・あか・しろの組み合わせで、文字と膜を重ねた背景の比が 4.5 対 1 以上であることを確かめた（しろ＋明るい写真は従来から対象外）。
      PhotoTextButtonContrastTest のあかの除外を外し、BL-187 の SystemBarIconsTest・BL-188・BL-108 の InkColorTest・PhotoInkTest も通ることを確かめた。AppearanceViewModelTest の明るいテーマの膜の期待値を 0.7 に直した。
      膜の濃さのスライダーが最小（0.3）で暗い写真のときは 4.5 対 1 を保証しない（既定と最大が対象）。実機の確認は BL-199 に足した。
      見た目が変わるのは写真を置いた画面（明るいテーマの膜が濃くなる、あかの文字が暗くなる）。スクリーンショットのテストは写真なしの見込みで、記録し直しは要らない見込み（流していない）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/BackgroundScrim.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/InkColor.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/InkOnPhotoContrastTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/PhotoTextButtonContrastTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*InkOnPhotoContrastTest" --tests "*PhotoTextButtonContrastTest" --tests "*SystemBarIconsTest" --tests "*InkColorTest" --tests "*PhotoInkTest" --tests "*AppearanceViewModelTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。実機の確認は未実施（BL-199）
- date: 2026-11-15 14:03
  summary: 暗いテーマで背景の写真を置いたとき、メモ画面の「文字で追加」の文字色をペンの色にして読めるようにした
  details:
    関連ID:
      - BL-188
    変更内容: >-
      ShoppingListScreen の「文字で追加」の文字ボタンに photoTextButtonColors() を指定した。固定のテーマ色（暗いテーマでは薄い青の primary）が、ペンの色くろ・あおの明るい膜に溶けていたため、
      写真の上ではペンの色（暗い色）にする。写真が無いとき・おまかせ（ペンの色なし）は従来どおりテーマ色。
      単体テスト PhotoTextButtonContrastTest で、暗いテーマの全プリセット、ペンの色（おまかせ・くろ・あお・しろ）、膜の濃さ（既定・最大）、明るい写真と暗い写真（灰色 0.9 と 0.1 を仮定、
      BL-187 の SystemBarIcons.backgroundLuminance で膜を重ねた背景の明るさを求める）のすべてで、文字と背景の比が 4.5 対 1 以上になることを確かめた。
      あか（暗い写真・既定の濃さで約 3.7 対 1）は完了条件の外で、BL-189 の対象。実機の確認は BL-199 に確認点として足した。
      見た目はメモ画面の文字ボタンの色だけ（写真ありの組み合わせ）で、スクリーンショットのテストは写真なしのため記録し直しは要らない見込み（流していない）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/PhotoTextButtonContrastTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*PhotoTextButtonContrastTest" --tests "*PhotoInkTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。実機の確認は未実施（BL-199）
- date: 2026-11-15 14:15
  summary: 背景の写真の膜が明るいとき、状態バーとナビゲーションバーのアイコンを暗い色にして読めるようにした
  details:
    関連ID:
      - BL-187
    変更内容: >-
      designsystem/SystemBarIcons.kt を足した。純粋な関数で、膜の色と濃さ（BackgroundScrim.effectiveAlpha）を中間の灰色（0.5）の写真の上に重ねた背景の相対輝度を求め、
      0.179（白と黒のアイコンのコントラストが等しくなる輝度）より明るければ暗いアイコンにする。写真が無いときはアプリのテーマに合わせる（暗いテーマは白、明るいテーマは暗い色）。
      SystemBarIconsEffect（SideEffect）が WindowCompat の insets controller で状態バー・ナビゲーションバーのアイコンの明暗を設定し、AppThemeHost が showBackground のときだけ呼ぶ
      （他のアプリの上に出す音声入力シートは触らない）。これで端末のダークモードとアプリのテーマが食い違うときも読める。
      単体テスト SystemBarIconsTest で、明るい膜（くろ・あお・あか、明るい・暗いテーマ）は暗いアイコン、暗い膜（しろ）は白、おまかせはテーマの背景に従うこと、膜が最も薄いときの判定を確かめた。
      写真の中身は読まないため、極端な写真では外れうる。実機での確認は BL-199（人手検証）へ起票した。スクリーンショットは流していない（アイコンの色は Robolectric の画像に写らない見込み。記録し直しが要る画面は無い見込み）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/SystemBarIcons.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/SystemBarIconsTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SystemBarIconsTest" --tests "*InkColorTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。実機の確認は未実施（BL-199）
- date: 2026-11-15 13:55
  summary: メモ一覧ウィジェットを幅 2 マスまで縮めても更新ボタンが欠けないよう、期限が近い件数の見出しだけを縮める作りにした
  details:
    関連ID:
      - BL-186
    変更内容: >-
      見出しの行で、期限が近い件数の箱に重み（defaultWeight）を付け、残りの幅を受け持たせた。幅が足りないときは、この文字だけが省略され、
      右端の更新ボタン（固定の 48dp）は常に残る。期限が近い件数が無いときは従来どおり空きの Spacer に重みを付ける。3 マス以上では収まるため見た目は変わらない。
      単体テストに、幅 2 マス相当（110dp）で更新ボタンと期限が近い件数の見出しが両方あることを足した（Glance の単体テストは文字の省略まで測れないため、実機での確認は BL-106 に確認点として足した）。
      見た目はウィジェットだけで、メモ・記録・期限の画面は変えていないためスクリーンショットの記録し直しは要らない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。実機での幅 2 マスの確認は未実施（BL-106）
- date: 2026-11-15 13:05
  summary: LINE などで共有された文を共有シートの「いくメモに追加」から受け取り、チェック付きの一覧で確かめてメモへ取り込めるようにした
  details:
    関連ID:
      - BL-197
    変更内容: >-
      core に SharedTextParser を足した。先頭行が「🛒いくメモ」の文は行頭「・」の行を 1 行 1 品物として、最後の半角空白の後ろが数量らしいときだけ数量に戻す（BL-196 の送る文が元の品物と数量に戻る）。
      ほかの行は行頭の記号（・ - * • ☐ 1. (1) ① など、1.5L は外さない）を外し、品名と数字の間の空白を詰めてから VoiceTextParser の区切りと数量の取り出しを使う（小数点では分けない）。正規化した品名が同じ物は最初の 1 つだけ残す。
      受け取る文は信頼できない外部入力として、1 万文字・200 行・100 品物・品名 60 文字・数量 20 文字で打ち切り（サロゲートペアの途中では切らない）、制御文字と向きを変える書式文字を除く。
      ShareReceiveActivity（exported、ACTION_SEND の text/plain だけを受け付け、ほかの Intent・extra は読まずに閉じる。壊れた extra は空として扱う。権限は足さない）と、
      ShareImportViewModel・ShareImportSheet（チェック付きの一覧、既にメモにある物は最初から外す、追加後は Toast で閉じる、品物が無ければその旨を出す）を足した。受け取った文はログに出さない。LINE の SDK・外部通信は使わない。
      ShareText の HEADER・BULLET は SharedTextParser の定数を参照するようにした。テスト用の待ち合わせ（ComposeWait）の受け手を ComposeTestRule へ広げた（ActivityScenario で使うため。既存の呼び出しはそのまま）。
      使い方（send.md の見出しを「メモを LINE などで送る・取り込む」にして取り込みの説明を足した、index.md、USER_GUIDE.md）と DESIGN.md を反映。実機で LINE から取り込む確認は BL-198（人手検証）へ足した。
      共有から開く新しいシートでメモ・記録・期限の画面は変えていないため、スクリーンショットの記録し直しは要らない見込み（流していない）
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/share/SharedTextParser.kt
      - core/src/test/kotlin/com/ikumemo/core/share/SharedTextParserTest.kt
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/share/ShareText.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/share/ShareReceiveActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/share/ShareImportViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/share/ShareImportSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/send.md
      - mobile/src/main/assets/help/index.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/share/ShareImportViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/share/ShareImportContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/share/ShareReceiveActivityTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/testing/ComposeWait.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SharedTextParserTest" --tests "*ShareImportViewModelTest" --tests "*ShareImportContentTest" --tests "*ShareReceiveActivityTest" --tests "*ShareTextTest" --tests "*HelpContentTest" --tests "*VoiceSheetActivityTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・指定したテスト）と文書系ゲートが成功。フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。実機は未確認で BL-198 に足した
- date: 2026-11-15 12:40
  summary: メモを LINE などへ送る「メモを送る」を足し、共有シートで一覧を文字にして送れるようにした
  details:
    関連ID:
      - BL-196
    変更内容: >-
      お店チップの行の右端に送るアイコン（読み上げは「メモを送る」）を足した。置き場所は、上の帯が「買い物メモ」「えらんで消す」「設定」で大きい文字では窮屈になるため帯を避けた。
      普段のモードで品物があるときだけ出し、空のメモ・えらんで消すモードの間は出さない。押すと ShareText.build で「🛒いくメモ」の下に「・品物 数量」を 1 行ずつ並べた文を作り、ACTION_SEND（text/plain）の chooser を開く。
      取り込み（BL-197）が先頭行と行頭の「・」で元の品物と数量に戻せる形にした（品物・数量内の改行は空白に直す）。LINE の SDK・外部通信は使わない。開ける物が無い端末は Toast で知らせる。
      使い方（send.md を新設、index.md、USER_GUIDE.md）と DESIGN.md を反映。実機で LINE へ送る確認は BL-198（人手検証）に起票した。
      見た目が変わるため、スクリーンショットはメモ画面（memo）の記録し直しが要る見込み（このイテレーションでは流さず、区切りでまとめて扱う。calendar・reminder は変わらない見込み）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/share/ShareText.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShareButton.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/send.md
      - mobile/src/main/assets/help/index.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/share/ShareTextTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShareScreenTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ShareTextTest" --tests "*ShareScreenTest" --tests "*SelectModeScreenTest" --tests "*ShoppingListScreenTest" --tests "*AccessibilityTest" --tests "*HelpContentTest"
    検証結果: >-
      成功 - 軽い検証（ktlint・detekt・コンパイル・ShareTextTest・ShareScreenTest・HelpContentTest）と文書系ゲートが成功。ShoppingListScreenTest・SelectModeScreenTest・AccessibilityTest は 1 回前の流しで成功（その後の変更は StoreRow の移動と文書のみ）。フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。実機は未確認で BL-198 に起票
- date: 2026-11-15 12:26
  summary: メモ画面に「えらんで消す」モードを足し、チェックを入れた品物をまとめて「やめた」にして、まとめて元に戻せるようにした
  details:
    関連ID:
      - BL-194
    変更内容: >-
      メモの上の帯の「えらんで消す」（メモが空でないとき）で各行にチェックが付く。行を押すとチェックの入れ外しで、編集シートは開かず、スワイプと ○ は止める（読み上げの操作の一覧も出さない）。
      帯は「N件えらんでいます」と「えらぶのを終える」、画面の下は「N件を消す」（0 件の間は押せない）に替わり、戻る操作でもモードだけを終える。
      ラベルは「やめた」と紛らわしい「やめる」を避けた（BL-133）。消すと ShoppingRepository.dismissAll（1 トランザクション）で選んだ項目をまとめて「やめた」にし、「やめた：N件」と元に戻すを出す。
      UndoOffer を複数の操作を持てる形（actions）にして、1 回の元に戻すで消した順の逆に全件を戻す。チェックは ShoppingListUiState.selection（DB に入れない）で、もう無い項目は数えない。
      操作は SelectionEvent（Start・Stop・Toggle）と DismissSelected。普段の左スワイプ（やめた）と BL-193 の確定条件は変えていない。使い方（swipe.md・undo.md、USER_GUIDE.md）と DESIGN.md を反映。
      見た目が変わるため、スクリーンショットはメモ画面（memo）の記録し直しが要る見込み（このイテレーションでは流さず、区切りでまとめて扱う。calendar・reminder は変わらない見込み）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SelectionEvent.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/MemoGuideController.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/swipe.md
      - mobile/src/main/assets/help/undo.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/SelectModeViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/SelectModeScreenTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SelectModeViewModelTest" --tests "*SelectModeScreenTest" --tests "*ShoppingListScreenTest" --tests "*ShoppingListViewModelTest" --tests "*ShoppingListGuideTest" --tests "*AccessibilityTest" --tests "*SwipeConfirmTest" --tests "*ShoppingRepositoryTest" --tests "*HelpContentTest"
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。実機は未確認で、BL-149 に確認点を足した）
- date: 2026-11-15 12:11
  summary: 話して追加の「追加して、つづけて話す」の後、聞き取り画面に追加した件数を出し、読み上げでも伝える
  details:
    関連ID:
      - BL-195
    変更内容: >-
      VoiceInputState.Listening に addedCount を足し、AddAllAndContinue で追加した件数を次の Listening へ渡す（Start では 0）。
      聞き取り画面は件数が 1 以上のとき「N件追加しました」を liveRegion（Polite）付きで出す。ウィジェットのシート（VoiceSheetActivity）も同じ VoiceInputSheet を使うため同じに出る。
      「追加する」で閉じたときの知らせ（Added、Toast とメモ画面）は変えていない。使い方（voice.md、USER_GUIDE.md）と DESIGN.md を反映。
      見た目が変わるため、スクリーンショットの記録し直しが要るか確認が必要（既存の正解画像は聞き取り画面を含まない見込みだが未確認）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/voice.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*VoiceInputViewModelTest" --tests "*VoiceInputContentTest" --tests "*VoiceInputSheetRepeatTest" --tests "*VoiceSheetActivityTest"
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。実機は未確認）
- date: 2026-11-15 11:05
  summary: メモの横スワイプを、速度だけでは確定せず幅の 40% 引いたときだけ確定にし、縦優勢は確定しない
  details:
    関連ID:
      - BL-193
    変更内容: >-
      オーナーの指示（縦スクロールしようとすると削除のスワイプが検知される）への対応 A。material3 1.3.1 の SwipeToDismissBox は速度のしきい値を変えられず、
      40% に届かなくても素早く払うと確定していた。確定の直前（confirmValueChange）に、行のずれが幅の 40% 以上かを確かめ、満たさなければ戻す。
      あわせて指の動きの合計を Initial パスで観察（イベントは消費せず、一覧の縦スクロールは妨げない）し、縦が横より大きければ確定しない。実装の既定値は、
      縦スクロールを奪わずに済む「確定の直前の判定」にした。確定の確認が続けて 2 回呼ばれるため、1 回の指の動きで 1 度だけ onBought・onDismissed を呼ぶ。
      右へ買った・左へやめた・○・元に戻す・振動は変えていない。見た目は変わらない。DESIGN.md の R-05 の行へ反映。
      C（えらんで消すモード）は BL-194 として起票。実機での誤操作の確認は BL-149 へ足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeGuard.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/SwipeConfirmTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SwipeConfirmTest" --tests "*ShoppingListScreenTest" --tests "*AccessibilityTest"、python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。見た目は変わらない。実機は未確認）
- date: 2026-11-15 10:45
  summary: 話して追加の確認カードに「追加して、つづけて話す」を足す（案 d）
  details:
    関連ID:
      - BL-191
    変更内容: >-
      オーナーが 2026-11-15 に案 (d) を選んだ（自動再開 (a) には進まない）。確認カードの「追加する」はそのまま残し、下に「追加して、つづけて話す」（48dp 以上）を足した。
      押すと VoiceInputEvent.AddAllAndContinue で確認中の品を追加し、Added を経ずに start() で Listening へ戻る。シートは閉じず、止め方は既存どおり（シートを閉じる・5 秒無音で NotHeard）。
      確認してから追加する原則（R-01・R-42）は保つ。確認カードの部品はウィジェットの VoiceSheetActivity と共用のため、ウィジェット経由でも同じに動き、追加後に Activity は閉じない（既定値で共用のまま）。
      使い方（help/voice.md）と DESIGN.md の R-30・R-31 の行へ反映。見た目が変わるため、確認カードが写るスクリーンショットは記録し直しが要る（本コミットでは流していない）。
      実機の連続確認は BL-192 へ足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceConfirmCards.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/voice.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*VoiceInputViewModelTest" --tests "*VoiceInputContentTest" --tests "*VoiceSheetActivityTest"、python scripts/make-user-guide.py、python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug・スクリーンショットは未実施。確認カードが写るスクリーンショットは無く、記録し直しは不要。実機は未確認）
- date: 2026-11-15 10:43
  summary: 話して追加を 1 回完了したあと続けて使えない不具合を直す（古い追加済みの状態が残る・recognizer の使い回し）
  details:
    関連ID:
      - BL-190
    変更内容: >-
      原因は、追加後の VoiceInputState.Added が Idle へ戻らず、Activity に紐づく ViewModel に残ること。次に 🎤 でシートを開くと古い Added を拾って即 onAdded で閉じ、
      聞き取り中が出なかった。VoiceInputEvent.AddedHandled を足し、VoiceInputSheet が Added を受け取ったら Idle へ戻す。
      開いた時点で残っていた古い Added は onAdded へ渡さず Idle へ戻す。AndroidSpeechEngine.start は使い回す recognizer へ startListening する前に cancel する（BUSY の予防）。
      水平展開（オーナー指示）。ウィジェットの VoiceSheetActivity は毎回新しい ViewModel と engine で、finish で ViewModel が破棄され（onCleared で engine.destroy）、状態も recognizer も残らない。
      品名欄・探すのマイク（FieldDictation）は start のたびに先に engine.cancel() し、結果は 1 回きりの state.result で受けて消費するため、同じ原因は無い。
      見た目・文言は変えていない。実機での連続 3 回は BL-192（人手検証）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/SpeechEngine.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/voice/VoiceInputViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputSheetRepeatTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*VoiceInputViewModelTest" --tests "*VoiceInputSheetRepeatTest" --tests "*VoiceInputContentTest" --tests "*VoiceSheetActivityTest" --tests "*ShoppingListScreenTest"、記録の更新後に python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug は未実施。実機は未確認）
- date: 2026-11-15 00:37
  summary: 数量が「〇」だけのとき 0 にせず元の文字のまま残す
  details:
    関連ID:
      - BL-185
    変更内容: >-
      手入力の数量の正規化（QuantityText.normalize）で、先頭の漢数字が「〇」だけ（〇〇も含む）で助数詞が続かないとき 0 になっていた経路を直し、元の文字のまま残すようにした。
      「〇個」など助数詞付き（0個）、「〇分」など助数詞でない語（そのまま）、ほかの漢数字・算用数字の扱いは変えていない。
      音声入力は助数詞が続かない語を数量に分けないため、この経路だけが原因だった。使い方の説明に該当する記述は無い
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/voice/QuantityText.kt
      - core/src/test/kotlin/com/ikumemo/core/voice/QuantityTextTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*QuantityTextTest"、記録の更新後に python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug は未実施）
- date: 2026-11-15 00:27
  summary: おまかせ（壁紙の色）の配色の分岐を Android の版ごとに単体テストで確かめられるようにする
  details:
    関連ID:
      - BL-182
    変更内容: >-
      ThemeChoice.palette(context, darkTheme) に既定引数 sdkInt（既定は Build.VERSION.SDK_INT）と wallpaperColors（既定は端末の壁紙の配色）を足し、版の判定と壁紙の配色の取得を差し替えられるようにした。
      本番の呼び出し側は引数を渡さないため挙動は変わらない。ThemeChoiceTest に、Android 11 以前（30）は壁紙の配色を呼ばず既定のポップの配色になること、
      Android 12 以降（31）は壁紙の配色（明るい・暗いの両方）になることを足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemeChoice.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ThemeChoiceTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ThemeChoiceTest"、記録の更新後に python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・assembleDebug は未実施。見た目の変更は無い）
- date: 2026-11-15 00:25
  summary: 見本の色の保存値 seed の 16 進の検証を半角の 0-9・a-f・A-F だけにする
  details:
    関連ID:
      - BL-181
    変更内容: >-
      ThemeChoice.fromStorageKey の seed:#RRGGBB の検証が isDigit を使い全角数字なども通していたため、半角の 0-9・a-f・A-F だけを通すようにした。
      それ以外を含む値は従来の不正値と同じく既定のポップに戻り、半角の大文字・小文字の値は従来どおり読める。仕様はオーナーの回答（半角に絞る）による
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemeChoice.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ThemeChoiceTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ThemeChoiceTest"、記録の更新後に python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（ThemeChoiceTest に全角数字・全角英字・範囲外の英字・半角の大文字小文字のテストを追加。フル検証・Android Lint・assembleDebug は未実施。見た目の変更は無い）
- date: 2026-11-15 00:19
  summary: データの書き出しのテーマを、画面用に置き換える前の保存値で書くようにする
  details:
    関連ID:
      - BL-180
    変更内容: >-
      AppearancePrefs に、端末で選べるかどうかで置き換えない保存したままのテーマ savedTheme（Flow）を足し、BackupSettingsStore.read の theme をそこから書くようにした。
      Android 11 以前でおまかせ（wallpaper）を保存したまま書き出しても theme は wallpaper のまま出て、Android 12 以降で読み込めばおまかせに戻る。
      プリセットと seed の値は従来どおり、未知の値は従来どおり既定のポップの値で書き出す（保存値は ThemeChoice.fromStorageKey を通すため、バックアップの値の種類は増えない。既定値で決めた）。
      読み込み側（apply）と画面に出す配色（appearance は 11 以前で既定）は変えていない。DESIGN.md の F-64 の行に書き出しの扱いを足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppearancePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupSettingsStore.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/BackupSettingsStoreTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*BackupSettingsStoreTest" --tests "*BackupRoundTripTest" --tests "*AppearanceViewModelTest"、
      記録の更新後に python scripts/validate-records.py
    検証結果: >-
      成功 - 軽い検証がすべて成功（BackupSettingsStoreTest 4 件を含む。フル検証・Android Lint・assembleDebug は未実施。見た目の変更は無いためスクリーンショットは流していない）
- date: 2026-11-14 23:30
  summary: 着せ替えに「見本の色から選ぶ」を足し、選んだ色から明るい・暗いの配色を作る
  details:
    関連ID:
      - BL-177
    変更内容: >-
      ThemeChoice に種の色を持つ data class ThemeChoice.Seed を足し、保存値は従来の appearance_theme と書き出しの設定の theme に "seed:#RRGGBB" を入れる形にした
      （プリセットと wallpaper の値は不変。形の壊れた seed の値と未知の値は既定のポップへ戻し、見本に無い色でも形が正しければ読む。古い版は seed の値を読むと既定のポップになる）。
      配色は新しい SeedPalette が種の色の色相から 5 つの系統（主・副・第三は色相を 60 度回す・中間色・中間色の変種）を作り、Material 3 と同じ役割ごとの明るさ
      （明るい画面は 40/100/90/10、背景 98、暗い画面は 80/20/30/90、背景 6 など）で IkumemoPalette の全項目を埋める。明るさは CIELAB の L* で取り、sRGB に収まらない色は
      明るさを保ったまま彩度だけを二分探索で下げるため、どの種の色でも文字と面の色の比 4.5:1 以上になる。依存は足していない
      （計算は 200 行ほどの純粋な Kotlin で足り、依存を足すと版の組み合わせ・ライセンス・配布物の大きさの確認が増えるため。Material の配色計算ライブラリの配布の形と版の相性は未確認。
      HCT ではなく CIELAB の近似のため、Material の配色とは色味が少し違う）。見本は SeedSwatch（10 色、読み上げ用に身近なものの名前）にまとめ、着せ替えのテーマのカードの下に
      丸い見本（押せる大きさ 52dp、選んだ色は枠とチェックの印）を並べる SeedColorPicker を足した。おまかせと違い全 OS で選べ、角の丸みはおまかせと同じ標準にした（既定値で決めた）。
      アプリ本体・ウィジェット（WidgetColors.from）・写真の上のペンの色は ThemeChoice.palette を通して同じ配色に従う。使い方（help/theme.md）に 1 行足し、docs/USER_GUIDE.md を作り直した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/SeedPalette.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/SeedSwatch.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemeChoice.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SeedColorPicker.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/theme.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/SeedPaletteTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/SeedThemeFollowTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/BackupRoundTripTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AppearanceContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SeedPaletteTest" --tests "*SeedThemeFollowTest" --tests "*BackupRoundTripTest" --tests "*AppearanceContentTest"
      --tests "*ThemeChoiceTest" --tests "*WallpaperPaletteTest" --tests "*ThemeContrastTest"、記録の更新後に --quick --tests "*HelpContentTest"
    検証結果: >-
      成功 - 軽い検証がすべて成功（フル検証・Android Lint・スクリーンショットは未実施。着せ替え画面の見た目が変わるため、正解画像の記録し直しは区切りの作業で判断する）
- date: 2026-11-14 23:20
  summary: 着せ替えのテーマに「おまかせ（壁紙の色）」を足す
  details:
    関連ID:
      - BL-176
    変更内容: >-
      テーマの選択肢を sealed interface ThemeChoice で表し、ThemePreset（enum）をその実装にし、おまかせを data object ThemeChoice.Wallpaper として足した。
      保存は従来の appearance_theme に値 wallpaper を足すだけで、プリセットの値と書き出しの設定の theme の形は変えていない（古いバックアップはそのまま読め、
      古い版が wallpaper を読むと既定のポップになる）。配色は ThemeChoice.palette(context, dark) で取り、おまかせは Android 12 以降の
      dynamicLight/DarkColorScheme を IkumemoPalette へ写す（toPalette）。アプリ本体は AppThemeHost から IkumemoTheme(choice) を、ウィジェットは
      WidgetColors.from(context, appearance) を通して同じ配色を使い、写真の上のペンの色もこのテーマの上で決まる。Android 11 以前では着せ替えに
      おまかせのカードを出さず、保存値が wallpaper でも AppearancePrefs が書き換えずに既定のテーマとして読む。角の丸みはシンプルと同じにした（既定値で決めた）。
      BL-177 は ThemeChoice に実装（種の色の値を持つ型）を足し、fromStorageKey と palette の分岐を増やせば同じ保存の形に乗る。
      使い方（help/theme.md）に 1 行足し、docs/USER_GUIDE.md を作り直した。
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemeChoice.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemePreset.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/IkumemoTheme.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppearancePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupSettingsStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetColors.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/theme.md
      - docs/USER_GUIDE.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ThemeChoiceTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/WallpaperPaletteTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/BackupRoundTripTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AppearanceContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ThemeChoiceTest" --tests "*WallpaperPaletteTest" --tests "*AppearanceViewModelTest"
      --tests "*AppearanceContentTest" --tests "*BackupRoundTripTest" --tests "*ThemeContrastTest" --tests "*MemoListWidgetTest"、
      行の長さを直した後に --quick --tests "*WallpaperPaletteTest" --tests "*AppearanceViewModelTest"
    検証結果: >-
      成功 - 1 回目はテストが通り ktlint・detekt がテストの行の長さで失敗、直した 2 回目は軽い検証がすべて成功（フル検証・スクリーンショットは未実施）
- date: 2026-11-14 23:00
  summary: BL-175 のプリセット 4 色の見直しに伴い、スクリーンショットの正解画像を記録し直す
  details:
    関連ID:
      - BL-175
    変更内容: >-
      MainScreensScreenshotTest の memo・calendar・reminder を 1 画面ずつ記録し直した（各画面 24 枚のうち、ポップ・やさしい・クールの 3 テーマ
      × 明暗 × 文字の大きさ 3 段階 = 18 枚、計 54 枚が変わった。シンプルは変化なし）。ポップの extra_large（文字が最大）の memo を縮小せず読み、
      背景が FFF4EC のコーラルオレンジ系になり、文字・ボタンの崩れが無いことを確かめた。
    変更ファイル:
      - mobile/screenshots/memo/
      - mobile/screenshots/calendar/
      - mobile/screenshots/reminder/
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true --tests "*MainScreensScreenshotTest.memo*"（calendar・reminder も同様）、
      続けて -Proborazzi.test.verify=true で 3 画面を 1 画面ずつ
    検証結果: >-
      成功 - 記録し直しの 3 回と verify の 3 回すべて終了コード 0（フル検証は未実施）
- date: 2026-11-14 22:54
  summary: プリセットテーマ 4 種の背景・面・アクセントを色の系統で分ける
  details:
    関連ID:
      - BL-175
    変更内容: >-
      4 種とも背景と面がほぼ白でアクセントだけが違い、ポップとやさしいはピンク系、クールとシンプルは暗い無彩色寄りで近く見えていた。
      ポップはコーラルオレンジ（背景 FFF4EC、アクセント B5381C、副色アンバー）、やさしいはセージグリーン（背景 EEF5EB、アクセント 4A6B4F、
      副色にくすみピンク）、クールは青みの背景（EDF2FD）で紺のまま、シンプルは白黒のまま（青の副色）とし、明るい・暗いの両方の色を直した。
      ポップの角の丸みをやさしいと別にした（16・28・36 dp）。背景と面は従来どおり同じ色にし、面の中の文字は BL-170 の onSurface のまま変えない。
      storageKey・表示名・ThemePreset の構造は変えていない。ウィジェットは WidgetColors がパレットを読むため変更なしで新しい配色に従う。
      テーマごとのパレットを 1 か所（ThemePreset）に並べる形のままなので、BL-176・BL-177 は同じ IkumemoPalette を返す生成を足せばよい。
      ThemeDistinctTest で、色付き 3 種の背景とアクセントの色相差が 40 度以上、シンプルの背景が無彩色、角の丸みが全テーマで別であることを確かめ、
      既存の ThemeContrastTest（全ペア 4.5:1 以上）・InkColorTest・PhotoInkTest も通った。
      見た目が変わるため、スクリーンショットテストは流していない。記録し直しが必要なのは MainScreensScreenshotTest の memo・calendar・reminder
      （全画面。テーマの背景と面の色が変わるため）。
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ThemePreset.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/ThemeDistinctTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ThemeDistinctTest" --tests "*ThemeContrastTest" --tests "*InkColorTest" --tests "*PhotoInkTest" --tests "*MemoListWidgetTest"
    検証結果: >-
      成功 - 軽い検証（フル検証は未実施）。スクリーンショットテストは未実施（記録し直しが必要）
- date: 2026-11-14 22:46
  summary: 背景の写真の上に直接置く文字（副題・見出し・チップ・ボタン）をペンの色にする
  details:
    関連ID:
      - BL-174
    変更内容: >-
      設定と着せ替えで、面に載らず写真（膜）の上に出る文字がテーマの固定色（onSurfaceVariant・primary）のままで読めなかった。
      ペンの色を LocalPhotoInk として AppThemeHost から渡し（写真があり、おまかせでないときだけ。それ以外は null）、
      photoInk（無ければテーマ色のまま）を設定の各画面の副題・区分の見出し・件数と時刻・空の案内・FAQ の答えに使った。
      着せ替えの未選択のチップ（文字の大きさ・暗い画面・ペンの色）と「写真を選ぶ」「外す」のボタンも photoFilterChipColors・photoOutlinedButtonColors・
      photoTextButtonColors で文字色をペンの色にした（選択中のチップは従来どおり面の色）。ペンの色のチップは各ペンの色で塗った文字をやめ、ペンの色に揃えた
      （明るいテーマ＋暗い写真でくろの文字が背景に溶けるため）。写真が無い・おまかせのときの見た目は変えない。
      テスト PhotoInkTest で、ペンの色が無ければテーマ色のまま、ペンの色があれば全テーマ・明暗でそれに替わり膜との比が 4.5:1 以上であることを確かめた。
      見た目が変わるため、スクリーンショットテストは流していない（写真が無い画面は変わらない想定）。
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/PhotoInk.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AboutScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/FeedbackScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ProductManageScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/StoreManageScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ReminderSettingsScreen.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/PhotoInkTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*PhotoInkTest" --tests "*InkColorTest"
    検証結果: >-
      成功 - 軽い検証（フル検証は未実施）。スクリーンショットテストは未実施
- date: 2026-11-14 22:26
  summary: 下タブの切り替えで 2 段積んだ画面からの復帰と外部起動の経路をテストで固定する
  details:
    関連ID:
      - BL-173
    変更内容: >-
      BL-173（点検の要修正への対応で、オーナーの指示により起票せずに完了）。BL-168 の navigateToTab の popBackStack ループに次のテストが無かった。
      (a) 設定の下の画面（設定、着せ替え）まで 2 段積んだ状態から下タブを押すと、そのタブの一覧まで戻ること。
      (b) 設定の下の画面を開いた状態から、ウィジェット・ショートカット相当の外部起動（おしらせを開く要求）が来ても、pop の途中の画面を経由して
      outsideRoute が外れず、おしらせのガイドが出ないこと。(b) は実際に起きるかを確かめたが起きなかった
      （pop と navigate は同じフレーム内で終わり、再構成の前に最終の画面だけが観測されるため）。修正は入れずテストで固定した。
      製品コードと下タブの見た目は変えていない。
    変更ファイル:
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/BottomTabSettingsTest.kt
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*BottomTabSettingsTest"
    検証結果: >-
      成功 - 軽い検証（フル検証は未実施）
- date: 2026-11-14 22:30
  summary: 数量の欄の漢数字の変換を、助数詞が続くときだけにする
  details:
    関連ID:
      - BL-172
    変更内容: >-
      BL-172（オーナーの指示で起票せずに完了）。QuantityText.normalize が先頭の漢数字を無条件に算用数字へ変えており、
      「十分」が「10分」になるなどの誤変換があった。先頭の漢数字の後ろが空か、QuantityParser の助数詞の一覧（counters を internal にして共有）と
      完全に一致するときだけ変換する（二本 → 2本、三 → 3、十個 → 10個、百二十g → 120g。十分・一部・一杯・二本ずつ は変えない）。
      全角数字の半角化は従来どおり全体に効く。保存済みのデータ、音声の QuantityParser の挙動、欄のキーボードは変えない。
      未変更で報告する点は、「〇」だけの入力が 0 になること。
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/voice/QuantityText.kt
      - core/src/main/kotlin/com/ikumemo/core/voice/QuantityParser.kt
      - core/src/test/kotlin/com/ikumemo/core/voice/QuantityTextTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*QuantityTextTest" --tests "*RecordRepositoryTest"
    検証結果: >-
      成功 - 軽い検証（フル検証は未実施）
- date: 2026-11-14 22:00
  summary: Baseline Profile を実機の Macrobenchmark で作り直し、コールドスタートを計測する
  details:
    変更内容: >-
      BL-100。:baselineprofile モジュールで USB 接続の Pixel 8 Pro（Android 17）から規則を生成し（mobile/src/release/generated/baselineProfiles/）、
      旧 mobile/src/main/baseline-prof.txt（7 行の広い規則）を削除して置き換えた。前回の startActivityAndWait の失敗（Unable to confirm activity launch completion）は、
      アプリのクラッシュではなく（logcat で Displayed +151ms を確認）、benchmark-macro 1.3.4 が Android 17 に未対応なためで、androidx.baselineprofile・benchmark-macro を 1.4.1 に上げて解消した。
      規則あり計測は対象アプリの profileinstaller が 1.4.0 未満だと API 34 以降で失敗するため、profileinstaller を 1.3.1 から 1.4.1 に上げた。
      計測（Pixel 8 Pro、コールドスタート 10 回、timeToInitialDisplayMs）は規則なし 中央値 186.3ms（最小 163.3・最大 240.8）、規則あり 中央値 182.3ms（最小 162.3・最大 202.9）。
      上位機のため差は小さい。中級機での 1.5 秒以内の確認は BL-171（人手検証）。計測用に入れた com.ikumemo.mobile と baselineprofile は端末から消えている。
    変更ファイル:
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/baseline-prof.txt
      - mobile/src/release/generated/baselineProfiles/baseline-prof.txt
      - mobile/src/release/generated/baselineProfiles/startup-prof.txt
      - docs/BASELINE_PROFILE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ANDROID_SERIAL=39181FDJG008MY ./gradlew :mobile:generateBaselineProfile、同 :baselineprofile:connectedBenchmarkReleaseAndroidTest、
      python scripts/quality-gates.py（フル検証）
    検証結果: >-
      成功 - 生成・計測とも成功。フル検証は ktlint・detekt・Android Lint・単体テスト・assembleDebug・記録ファイル・使い方・秘密情報が成功。Markdown 静的解析は行長の指摘を直して再実行し成功
- date: 2026-11-14 15:57
  summary: 背景の写真とペンの色しろで、明るいテーマの面の中の文字が白になって読めない不具合を直す
  details:
    変更内容: >-
      BL-170。原因は、テーマの surface と background が同じ色のため、AppThemeHost が onBackground をペンの色へ差し替えると、
      Surface の既定の文字色（contentColorFor）が background に先に当たり、面の中の文字までペンの色になること。
      AppThemeHost の onBackground の差し替えをやめ、ペンの色は LocalContentColor（写真の上に直接置く文字）だけに効かせる。
      面の中は文字色を明示して onSurface に戻す（ReadablePanel の Surface、メモの行 SwipeableItemRow）。
      写真の上に直接置くお店のチップ（StoreChip）は、背景の写真があるときだけペンの色に合わせる（暗い膜の上で濃い文字になっていたため）。
      背景の写真が無いときの見た目は変わらない（onBackground と onSurface は全テーマで同値）ため、スクリーンショットの正解画像は記録し直し不要
      （背景の写真を使う条件のスクリーンショットテストも無い）。InkColorTest に、全テーマ・明暗・ペンの色で面の文字と写真の上の文字が 4.5:1 以上になるテストを足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ReadablePanel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/StorePicker.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/InkColorTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*InkColorTest"
    検証結果: >-
      成功（軽い検証）- ktlint・detekt・コンパイル・InkColorTest が成功。Android Lint・全テスト・assembleDebug のフル検証は未実施。
      実機での確認は BL-109 で行う
- date: 2026-11-14 15:20
  summary: 文字が大きいときの期限の画面の最近買った物の行で、ボタンを品名の下の行へ回す
  details:
    変更内容: >-
      BL-169。RecentSection.kt の RecentRow で、文字の倍率（LocalDensity の fontScale、端末の設定とアプリ内の着せ替えを掛けた後の値）が 1.5 以上のとき、
      品名と買った日を 1 行目、「期限なし」「期限を決める」を右寄せの 2 行目に並べる。font_scale 2.0 の「とても大きい」で品名に幅が残る。
      1.5 未満（標準・大・とても大きいに端末標準の組み合わせ）の並びは変えないため、スクリーンショットの正解画像は記録し直していない。
      テストは ReminderContentTest に、標準ではボタンが品名の右、2.0 倍では品名の下に並ぶことを足した（Robolectric は文字幅を正しく測れないため、1 行に収まるかは実機で確かめる）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/RecentSection.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/ReminderContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ReminderContentTest"
    検証結果: >-
      成功（軽い検証）- ktlint・detekt・コンパイル・指定したテストが成功。Android Lint・全テスト・assembleDebug のフル検証は未実施。
      スクリーンショットテストと実機の確認は未実施（BL-151 で期限の画面を確かめ直す）
- date: 2026-11-14 15:05
  summary: 下タブを押したときに、タブの上に積んだ設定が復元されて残る不具合を直す
  details:
    変更内容: >-
      BL-168。IkumemoApp.kt の navigateToTab で、移動の前に、いまタブの画面でなければ（設定やその下の画面なら）タブの画面まで戻る履歴を閉じるようにした。
      これで popUpTo の saveState が設定ごと保存せず、同じタブを押すと一覧へ戻り、別のタブ経由でも設定が復元されない。
      タブごとの saveState・restoreState（スクロール位置などの保存）は残している。設定を開いている間に下タブが選ばれていない見え方は変えていない。
      テストは BottomTabSettingsTest を足し、同じタブを押す場合と別のタブ経由の場合の 2 通りを確かめる
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/BottomTabSettingsTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*BottomTabSettingsTest" --tests "*OpenedFromOutsideGuideTest" --tests "*OnboardingTest"
    検証結果: >-
      成功（軽い検証）- ktlint・detekt・コンパイル・指定したテストが成功。Android Lint・全テスト・assembleDebug のフル検証は未実施。
      実機での確認は BL-148 で行う
- date: 2026-11-14 14:57
  summary: 手で入れた数量の全角数字と先頭の漢数字を、保存時に半角の算用数字へそろえる
  details:
    変更内容: >-
      オーナー指示（2026-11-14、数量の欄は単位を残し、数字を半角にする）により、core/voice/QuantityText.kt に normalize を足した。
      前後の空白を除き、全角の数字を半角にし、先頭の漢数字（KanjiNumber、〜999）を算用数字にする。単位やそれ以外の文字は残し、空なら null。
      ShoppingRepository の addItem・updateItem と RecordRepository の addRecord・updateRecord で、これまでの trim と空なら null の処理を置き換えた。
      欄は自由入力のまま（キーボードの種類は変えない）。バックアップの読み込みは変えていない。
      テストは QuantityTextTest を足し、RecordRepositoryTest の数量の入力を全角数字・漢数字に変えて「2本」で保存されることを確かめる。
      あわせて、オーケストレーションによる実機の人手検証（BL-008・BL-118・BL-148・BL-151・BL-153）の結果を BACKLOG へ追記し、
      見つかった不具合 2 件を BL-168・BL-169 として起票した
    変更ファイル:
      - core/src/main/kotlin/com/ikumemo/core/voice/QuantityText.kt
      - core/src/test/kotlin/com/ikumemo/core/voice/QuantityTextTest.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ShoppingRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/RecordRepository.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordRepositoryTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*QuantityTextTest" --tests "*RecordRepositoryTest"
      --tests "*ShoppingListViewModelTest" --tests "*VoiceInputViewModelTest"、./gradlew :mobile:installDebug（Pixel 8 Pro）
    検証結果: >-
      成功（軽い検証）- 1 回目は detekt の ReturnCount で失敗し、関数を分けて 2 回目で ktlint・detekt・コンパイル・指定したテストが成功した。
      Android Lint・全テスト・assembleDebug のフル検証は未実施。実機へのインストールは成功したが、数量の欄の実機での入力は未確認
- date: 2026-11-14 13:32
  summary: 配布元 copilot-rules 0.30.0 の秘密情報スキャンと記録ファイル検証フックを取り込む
  details:
    変更内容: >-
      オーナー判断（2026-11-14）により、scripts/scan-secrets.py を配布元 0.30.0 版へ置き換え、ikumemo 固有のファイル名規則
      （keystore.properties・secrets.properties）を FILE_NAME_RULES へ移した。既定の検査対象はステージ済みの差分になり、
      --include-unstaged（作業ツリーと未追跡）、--range（コミット範囲）、--require-changes（対象が空なら終了コード2）が使える。
      Slack・JWT・接続文字列などの規則が増え、ダミー値の除外と行ごとの許可の目印は無くなった（現在は使っていない）。
      置き換え前に配布元版で全履歴の範囲を検査し、検出が0件であることを確かめた。
      scripts/quality-gates.py の秘密情報スキャンは、これまでと同じ作業ツリーを検査するよう --include-unstaged 付きで呼ぶ。
      オーナー判断により記録ファイル検証の PostToolUse フック .claude/hooks/validate-records-after-edit.py を置き、
      .claude/settings.json の hooks.PostToolUse へ登録した（既存の PreToolUse の guard-git-hook と permissions は変えていない）。
      DESIGN.md の品質ゲートと settings の記述を更新した。文書側の取り込みは CHANGELOG.md に記録した
    変更ファイル:
      - scripts/scan-secrets.py
      - scripts/quality-gates.py
      - .claude/hooks/validate-records-after-edit.py
      - .claude/settings.json
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --skip-gradle、python scripts/quality-gates.py --only secrets、
      配布元版の scan-secrets.py による全履歴の範囲の検査、移した規則とフックの入力別の終了コードの確認
    検証結果: >-
      成功 - markdownlint・records・user-guide・secrets がすべて成功した。keystore.properties・secrets.properties・.jks が検出され、
      .env.example は検出されないことを確かめた。フックは記録ファイルの編集で検証を流して0、それ以外の編集では何もせず0を返した
      （Gradle の5ゲートはコードを変えていないため流していない。実セッションでのフックの発火は次のセッションから）
- date: 2026-11-13 11:24
  summary: 記録の画面の品名検索の欄に、編集シートと同じマイクを付けて話した品名で探せるようにする
  details:
    変更内容: >-
      BL-117 を実装した（オーナー承認 2026-11-13）。CalendarScreen.kt の SearchPane の検索欄の trailingIcon に FieldMic を付け、
      欄のテストタグ CalendarTestTags.SEARCH_INPUT を聞き取りの欄の名前に使い、聞き取った文字は 1 行の欄として検索語を置き換えて QueryChanged を送る。
      上部のバーには足さず欄の中に置いた（BL-137 で上部のバーの幅に余裕が少ないため）。権限・認識の仕組みは既存の F-04 の FieldDictationViewModel と同じで、
      新しい権限や端末の外への送信経路は増えていない。音声認識が使えない端末では出さない（既存の FieldMic の挙動）。
      Compose のテスト CalendarSearchMicTest を足し、マイクの結果が検索語に入ることと、認識が使えないときマイクが無いことを確かめた。
      使い方 help/search.md にマイクの 1 行を足して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-42 の行を更新した。
      実際の音声認識での確認は人手検証 BL-118 に足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarSearchMicTest.kt
      - mobile/src/main/assets/help/search.md
      - docs/USER_GUIDE.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*CalendarSearchMicTest" --tests "*CalendarContentTest" --tests "*FieldMicTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug・スクリーンショットの比較は流していない。MainScreensScreenshotTest の calendar は検索を閉じた月の表示で、マイクの窓口も渡さないため画像は変わらない）
    関連ID:
      - BL-117
- date: 2026-11-13 11:19
  summary: 画面と掲載文の「つ」の件数表記で、数字の前の空白も外して「の2つ」にそろえる
  details:
    変更内容: >-
      BL-155 を実装した（オーナー判断 2026-11-13、BACKLOG には起票せず今回のコミットで完了扱い）。BL-154 で数字と「つ」の間の空白を外したのに続き、
      半角数字と「つ」の件数表記の直前にある半角空白を外した。使い方 help/theme.md（を4つから）、undo.md（いちばん新しい1つ）、
      usual.md（押すたびに1つずつ）、store.md（登録が2つある）と、docs/store/STORE_LISTING.md（の2つのウィジェット）を直し、
      python scripts/make-user-guide.py で docs/USER_GUIDE.md を作り直した。strings.xml と画面に出る Kotlin の文言（BackupValidator.kt を含む）には
      該当が無く変えていない。行頭・見出しの記号・表の区切りの直後で元から空白が無いもの（# 2つのウィジェット、- 4つのテーマ、表の 4つのテーマ）はそのまま。
      「つ」以外の助数詞（牛乳 2 本 など）、時刻、コードのコメント、RELEASE_NOTES・CLOSED_TEST・PLAN は範囲外で変えていない。
      文言を期待値に持つテストは無かった。DESIGN.md の F-01 の表記の決まりに、数字の前にも空白を入れないことを書き足した
    変更ファイル:
      - mobile/src/main/assets/help/theme.md
      - mobile/src/main/assets/help/undo.md
      - mobile/src/main/assets/help/usual.md
      - mobile/src/main/assets/help/store.md
      - docs/USER_GUIDE.md
      - docs/store/STORE_LISTING.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*HelpContentTest" --tests "*HelpScreenTest"、python scripts/validate-records.py、
      python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流していない。画面のコードは変えておらずスクリーンショットは対象外）
    関連ID:
      - BL-155
- date: 2026-11-13 11:16
  summary: 文字で追加でも「牛乳 2本」を品名 牛乳 と数量 2本 に分けて入れる
  details:
    変更内容: >-
      BL-147 を実装した（UX レビュー B7、オーナー承認済みの案）。ShoppingListViewModel の handleDataEvent で、AddByText のときだけ
      core の QuantityParser.split を通して品名と数量に分け、repository.addItem(name, quantity) に渡すようにした。
      「いつもの」チップと入力中の候補（AddProduct）は品名そのままで変えていない。区切りでの複数件の分割はしない。
      数量だけの入力（「2本」）は QuantityParser が品名を空にしない既存のふるまいにより、分けずにそのまま品名になる。
      分けた品名がメモにあるときは R-01 のとおり追加せずに光らせ、既存の項目の数量は書き換えない（ShoppingRepository は変更なし）。
      ShoppingListViewModelTest に、分けて入ること、メモにある品名は光って数量が変わらないこと、数量だけの入力はそのまま品名になることのテストを足した。
      QuantityParser は変えていない。使い方 help/text.md に一文を足し、docs/USER_GUIDE.md を作り直した。DESIGN.md の F-03 に数量の取り出しを足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/main/assets/help/text.md
      - docs/USER_GUIDE.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ShoppingListViewModelTest" --tests "*ShoppingRepositoryTest"
      --tests "*VoiceTextParserTest"、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流していない。画面の見た目は変わらないためスクリーンショットは対象外）
    関連ID:
      - BL-147
- date: 2026-11-13 11:11
  summary: メモの項目の編集シートに「メモから外す（やめた）」を足し、スワイプしなくても「やめた」にできるようにする
  details:
    変更内容: >-
      BL-146 を実装した（UX レビュー B6、オーナー承認済みの案）。EditItemSheet.kt に引数 onRemove を足し、下のボタンを EditSheetActions に分けて、
      「閉じる」の行の下に区切り線をはさんで「メモから外す（やめた）」の TextButton（48dp 以上、文言は strings.xml の edit_remove_from_memo、
      目印 TestTags.EDIT_REMOVE）を置いた。ShoppingListScreen.kt はシートの切り替えを HomeSheets に分け、外すときは CloseSheet を送ってから
      スワイプと同じ Dismiss を送る。シートで直した内容は保存しない（ModalBottomSheet の onDismissRequest を通らないため保存の処理は動かない）。
      ShoppingListScreenTest に外すとシートが閉じて「やめた：洗剤」が出て、元に戻すと直しかけた数量が残らないことのテストを、
      ShoppingListViewModelTest に編集中の項目を外すとシートが閉じて元に戻すの申し出（Dismissed）が出るテストを足した。
      FieldMicTest の EditItemSheet の呼び出しに onRemove を足した。使い方 help/edit.md・swipe.md に一文を足し、docs/USER_GUIDE.md を作り直した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/EditItemSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/edit.md
      - mobile/src/main/assets/help/swipe.md
      - docs/USER_GUIDE.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/FieldMicTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ShoppingListScreenTest" --tests "*ShoppingListViewModelTest"
      --tests "*AccessibilityTest" --tests "*FieldMicTest"、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目は追加した ViewModel のテストが失敗した（元に戻すの申し出とメモの一覧の更新が別々に届くのを待っていなかった）。
      両方がそろうまで待つよう直した 2 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流していない。編集シートはスクリーンショットテストの対象外）
    関連ID:
      - BL-146
- date: 2026-11-13 11:01
  summary: データの書き出しは写真も含む 1 行を表に出し、写真なしと表計算用を「ほかの形式で書き出す」の折りたたみへ入れる
  details:
    変更内容: >-
      BL-145 を実装した（UX レビュー B5、オーナー承認済みの案）。DataScreen.kt の「書き出す」の区切りを ExportSection に分け、
      主の行「データを書き出す（写真も含む）」の下に OtherExportToggle（「ほかの形式で書き出す」、矢印の向きで開閉を示し、
      読み上げ用に「開いています」「閉じています」の状態を付ける）を置き、開いたときだけ写真なし・表計算用の 2 行を出す。
      開いた状態は remember で持ち保存しない。写真なし・表計算用の書き出しの処理中は、進み具合が隠れないよう閉じていても 2 行を出す。
      説明から ZIP・JSON・CSV の語を外し「いくメモで読み込めるファイル」「表計算ソフト（Excel など）で開けるファイル」と書き、
      data_import_detail も「いくメモで書き出したファイル（表計算用は除く）」に直した。書き出す中身・ファイル名・ExportKind は変えていない。
      使い方 help/data-export.md・device-change.md を新しい行の名前に合わせ、docs/USER_GUIDE.md を作り直した。
      DataContentTest に、最初は写真も含む行だけが見えること・開くと 2 行が出て押すと onExport が呼ばれること・もう一度押すと閉じること・
      説明に形式名が出ないことのテストを足し、既存の書き出しのテストは開いてから押すよう直した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/data-export.md
      - mobile/src/main/assets/help/device-change.md
      - docs/USER_GUIDE.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*DataViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流していない。設定の画面はスクリーンショットテストの対象外）
    関連ID:
      - BL-145
- date: 2026-11-13 10:55
  summary: 通知の許可を求める前の説明に見出しと届く時刻と次の画面の案内を出す
  details:
    変更内容: >-
      BL-140 を実装した（UX レビュー A11）。NotificationPermission.kt の説明のダイアログを、時刻と押したときの処理だけを引数に取る
      NotificationRationaleDialog に分け、見出し「通知を使います」、本文「期限の前の日の（時刻）に、スマートフォンの通知でお知らせします。
      次の画面で『許可』を押してください。」、ボタン「次へ」と「今はしない」にした。時刻は BL-143 で ReminderUiState に持たせた notifyTime を
      formatReminderTime（例 9:00）で出す。文言は strings.xml の reminder_permission_title・reminder_permission_rationale（時刻の引数付きに変更）・
      reminder_permission_next（旧 reminder_permission_allow を置き換え）。説明を出す条件（Android 13 以降・初めて期限を決めたとき・案内からは 2 回まで）は変えていない。
      使い方 help/due.md に初めて決めたときに通知の許可を求めることを足し、docs/USER_GUIDE.md を作り直した。
      NotificationRationaleTest を新設し、見出し・設定した時刻を含む本文・「次へ」で許可を求める処理が呼ばれること・「今はしない」では呼ばれないことを確かめた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/NotificationPermission.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/due.md
      - docs/USER_GUIDE.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/NotificationRationaleTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*NotificationRationaleTest" --tests "*ReminderViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目は新設のテストの行の長さで ktlint が失敗し、行を分けた 2 回目で ktlint・detekt・compile・指定のテストクラス
      （NotificationRationaleTest 3 件・ReminderViewModelTest 10 件）・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流していない。ダイアログはスクリーンショットテストの対象外）
    関連ID:
      - BL-140
- date: 2026-11-13 10:48
  summary: 設定の一覧に見出しの区切りと各行の 1 行の説明を付ける
  details:
    変更内容: >-
      BL-139 を実装した（UX レビュー A10）。SettingsScreen.kt に区切りの SettingsSection を足し、
      使い方は見出し無しで一番上（H-01）、「見た目と操作」に着せ替え・操作とお知らせ・ホーム画面に置く、
      「登録したもの」にお店の管理・よく買う物の管理・データ、「このアプリ」にこのアプリについてを並べた。
      見出しは titleSmall の primary 色で semantics の heading を付けた。SettingsItem に説明の detail を足し、
      各行の下に onSurfaceVariant の bodyMedium で出す（SettingsSwitchRow と同じ作り、使い方の行は説明なし）。
      項目・行き先・SettingsItem の並び（enum）とテストタグは変えていない（画面での並びはホーム画面に置くが着せ替えの組へ上がる）。
      文言は strings.xml の settings_section_* と settings_*_detail。使い方 help/ の「設定の」の記述は項目の名前だけで食い違いが無く、直していない。
      SettingsScreenTest を新設し、区切りが全項目を 1 回ずつ含むこと、見出し（heading）と説明が出ること、各行が今までどおりの項目を開くことを確かめた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/SettingsScreenTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*SettingsScreenTest" --tests "*AboutContentTest" --tests "*HelpScreenTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す。設定の画面はスクリーンショットテストの対象外）
    関連ID:
      - BL-139
- date: 2026-11-13 10:40
  summary: 画面に出る「つ」の件数表記を空白なしの「3つ」にそろえる
  details:
    変更内容: >-
      BL-154 を実装した（オーナー判断 2026-11-13。BL-136 で足した voice_add_count が「%1$d つ追加する」で、
      ウィジェットの「%1$dつ」と空白の有無がそろっていなかったため、件数の表記を空白なしにそろえると決めた）。
      strings.xml の voice_add_count を「%1$dつ追加する」に、onboarding_voice_body を「2つに分かれて」に、
      data_export_zip_detail を「1つのファイル」に直した。BackupValidator の読み込み失敗の文言を「1つの記録に期限が複数あります」に直した。
      使い方 help/ の voice.md（1つずつ・3つ追加する）、start.md（2つに分かれて）、index.md と widgets.md（2つのウィジェット）、
      store.md（2つある）、theme.md（4つから）、undo.md（1つだけ）、usual.md（1つずつ）を直し、
      make-user-guide.py で docs/USER_GUIDE.md を作り直した。VoiceInputContentTest の期待値を「2つ追加する」「1つ追加する」に直した。
      Kotlin のコメント、「つ」以外の助数詞、ストア掲載文・リリースノートなど docs/ の文書は変えていない。
      DESIGN.md の F-01 の行を「Nつ追加する」に直し、表記の決まりを足した
    変更ファイル:
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupValidator.kt
      - mobile/src/main/assets/help/index.md
      - mobile/src/main/assets/help/start.md
      - mobile/src/main/assets/help/store.md
      - mobile/src/main/assets/help/theme.md
      - mobile/src/main/assets/help/undo.md
      - mobile/src/main/assets/help/usual.md
      - mobile/src/main/assets/help/voice.md
      - mobile/src/main/assets/help/widgets.md
      - docs/USER_GUIDE.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*VoiceInputContentTest" --tests "*BackupValidatorTest" --tests "*HelpContentTest"、
      python scripts/make-user-guide.py、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す。
      変えた文言は音声入力のシート・初めての案内・データの画面・使い方で、スクリーンショットテストの対象外）
    関連ID:
      - BL-154
- date: 2026-11-13 10:37
  summary: 着せ替えの画面で文字の大きさを一番上に移し、テーマ・暗い画面・背景の写真の順に並べ替える
  details:
    変更内容: >-
      BL-138 を実装した。settings/AppearanceScreen.kt の AppearanceContent の並びを
      テーマ → 背景の写真 → 暗い画面 → 文字の大きさ から、文字の大きさ → テーマ → 暗い画面 → 背景の写真 に変えた
      （背景の写真は選ぶと膜の濃さ・ペンの色が増えるので最後）。各部品の中身と testTag は変えていない。
      AppearanceContentTest に、文字の大きさ・テーマの見本・暗い画面・背景の写真の縦位置（positionInRoot）がこの順に並ぶテストを足した。
      着せ替えの画面はスクリーンショットテストの対象外で、使い方 help/ には並び順の記述が無いため変えていない。
      DESIGN.md の F-60・F-63・F-65 の行に並び順を足した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AppearanceContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*AppearanceContentTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目で ktlint・detekt・compile・指定のテストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-138
- date: 2026-11-13 10:32
  summary: 記録の画面で買い物の点を大きくし、別の月から今日へ戻る「今日」と文字付きの「探す」を出す
  details:
    変更内容: >-
      BL-137 を実装した。calendar/MonthGrid.kt の DayCell で買い物の点を 6dp から 8dp にし、日付の数字との間を 2dp 空けた（マスの最小 48dp は変えない）。
      MonthHeader に省略可能な onToday を足し、渡したときだけ見出しと ▶ の間に「今日」（TextButton、48dp 以上、テストタグ calendar_today）を出す。
      CalendarScreen.kt の MonthPane は今月以外を見ているときだけ onToday を渡し、押すと新しい CalendarEvent.GoToToday を送る。
      CalendarViewModel は GoToToday で showMonth（今月なら今日を選ぶ）を呼ぶ。
      上部のバーの「探す」をアイコンだけの IconButton からアイコン＋文字の TextButton（SearchToggleButton）にし、検索中は「✕ やめる」、
      読み上げは今までどおり「探す」「探すのをやめる」を semantics で付けた。文字が大きくても題名と 2 つのボタンが重ならないよう、題名は 1 行で溢れたら省く。
      strings.xml に calendar_today「今日」と calendar_search_stop「やめる」を足した。
      CalendarContentTest に今月では「今日」が出ない・別の月では出て押すと GoToToday が出る・探すの文字と検索中の「やめる」のテストを、
      CalendarViewModelTest に GoToToday で今月の今日が選ばれるテストを足した。
      使い方 help/calendar.md に「今日」、help/search.md に「やめる」を足して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-40・F-42 の行を合わせた。
      スクリーンショットの calendar の記録し直しはオーケストレータが区切りで行う
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/calendar.md
      - mobile/src/main/assets/help/search.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*CalendarContentTest" --tests "*CalendarViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証の 1 回目で ktlint・detekt・compile・指定の 2 テストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug・スクリーンショットテストは流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-137
- date: 2026-11-13 10:26
  summary: 話して追加の確認カードで、外したカードに取り消し線と「戻す」を出し、追加ボタンに入れる件数を出す
  details:
    変更内容: >-
      BL-136 を実装した。外したカードは品名と数量に取り消し線を引いて onSurfaceVariant で薄くし、
      右のボタンを × から「↩ 戻す」（AutoMirrored の Undo アイコンと文字、TextButton で 48dp 以上）に替えた。
      読み上げ（voice_exclude_card・voice_include_card）は変えず、戻すボタンには semantics で同じ読み上げを付けた。
      追加ボタンは入れる件数を voice_add_count「%1$d つ追加する」で出し、0 件のときは今までどおり voice_add_all「追加する」で押せない。
      strings.xml に voice_add_count と voice_restore_card「戻す」を足した。
      detekt の TooManyFunctions（1 ファイル 11 個）に掛かったため、確認カードと下のボタン（ConfirmCards・ConfirmCard・CardTexts・ToggleCardButton・EditingCard）を
      voice/VoiceConfirmCards.kt へ移した。VoiceInputContentTest に外したカードの「戻す」と件数の付いた追加ボタンのテストを足した。
      使い方 help/voice.md と docs/USER_GUIDE.md、DESIGN.md の F-01 の行を新しい見た目に合わせた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceInputSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/voice/VoiceConfirmCards.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/voice.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/voice/VoiceInputContentTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*VoiceInputContentTest" --tests "*VoiceInputViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目の軽い検証は detekt の TooManyFunctions で失敗し、ファイルを分けた 2 回目で ktlint・detekt・compile・指定の 2 テストクラス・
      markdownlint・records・user-guide・secrets がすべて成功した（Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-136
- date: 2026-11-13 10:14
  summary: ダイアログのボタンの言葉を、確定は「決める」、取り消しは「戻る」にそろえる
  details:
    変更内容: >-
      BL-133 を実装した。strings.xml に common_decide「決める」を足し、使われなくなった reminder_settings_cancel「やめる」を消した。
      日付・時刻を選ぶダイアログ（reminder/DueSheet.kt の期限の日付、calendar/RecordEditSheet.kt の RecordDatePicker、
      settings/ReminderSettingsScreen.kt の時刻）の確定を「できた」から「決める」に、取り消しを「閉じる」「やめる」から「戻る」（common_back）にした。
      settings/DataScreen.kt の確認ダイアログと settings/StoreManageScreen.kt の 2 つのダイアログの取り消しも「戻る」にした。
      確定に操作の名前が入るダイアログ（すべて消す・読み込む・使わない）の確定の文字と、ボタンの並び・動きは変えていない。
      音声カードの「できた」とシートを閉じる「閉じる」は対象外のまま。使い方 help/ にダイアログのボタンを指す記述は無く変えていない。
      DataContentTest の取り消しのテストを「戻る」で押すよう直し、取り消しが「戻る」であるテストを足した。DESIGN.md の UI/UX 方針に 1 行足した
    変更ファイル:
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/DueSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ReminderSettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/StoreManageScreen.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*ManageContentTest" --tests "*OperationContentTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目の軽い検証で ktlint・detekt・compile・指定の 3 テストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-133
- date: 2026-11-13 10:12
  summary: データの画面を見出しで 3 つに区切り、すべて消すを離して error 色にし、処理中の行と結果を分かるようにする
  details:
    変更内容: >-
      BL-134 を実装した。settings/DataViewModel.kt に DataTask（EXPORT_ZIP・EXPORT_JSON・EXPORT_CSV・IMPORT・ERASE）を足し、
      DataUiState は busy の代わりに running（処理中の行、無ければ null）を持つ。busy は running から求める読み取りの値として残し、二重に押せなくする動きは変えない。
      settings/DataScreen.kt の DataContent は結果の知らせを一番上に出し、見出し「書き出す」「読み込む」「消す」（titleSmall・primary）で区切る。
      「データをすべて消す」は読み込みの下に 32dp の間と区切り線を空けて離し、題の文字と確認ダイアログの「すべて消す」を colorScheme.error にした（読み込みの確認は変えない）。
      処理中は押した行に CircularProgressIndicator と strings.xml の data_progress_export・data_progress_import・data_progress_erase の文字を出す。
      行の引数は detekt の LongParameterList を避けるため DataRowSpec にまとめた。使い方 help/data-export.md は区切り・消す行に触れていないため変えていない。
      DESIGN.md の F-72 と F-76 の行を更新した。設定の画面はスクリーンショットテストの対象外のため記録し直していない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/DataViewModelTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*DataContentTest" --tests "*DataViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 4 回目の軽い検証で ktlint・detekt・compile・指定の 2 テストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （1 回目は見出しが増えて画面の外になった行を押せず、切り取られた位置で比べていたため失敗し、performScrollTo と getUnclippedBoundsInRoot に直した。
      2 回目は進み具合が押せる行にまとめられて見つからず useUnmergedTree で探すよう直し、3 回目はテストの行の長さで ktlint に当たった。
      Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-134
- date: 2026-11-13 10:01
  summary: 期限を決めるシートの候補のチップに、今日から数えた日付を曜日付きで添える
  details:
    変更内容: >-
      BL-132 を実装した。reminder/DueSheet.kt の候補（3日後・1週間後・2週間後・1か月後）のチップを private の DueChoiceChip に分け、
      上の行に従来の文字、下の行に日付（strings.xml の新しい reminder_due_choice_date「%1$d月%2$d日（%3$s）」、labelSmall）を出す。
      日付の値は純粋な関数 DueChoice.dateParts（DueDateParts の月・日・曜日。曜日は Locale.JAPANESE の短い名前でカレンダーの dayTitle と同じ作り、
      1 か月後の月末の丸めは DueChoice.from に従う）で作る。「日付を選ぶ」のチップは 1 行のまま、チップの高さは 48dp 以上を保つ。
      使い方の help/due.md に日付も出ることを足して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-50 の行を更新した。
      あわせて BL-131 の実機の確かめ（キーボードが出るか、ショートカット・ウィジェットから開いたとき）を BL-153 として人手検証で起票し、BL-151 の依存から BL-132 を外した。
      シートはスクリーンショットテストの対象外のため記録し直していない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/DueSheet.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/due.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/DueChoiceTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/DueSheetTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*DueSheetTest" --tests "*DueChoiceTest" --tests "*ReminderViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 3 回目の軽い検証で ktlint・detekt・compile・指定の 3 テストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （1 回目は行の長さで ktlint・detekt、2 回目は DueSheet が長くなって LongMethod の detekt 違反になったため、チップを DueChoiceChip に分けた。
      Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-132
- date: 2026-11-13 09:49
  summary: メモの「文字で追加」とカレンダーの「買った物を記録する」のシートを開いたら、品名の欄にフォーカスしてキーボードを出す
  details:
    変更内容: >-
      BL-131 を実装した。shoppinglist/ItemSheets.kt に rememberFocusOnOpen（FocusRequester を作り、LaunchedEffect で requestFocus し、失敗は無視する）を足し、
      AddItemSheet の入力欄（ADD_SHEET_INPUT）に focusRequester を付けた。ショートカット・ウィジェットから開いたときも同じシートのため同じく効く。
      calendar/AddRecordSheet.kt は品名の欄を private の NameField（EditField と同じ見た目で横にマイク）に分け、同じくフォーカスを当てる。
      共用の EditField は引数を足すと detekt の LongParameterList に当たるため変えず、メモの編集シートと記録の編集シートは従来どおり自動でフォーカスしない。
      使い方の text.md と add-later.md は入力欄を押す手順が無いため変えていない。DESIGN.md の F-03 と F-17 の行を更新した。
      シートはスクリーンショットテストの対象外のため記録し直していない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ItemSheets.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/AddRecordSheet.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreenTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/AddRecordSheetTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ShoppingListScreenTest" --tests "*AddRecordSheetTest" --tests "*RecordEditSheetTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定の 3 テストクラス・markdownlint・records・user-guide・secrets がすべて成功した
      （1 回目は EditField に引数を足して LongParameterList、AddRecordSheet が長くなって LongMethod の detekt 違反になったため、品名の欄を NameField に分けた。
      Android Lint・全テスト・assembleDebug は流しておらず、フル検証はループの最後に流す）
    関連ID:
      - BL-131
- date: 2026-11-13 05:39
  summary: カレンダーの記録を消した後に「元に戻す」を出し、記録と付いていたお知らせを同じ id で戻せるようにする
  details:
    変更内容: >-
      BL-135 を実装した。data/RecordRepository.kt の deleteRecord が、消す前の記録・お知らせ・お買い物を DeletedRecord として返すようにし、
      restoreRecord で記録とお知らせを同じ id で戻して品目の集計を計算し直すようにした（お買い物は残っていればそれを、無ければ同じ日・お店のもの、それも無ければ元の id で戻す）。
      お知らせが消えた・戻ったときに次の通知を予約し直すため、RecordRepository に onRemindersChanged を足し、AppContainer で ReminderScheduler.reschedule を渡した。
      ShoppingTripDao に getById を足した（クエリだけでスキーマは変わらない）。
      calendar/CalendarViewModel.kt は消すと RecordUndoOffer（通し番号と消した内容）を出し、CalendarEvent.UndoDelete で戻して戻した記録の日を表示し、UndoExpired で申し出を消す。
      calendar/RecordUndo.kt の RecordUndoEffect がメモの UndoEffect と同じ作りで「消しました：品名 ／ 元に戻す」を 5 秒（読み上げ利用時は延長）出し、
      CalendarContent の下に SnackbarHost（testTag calendar_snackbar）を置いた。
      calendar/RecordEditSheet.kt の DeleteAndClose は「閉じる」を右に置き、「この記録を消す」を区切り線の下のシートの一番下へ離した。
      写真のファイルは消さず参照が戻るだけで、R-14 の掃除は端末を使っていない間だけ動き 1 日未満の写真を残すため、追加の対策はしていない。
      使い方 edit-record.md と undo.md を直して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-16・R-12・R-14 の行を更新した。
      あわせて人手検証の BL-118 に、記録の編集シートを 3 通りで閉じたときの保存（BL-144）と、消した後の元に戻す（BL-135）の確認を足した。
      スクリーンショットテストの calendar は差分が無く、記録し直していない（元に戻すの表示は既定の状態では出ない）
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/RecordRepository.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/Daos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordUndo.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/edit-record.md
      - mobile/src/main/assets/help/undo.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/RecordRepositoryTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/RecordEditSheetTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*RecordRepositoryTest" --tests "*CalendarViewModelTest" --tests "*CalendarContentTest" --tests "*RecordEditSheetTest"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.calendar*"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定の 4 テストクラス・markdownlint・records・user-guide・secrets がすべて成功し、calendar のスクリーンショットも一致した
      （1 回目は restoreTrip の return が 3 つで detekt の ReturnCount、時間切れのテストが削除前の一覧の状態を拾ったこと、
      シートの外にはみ出す行を切り取られた座標で比べたことでテストが失敗したため、式を ?: で繋ぎ、待つ条件と比べる座標を直した。フル検証はループの最後に流す）
    関連ID:
      - BL-135
- date: 2026-11-13 05:26
  summary: カレンダーの記録の編集シートを、メモの編集と同じく閉じたら保存するようにする
  details:
    変更内容: >-
      BL-144 を実装した。calendar/RecordEditSheet.kt の ModalBottomSheet の onDismissRequest を CalendarEvent.CloseEditor から save（CalendarEvent.SaveRecord）にし、
      払う・戻る・外を押すのどれで閉じても内容を保存するようにした（確認なし、PLAN §1.3-4）。「直す」ボタンは common_close の「閉じる」に替えて押すと保存し、
      DeleteAndSave を DeleteAndClose、testTag を RECORD_SAVE から RECORD_CLOSE（calendar_record_close）に改名した。
      使われなくなった CalendarEvent.CloseEditor と文字列 calendar_record_save は消した。
      消すときはシートが描かれなくなるだけで onDismissRequest は呼ばれず、保存が走らないことを RecordEditSheetTest で確かめた。
      日付を変えて閉じたときに保存後その日へ移る動き（saveRecord）は変えていない。
      使い方 edit-record.md に「シートを閉じると保存されます」を足して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-16 の行に閉じると保存を足した。
      シートはスクリーンショットテストの対象外のため画像の記録し直しはしていない
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/RecordEditSheet.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/edit-record.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/RecordEditSheetTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/calendar/CalendarViewModelTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*RecordEditSheetTest" --tests "*CalendarViewModelTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定のテスト（RecordEditSheetTest 3 件、CalendarViewModelTest 6 件）・markdownlint・records・user-guide・secrets がすべて成功した
      （1 回目は「閉じる」ボタンの行が 120 文字を超えて ktlint と detekt が失敗したため、引数を行に分けた。フル検証はループの最後に流す）
    関連ID:
      - BL-144
- date: 2026-11-13 05:18
  summary: 3 つのタブのどこからでも設定を開けるようにし、設定を 8 項目に減らして期限の画面からお知らせの時刻へ 1 タップで行けるようにする
  details:
    変更内容: >-
      BL-143 を実装した。メモ画面の ⚙ 設定（アイコン＋文字の TextButton）を settings/SettingsScreen.kt の SettingsButton として共通にし、
      記録の画面の上部のバー（探すの右）と期限の画面の上部のバーにも置いて、IkumemoApp.kt の NavHost から SettingsRoute を開く処理を渡した。
      期限の画面は開く先を ReminderNavigation（使い方・設定・お知らせの設定）にまとめ、一覧の先頭（通知オフの帯の下）に「お知らせの時刻 9:00」の
      ボタン（時刻は設定値、背景の写真があるときは面の上）を置いて OperationRoute を開くようにした。時刻は ReminderViewModel で ReminderPrefs.settings から読み、
      ReminderUiState.notifyTime に持たせた。「操作」と「お知らせ」は 1 つの画面「操作とお知らせ」（OperationScreen）にまとめ、
      ○ を出す・振動・お知らせの時刻・期限の当日もお知らせ の 4 行を並べた。ViewModel は OperationViewModel と ReminderSettingsViewModel のまま使い、
      DataStore のキーは変えていない（分けていた頃のキーで保存した値がまとめた画面に出ることをテストで確かめた）。
      ReminderSettingsScreen と ReminderSettingsRoute は廃止し、お知らせの行は ReminderSettingsRows にした。
      「ご意見・ご要望」は設定の一覧から外して このアプリについて の中の行にした（FeedbackRoute と使い方の「やってみる」の行き先は残した）。
      設定は 使い方・着せ替え・操作とお知らせ・お店の管理・よく買う物の管理・ホーム画面に置く・データ・このアプリについて の 8 項目になった。
      文言は settings_operation を「操作とお知らせ」にし、使われなくなった settings_reminder を消し、reminder_time_button を足した。
      使い方（bought-button.md・swipe.md・notified.md・feedback.md・faq.md）と docs/SUPPORT.md の場所の説明を直して docs/USER_GUIDE.md を作り直し、
      DESIGN.md の設定・操作・お知らせ・F-77・このアプリについて の行と docs/design/WIREFRAME.md の設定と上部のバーを直した。
      スクリーンショットは calendar（上部のバーの右だけが変わった）と reminder（設定のボタンと時刻のボタンが入って一覧が下がった）を記録し直し、memo は差分なしを確かめた
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/OperationScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/ReminderSettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AboutScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/bought-button.md
      - mobile/src/main/assets/help/swipe.md
      - mobile/src/main/assets/help/notified.md
      - mobile/src/main/assets/help/feedback.md
      - mobile/src/main/assets/help/faq.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/AboutContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/OperationContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/ReminderContentTest.kt
      - mobile/screenshots/calendar/（24 枚）
      - mobile/screenshots/reminder/（24 枚）
      - docs/USER_GUIDE.md
      - docs/SUPPORT.md
      - docs/design/WIREFRAME.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*AboutContentTest" --tests "*OperationContentTest" --tests "*CalendarContentTest"
      --tests "*ReminderContentTest" --tests "*ReminderViewModelTest" --tests "*AccessibilityTest"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true --tests "*MainScreensScreenshotTest.calendar*"、
      同じく --tests "*MainScreensScreenshotTest.reminder*"、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定のテスト・markdownlint・records・user-guide・secrets がすべて成功した
      （1 回目は ReminderContentTest の面のテストが、時刻のボタンで画面外に出た行を探せず失敗したため、一覧を送ってから探すよう直した）。
      memo のスクリーンショットは差分なし、calendar と reminder は記録し直し、extra_large でも題名と設定のボタンが重ならないことを画像で確かめた（フル検証はループの最後に流す）
    関連ID:
      - BL-143
- date: 2026-11-13 05:03
  summary: 背景の写真があるとき、記録と期限の画面の一覧にテーマの面を敷いて文字を読めるようにする
  details:
    変更内容: >-
      BL-130 を実装した。designsystem/ReadablePanel.kt を新設し、LocalHasBackgroundImage が true のときだけテーマの surface を不透明に敷いた Surface
      （左右 8dp、角 12dp、testTag は readable_panel）で中身を描く ReadablePanel を作った。LazyColumn の行ごとに敷いても 1 枚の面に見えるよう、
      PanelEdge（SINGLE・TOP・MIDDLE・BOTTOM）で上下の角と外側の余白を分け、panelEdge(index, count) で行の位置から選ぶ。
      面の中は Surface により文字色が onSurface に戻るため、ペンの色に関わらず CalendarColors・onSurfaceVariant・error・primary がテーマの面の上と同じに見える。
      写真が無いときは Column で並べるだけにして今の見た目を変えない（スクリーンショットの calendar・reminder は差分なし）。
      記録の画面は月の表（MonthHeader・MonthGrid）を 1 枚、日の見出し〜記録の行〜「買った物を記録する」を続いた 1 枚、検索の欄と結果の行に敷いた
      （検索結果の行は SearchResultRow に分けた）。期限の画面は期限が近い順の行、最近買った物の行と「ほか N 件を見る」、空のときの案内、
      「おしらせが届かないとき」に敷き、セクションの見出しはペンの色のまま写真の上に残した。ペンの色の仕組み（InkColor・AppThemeHost）は変えていない。
      使い方 background.md に面を敷くことを足して docs/USER_GUIDE.md を作り直し、DESIGN.md の F-61 の行を更新した。
      BL-143・BL-137・BL-109 の依存から BL-130 を外した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/ReadablePanel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/RecentSection.kt
      - mobile/src/main/assets/help/background.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/ReminderContentTest.kt
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*CalendarContentTest" --tests "*ReminderContentTest" --tests "*ThemeContrastTest" --tests "*InkColorTest"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.calendar*"、
      同じく --tests "*MainScreensScreenshotTest.reminder*"、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定のテスト・markdownlint・records・user-guide・secrets がすべて成功し、
      スクリーンショットの calendar と reminder は差分なしで成功した（フル検証はループの最後に流す）
    関連ID:
      - BL-130
- date: 2026-11-13 01:45
  summary: 期限の画面で期限が近い順を上に出し、最近買った物は 3 件だけ出して「期限なし」で一覧から外せるようにする
  details:
    変更内容: >-
      BL-142 を実装した。ReminderList の並びを 通知オフの帯 → 期限が近い順 → 最近買った物 → お知らせが来ないときは にした（期限切れの有無によらず常にこの順）。
      最近買った物の行と「ほか N 件を見る」を新しい reminder/RecentSection.kt に移し（ReminderScreen.kt の行数を抑えるため）、最初の 3 件だけ出して
      残りは「ほか N 件を見る」で全部出す（開いた状態は rememberSaveable だけで保存しない）。各行に「期限なし」（TextButton、48dp 以上）を足し、
      押すと ReminderEvent.NoDue で記録の id を ReminderPrefs の reminder_hidden_recent_ids（文字列の集合）へ保存して一覧から外し、
      「期限なしにしました：品名 ／ 元に戻す」を 5 秒出す（メモの UndoEffect と同じ作り。押すと UndoNoDue で id を外す）。記録は消さない。
      保存のたびに、その時点の最近買った物（7 日以内・期限未設定）に無い id を掃除する。読んで書くまでは 1 回の DataStore の edit で行う。
      ReminderViewModel は外した記録を除いた一覧で uiState.recent と G-08 の条件を作るため、G-08 は外した記録を除いた 1 行目で出る。
      G-08 で照らす場所は行全体から「期限を決める」のボタンに絞った（行に「期限なし」が並んだため）。
      設定の書き出し・読み込み（BackupSettingsStore）には外した id を含めない（7 日で消える一時的な値のため、既定のとおり）。
      データを全部消す（DataEraser）は DataStore を丸ごと消すため、外した id も消える。
      使い方 due.md に並びと「ほか ○ 件を見る」「期限なし」「元に戻す」を足して docs/USER_GUIDE.md を作り直し、
      DESIGN.md の F-50・F-53・G-07/G-08 の行と設定の保存方針を更新した。スクリーンショットの reminder 24 枚を記録し直した
      （並びが入れ替わり、最近買った物の行に「期限なし」が並ぶ。とても大きい文字では買った日が 2 行に折り返すが切れない）。
      あわせて BL-151 の確認項目に初めての案内の 3 枚目と最近買った物の行を足し、BL-151・BL-130 の依存から BL-142 を外した
    変更ファイル:
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/RecentSection.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/ReminderPrefs.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/assets/help/due.md
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/ReminderContentTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderViewModelTest.kt
      - mobile/screenshots/reminder/（24 枚）
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*ReminderContentTest" --tests "*ReminderViewModelTest"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true --tests "*MainScreensScreenshotTest.reminder*" --tests "*ScreenGuidesTest"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定のテスト・markdownlint・records・user-guide・secrets がすべて成功し、
      reminder のスクリーンショットの記録と ScreenGuidesTest も成功した（フル検証はループの最後に流す）
    関連ID:
      - BL-142
- date: 2026-11-13 01:33
  summary: 下タブと画面の名前を「カレンダー」「お知らせ」から中身に合わせた「記録」「期限」に変え、買い物のためのメモであることを案内と使い方に添える
  details:
    変更内容: >-
      BL-141 を実装した。strings.xml の tab_calendar を「記録」、tab_reminder を「期限」にし（上部のバーの題名も同じ文字列で変わる）、
      ショートカットを「記録」「期限」と長い名前「買った物の記録を開く」「期限の一覧を開く」に、設定の「ホーム画面に置く」の説明と
      通知の本文（押すと、期限の一覧を開きます）を新しい名前に合わせた。初めての案内の 3 枚目の本文と使い方の start.md に
      「予定表やなんでもリマインダーではなく、買い物のためのメモです」を添えた。使い方は calendar.md の見出しを「記録」、目次の章を「期限とお知らせ」にし、
      due.md・shortcuts.md・add-later.md・edit-record.md・search.md・not-arriving.md の画面を指す箇所を直して docs/USER_GUIDE.md を作り直した。
      通知そのものを指す「お知らせ」（チャンネル名・お知らせの時刻・お知らせが来ないときは・設定の「お知らせ」・notified.md）は変えていない。
      テストの目印（TopLevelTabTags）と route、アイコンは変えていない。AppShortcutsTest・HelpContentTest・HelpScreenTest の名前を直した
      （OnboardingTest には 3 枚目の文言のテストが無いため変えていない）。スクリーンショットの calendar と reminder を記録し直し、
      48 枚とも差分が上部のバーの題名の範囲だけであることを確かめた。memo は verify で変わらないことを確かめた。
      DESIGN.md の PLAN §3・F-74・F-70/G-01 の行と UI/UX 方針、docs/design/WIREFRAME.md のタブと画面の名前を合わせた
    変更ファイル:
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppShortcuts.kt
      - mobile/src/main/assets/help/start.md
      - mobile/src/main/assets/help/index.md
      - mobile/src/main/assets/help/calendar.md
      - mobile/src/main/assets/help/due.md
      - mobile/src/main/assets/help/shortcuts.md
      - mobile/src/main/assets/help/add-later.md
      - mobile/src/main/assets/help/edit-record.md
      - mobile/src/main/assets/help/search.md
      - mobile/src/main/assets/help/not-arriving.md
      - mobile/src/test/kotlin/com/ikumemo/mobile/AppShortcutsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/help/HelpContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/help/HelpScreenTest.kt
      - mobile/screenshots/calendar/（24 枚）
      - mobile/screenshots/reminder/（24 枚）
      - docs/USER_GUIDE.md
      - docs/design/WIREFRAME.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*AppShortcutsTest" --tests "*HelpContentTest" --tests "*HelpScreenTest" --tests "*OnboardingTest"、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true（calendar と reminder を 1 画面ずつ）、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は ktlint・detekt・compile・指定のテスト・markdownlint・records・user-guide・secrets がすべて成功し、
      スクリーンショットの記録と memo の verify も成功した（フル検証はループの最後に流す）
    関連ID:
      - BL-141
- date: 2026-11-13 01:25
  summary: カレンダーとおしらせの画面に、画面の操作を確かめる Compose のテスト（Robolectric）を足す
  details:
    変更内容: >-
      BL-116 を実装した。mobile/src/testDebug に calendar/CalendarContentTest.kt（5 件）と reminder/ReminderContentTest.kt（7 件）を新設した。
      公開されている CalendarContent と ReminderContent に状態を渡して描き、日を押すと SelectDate、記録の行を押すと EditRecord、
      検索結果を押すと JumpTo が出ること、買い物の無い日と検索で見つからないときの文言を確かめる。おしらせは 使い切った と 捨てた のイベント、
      期限が近い順の行を押すと OpenDue（期限つき）、最近買った物の 期限を決める で OpenDue、通知オフの帯の表示と 通知をオンにする の呼び出し、
      通知オンでは帯が無いこと、困ったときの入口がヘルプの NOT_ARRIVING を開くこと、空の一覧の案内を確かめる。
      後続のタスク（BL-142・BL-130・BL-143・BL-137 など）が文言やレイアウトを変えても壊れにくいよう、要素はテストタグ（CalendarTestTags・ReminderTestTags）で探し、
      文言は文字列リソースから context.getString で引く（表示文字列を直書きしない）。状態は同じファイル内の CalendarSamples・ReminderSamples の
      既定値つきの関数（state・row・recent・pending）で組み立て、描くのは各クラスの show に寄せた。ガイドは showGuide を false にして出さない。
      本体のコードは変えていない
    変更ファイル:
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/calendar/CalendarContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/reminder/ReminderContentTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*CalendarContentTest" --tests "*ReminderContentTest"、
      python scripts/quality-gates.py（フル検証、直前に ./gradlew --stop）、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証で CalendarContentTest 5 件・ReminderContentTest 7 件がすべて通り、フル検証は ktlint・detekt・lint・test・build・
      markdownlint・records・user-guide・secrets の全ゲートが成功した
    関連ID:
      - BL-116
- date: 2026-11-13 01:06
  summary: Claude Code の権限にゲート系と読み取り系のコマンドの許可を足し、git push と --no-verify を permissions.deny と PreToolUse フックの 2 段で止める
  details:
    変更内容: >-
      オーナーが 2026-11-13 に承認した BL-122 を実装した。.claude/settings.json の permissions.allow に、既存の記録ファイル 3 つの Edit と
      docs/records の Read を残したまま、./gradlew の品質ゲート（--stop・ktlintCheck・detekt・lintDebug・testDebugUnitTest・:core:test・
      :mobile:testDebugUnitTest・assembleDebug）、python scripts の quality-gates.py・validate-records.py・make-user-guide.py --check・
      scan-secrets.py・guard-git-hook.py --self-test、npx markdownlint-cli2、git status・git diff・git log・git branch --show-current を
      Bash と PowerShell の両方で足した。git add と git commit の許可は足していない。
      permissions.deny には git push *、git * --no-verify（末尾と途中の 2 形）、git commit -n * を Bash と PowerShell の両方で足した。
      2 段目として scripts/guard-git-hook.py を作り、hooks.PreToolUse（matcher は Bash|PowerShell、exec 形式で python に渡す、timeout 10 秒）に繋いだ。
      フックは tool_input.command を shlex で字句に分け（PowerShell はバッククォートを escape にし、ヒアストリングは先に空にする。
      bash のヒアドキュメントの本文も先に除く）、区切り（; && || | & 改行など）ごとにコマンドの位置の語だけを見る
      （先頭の変数代入と sudo・env・timeout などの前置きは読み飛ばし、引数の中の git は見ない）。その語が git なら大域オプション
      （-C・-c・--git-dir など）を読み飛ばしてサブコマンドが push か、--no-verify があるか、git commit の -n（-nm のようなまとめた形も含む）かを見る。
      bash -c・pwsh -Command の中も 3 段まで調べる。
      最初の版は区切りの中のどの位置の git も見ていたため、ヒアドキュメントで渡したコミットメッセージ本文の git push を拾って自分のコミットを止めた。
      settings.json をいったん戻し、コマンドの位置だけを見る判定とヒアドキュメントの本文の除外に直し、その例を self-test に足してから繋ぎ直した。
      止めるときは終了コード 2 と日本語 1 行の理由（push はユーザーが実行する旨）を標準エラーへ返し、入力が読めない・JSON でない・
      想定外の例外ではすべて終了コード 0 で通す。引用符の対応が取れない入力は、引用符を含まない素直な書き方だけを正規表現で見る。
      設定の書式は公式ドキュメント（code.claude.com/docs の hooks と permissions）で確かめた
    変更ファイル:
      - .claude/settings.json
      - scripts/guard-git-hook.py
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/guard-git-hook.py --self-test、標準入力へサンプルの PreToolUse の JSON を渡す確認（スクラッチの drive_hook.py）、
      設定を繋いだ後の無害なコマンド（Bash と PowerShell の git status --short、python scripts/quality-gates.py --skip-gradle）、
      本文に git push の語を含む cat のヒアドキュメントと PowerShell のヒアストリング、deny の届かない bash -c と pwsh -Command の中の
      git --version --no-verify（push しない無害な発火の確認）、python scripts/scan-secrets.py、本記録を含むコミット（ヒアドキュメントのメッセージ）
    検証結果: >-
      成功 - self-test は止める例 22 件と通す例 19 件がすべて期待どおり。標準入力の確認は git push・git push --force origin main・
      git commit --no-verify -m x・git -C path push が終了コード 2、git status・git log --oneline・quality-gates.py --quick・./gradlew ktlintCheck・
      push の語を含むコミットメッセージと grep が終了コード 0、空・壊れた JSON・不正なバイト列・command の欠けた入力も終了コード 0。
      繋ぎ直した後は無害なコマンドと git push の語を含むヒアドキュメント・ヒアストリングが Bash・PowerShell の両方で通り、
      bash -c と pwsh -Command の中の --no-verify は両方でフックが日本語の理由を出して止めた。
      git push そのものは実行していない
    関連ID:
      - BL-122
- date: 2026-11-13 00:58
  summary: 記録ファイルの検証に、BACKLOG の id の重複・区分・担当、EXECUTE の必須キーと date の形式、採番の衝突の検査を足す
  details:
    変更内容: >-
      scripts/validate-records.py に FORMAT.md の定めに沿った検査を足した。BACKLOG.md は id の重複、区分 が表の 6 値のどれか、担当 が空でないこと。
      EXECUTE.md は必須キー（date・summary・details の 変更内容・変更ファイル・検証コマンド・検証結果）、date の形式 YYYY-MM-DD HH:mm、
      変更ファイル と 関連ID が配列であること。あわせて BACKLOG.md の id が EXECUTE.md の 関連ID と重ならないこと（採番の衝突）を見る。
      状態が 進行中 の項目は途中までの実施を記録したものとして衝突の検査から除いた（既定値として選択）。
      既存の記録では、人手検証として起票した id を 関連ID に入れていた 3 件（BL-106・BL-109・BL-111）が衝突に掛かった。
      関連ID は対応したタスクの id と定めているため、これらの記録の 関連ID から起票した id を外した（ログは削除していない。検証結果の本文には残っている）。
      モジュール先頭の docstring の検査の一覧も更新した
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
      - scripts/validate-records.py
    検証コマンド: >-
      python scripts/validate-records.py、わざと壊した一時コピー（重複 id・区分の誤り・担当の空・date の形式違い・details のキー欠け・採番の衝突）での同スクリプト、
      python scripts/quality-gates.py --skip-gradle、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 今の記録で終了コード 0、壊した 6 種の一時コピーはいずれも該当の NG を出して終了コード 1。一時コピーはスクラッチの場所に作り削除した
    関連ID:
      - BL-120
- date: 2026-11-13 00:55
  summary: 品質ゲートに軽い検証のモードを足し、自律ループの各イテレーションは軽い検証、最後と止める前にフル検証を流す運用にする
  details:
    変更内容: >-
      オーナーの判断（2026-11-13）で起票した BL-129 を実装した。scripts/quality-gates.py に --quick と --tests（くり返し可）を足した。
      軽い検証は Gradle を ktlintCheck・detekt・コンパイル（:core:compileTestKotlin と :mobile:compileDebugUnitTestKotlin）と、
      --tests で渡したテストだけにし、Android Lint と assembleDebug は SKIP（軽い検証）、--tests が無ければ test を SKIP（--tests の指定なし）と表に出す。
      --tests のパターンはクラス名と同じ名前の .kt を core/src/test と mobile/src/test・testDebug から探して :core:test か
      :mobile:testDebugUnitTest へ振り分け、見つからなければ test を失敗にする。最後の行は「軽い検証の結果」と
      「フル検証ではない」を出す。run_gradle はゲートの表と受け皿のゲートを引数で受けるようにし、引数なし・--only・--skip-gradle の挙動は変えていない。
      --quick と --only・--skip-gradle の併用、--quick なしの --tests は引数の誤り（終了コード 2）。
      手順は CLAUDE.md「本リポジトリの品質ゲート定義」にいつ何を流すかを正本として足し、autonomous-loop のイテレーション手順 4・委任の指示・
      完了条件（最後と止める前にフル検証、失敗したら原因のコミットを特定して直すイテレーションを足す）と、backlog-task-implementer の検証の節
      （フル検証は指示されたときだけ、180 秒でバックグラウンドへ移ったら待って 2 つ目を起動しない、無関係な失敗は 1 回だけ流し直す、
      確かめなかった完了条件を報告に書く）を直した。DESIGN.md の品質ゲートの段落にも反映した。
      所要時間は、軽い検証（--tests "*DataViewModelTest"）がコマンド全体で 36 秒（Gradle 32 秒）。コンパイルは UP-TO-DATE で、
      テストのタスクは実際に流れた。フル検証は流しておらず、比較は BL-113 の 396 秒・269 秒
    変更ファイル:
      - scripts/quality-gates.py
      - CLAUDE.md
      - .claude/skills/autonomous-loop/SKILL.md
      - .claude/agents/backlog-task-implementer.md
      - CHANGELOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      python scripts/quality-gates.py --quick --tests "*DataViewModelTest"、
      python scripts/quality-gates.py --quick --tests "*NoSuchZzTest" --tests "*DataViewModelTest.zzNoSuchMethod"、
      python scripts/quality-gates.py --quick、python scripts/quality-gates.py --skip-gradle、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は終了コード 0 で lint・build が SKIP（軽い検証）。存在しないテストのクラスと一致しないフィルタを渡すと
      test が NG で原因（見つからないクラス、No tests found）が出て終了コード 1。--tests なしは test が SKIP（--tests の指定なし）で終了コード 0。
      --tests だけ、--quick と --only の併用は終了コード 2。--skip-gradle は終了コード 0。フル検証は流していない
    関連ID:
      - BL-129
- date: 2026-11-13 00:50
  summary: 読み上げでカレンダーの選択中の日・質問の開閉・おしらせの行の操作が伝わるようにする
  details:
    変更内容: >-
      calendar/MonthGrid.kt の DayCell の semantics に selected を足し、選ばれている日を読み上げで「選択中」と伝える。
      settings/FeedbackScreen.kt の困ったときの質問に、開閉の状態（stateDescription「答えを表示中」「答えは閉じています」）と
      押したときの意味（onClickLabel「答えを開く」「答えを閉じる」）を付けた。reminder/ReminderScreen.kt の PendingRow の clickable に
      onClickLabel「期限を決め直す」を付けた。文言は strings.xml に 5 件足した。見た目は変えていない。
      AccessibilityTest に 3 件の検査（選択中の日の selected、質問の状態と操作の文言が開閉で切り替わること、期限が近い順の行の操作の文言）を足した。
      DESIGN.md の「PLAN §8 アクセシビリティ」の行に反映した。既定値で決めた点は、選択中の表し方を selectable でなく semantics の selected にしたこと
      （押したときの処理は既存の clickable のまま）と、質問の状態・操作の文言
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/FeedbackScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/AccessibilityTest.kt
    検証コマンド: >-
      python scripts/quality-gates.py、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - quality-gates.py は 1 回目で Gradle の 5 ゲートと markdownlint・records・user-guide・secrets がすべて成功し、
      AccessibilityTest は 6 件すべて成功。見た目を変えていないためスクリーンショットテストは流していない
    関連ID:
      - BL-115
- date: 2026-11-13 00:43
  summary: データの書き出し・読み込みでファイルを開けないときにアプリが落ちないようにする
  details:
    変更内容: >-
      settings/DataViewModel.kt の export・import を、IOException に加えて SecurityException と IllegalArgumentException も捕まえる
      orWhenUnreadable にまとめ、それぞれ EXPORT_FAILED・IMPORT_FAILED を返すようにした（ContentResolver が選択元のアプリの都合や
      権限の取り消しで投げうるため。CancellationException は IllegalStateException の仲間で捕まえない）。DataViewModelTest を新設し、
      処理中に重ねて押した書き出し・読み込みを無視すること、開く関数が null・SecurityException・IllegalArgumentException のときの失敗の知らせ、
      書き出し・読み込み・消去の成功の知らせを、インメモリ DB と本物の BackupExporter・BackupImporter・DataEraser で確かめる。
      状態は Turbine で 1 件ずつ追わず、StateFlow を実時間で結果が出るまで待つ書き方にした（負荷による時間切れを避けるため）。
      DESIGN.md の F-72 の行に、開けないときの扱いを足した。既定値で決めた点は、捕まえる例外をタスクに挙がった 2 種と既存の IOException に限ったこと
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataViewModel.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/DataViewModelTest.kt
    検証コマンド: >-
      ./gradlew :mobile:testDebugUnitTest --tests "*DataViewModelTest*"、python scripts/quality-gates.py、
      python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - DataViewModelTest の 4 件が成功。quality-gates.py は 1 回目で Gradle の 5 ゲートと markdownlint・records・user-guide・secrets がすべて成功
    関連ID:
      - BL-114
- date: 2026-11-13 00:36
  summary: 写真の取り込みで元の写真が読めないときにアプリが落ちないようにする
  details:
    変更内容: >-
      data/PhotoStore.kt の import で、openInputStream が投げる IOException（FileNotFoundException を含む）と SecurityException、
      save の check が投げる IllegalStateException を捕まえて null を返すようにした。null は既存の「読めない画像」と同じ扱いで、
      呼び出し側（EditItemSheet.kt の PhotoSection、AppearanceViewModel の PickBackground）は何もしない。
      detekt の TooManyFunctions（クラス内の関数 11 個が上限）に当たったため、関数を分けずに import の中で try を書き、
      途中の return を ?.let のつなぎにした（ReturnCount も避ける）。PhotoStoreTest に存在しないファイルの Uri で null が返るテストを足し、
      DESIGN.md の F-07 の行に読めないときの扱いを足した。既定値で決めた点は 2 つ。(1) 捕まえる例外はタスクに挙がった 3 種に限り、
      OutOfMemoryError などは捕まえない。(2) 保存の失敗で残りうる一時ファイル（.tmp）の掃除はタスクの範囲外として足していない
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoStore.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/PhotoStoreTest.kt
    検証コマンド: >-
      python scripts/quality-gates.py（2 回）、./gradlew :core:runKtlintCheckOverMainSourceSet --rerun :core:ktlintMainSourceSetCheck、
      python scripts/quality-gates.py --only ktlint、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 1 回目は ktlint・detekt・test が失敗。detekt は今回の変更（TooManyFunctions）で、直した。test は AppearanceViewModelTest の
      scrimLevelIsSavedWithinRange の Turbine のタイムアウトで、PhotoStore を通らないテストのため負荷による揺らぎと判断し、2 回目は成功。
      ktlint は 2 回とも存在しないファイル core の ZzGateTmp.kt の違反で失敗し、ビルドキャッシュから復元された古い結果だった（BL-128 に起票）。
      --rerun で該当タスクを流し直した後の --only ktlint が成功。2 回目は ktlint 以外の Gradle のゲートと markdownlint・records・user-guide・secrets が成功
    関連ID:
      - BL-113
- date: 2026-11-13 00:16
  summary: 品質ゲートの一括実行スクリプトと、コミット前の秘密情報スキャンを足す
  details:
    変更内容: >-
      scripts/quality-gates.py を新設した。CLAUDE.md の品質ゲートの表と同じコマンドを順に流す（./gradlew --stop の後に Gradle の 5 ゲートを
      --continue 付きの 1 回の呼び出しで流し、失敗したタスク名からゲートごとの合否を出す。markdownlint は Summary 0 issues と Linting の件数を
      git ls-files の *.md の件数と照合。validate-records・make-user-guide --check・scan-secrets）。出力は成功したゲートが 1 行、失敗したゲートは
      原因の抜粋（ktlint・detekt・コンパイル・テストの行と What went wrong の節、最大 25 行、パスはリポジトリからの相対）、最後に合否と所要時間の表で、
      全文は build/quality-gates/ のログに残す。--skip-gradle・--only・--list を受け付け、Windows では gradlew.bat を使い、出力は UTF-8 にそろえる
      （Git Bash で文字化けしたため）。scripts/scan-secrets.py を新設した。変更行と未追跡のファイルから秘密鍵の見出し・Google API キー・
      GitHub トークン・AWS アクセスキー・署名のパスワードの値を、追跡中と未追跡のファイル名から署名鍵・資格情報のファイルを検出し、
      ファイルと行と種類だけを出して値は出さない。明らかなダミー値・コードで読み込む書き方・行に scan-secrets の allow 印のある行は数えず、
      自身のパターンは検出対象の文字列がソースに現れないよう組み立てた。CLAUDE.md の品質ゲートの表に 2 行と一括実行の説明を足し、
      autonomous-loop の手順 4 と 7 から参照した。既定値で決めた点は 3 つ。(1) スキャンの既定の範囲は BACKLOG の案の git diff --cached ではなく
      git diff HEAD（ステージ済みと未ステージの両方）とし、--staged で案どおりの範囲にした。git add の前に流せ、範囲が案を含むため安全側。
      (2) BACKLOG の案の「伏せ字で出す」は、値を一切出さない形にした（より漏れにくいため）。(3) 対象のファイル名に secrets.properties・.env・
      *.p12・*.pfx を足した（.gitignore が除外している資格情報のファイルと揃えるため）
    変更ファイル:
      - .claude/skills/autonomous-loop/SKILL.md
      - CHANGELOG.md
      - CLAUDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - scripts/quality-gates.py
      - scripts/scan-secrets.py
    検証コマンド: >-
      python scripts/quality-gates.py --skip-gradle、python scripts/quality-gates.py --only ktlint（PowerShell から）、
      一時的に置いた書式違反の Markdown と Kotlin のファイルで --only markdownlint,records と --only ktlint（失敗の表示の確認、ファイルは削除済み）、
      --only に知らない名前を渡す確認、スクラッチの一時リポジトリでの python scripts/scan-secrets.py（検出と非検出）、
      python scripts/validate-records.py、npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - --skip-gradle は 4 ゲート成功で終了コード 0。ktlint だけの実行は成功で終了コード 0、書式違反を置くと NG と違反の行だけが出て終了コード 1。
      markdownlint の違反は該当行と Summary だけが出て終了コード 1、知らないゲート名は終了コード 2。秘密情報スキャンは偽の秘密鍵の見出し・
      Google API キー・GitHub トークン・署名のパスワード・*.jks を検出して終了コード 1（値は出ない）、ダミー値・getProperty での読み込み・
      説明文・allow 印の行は検出せず終了コード 0。Gradle の全ゲートの一括実行は、PC のメモリが少ないためオーケストレータの指示で今回は流していない
      （Gradle のゲートはスクリプト経由で ktlint だけを確認。コードの変更は無い）。次に全ゲートを流すイテレーションで確かめる
    関連ID:
      - BL-119
- date: 2026-10-25 22:29
  summary: ウィジェットの色と角の丸みを本体のテーマに合わせる
  details:
    変更内容: >-
      本体の画面は background と onBackground、テーマごとの角の丸み（CornerSet）で描いているのに、ウィジェットは surface と固定の 16dp だった。
      WidgetColors に background・onBackground と corners（テーマの角の丸み）を足し、メモ一覧は background と onBackground、角は large、見出しの
      「いくメモ」は本体のタイトルと同じ文字色（onBackground）にした。1×1 の角は medium。色の値そのものは本体のパレットと同じものを
      ライト・ダークとも使っている。背景の写真とペンの色はウィジェットには使わない（既存の方針）
    変更ファイル:
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/VoiceWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetColors.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 既存のウィジェットのテストが通る。実機での見た目の比較は BL-106 に含めて確認する
    関連ID:
      - BL-112
- date: 2026-10-25 22:24
  summary: ウィジェットの更新中に回る表示を出す。1×1 の「話して追加」で足したメモが 4×4 に反映されない不具合を直す
  details:
    変更内容: >-
      原因は Glance のセッションの再利用だった。メモ一覧ウィジェットは provideGlance でメモと色を 1 回だけ読んでいたため、表示中のセッションが
      生きている間は updateAll や更新ボタンを押しても内容が古いままだった（反映の時機がまちまちだった理由でもある）。provideContent の中で
      MemoWidgetState.observe・WidgetColors.observe（新設）を collectAsState して、最新の値で描き直すようにした。1×1 の色も同様。
      更新ボタンは押すと矢印を回る表示（CircularProgressIndicator）に替え、WidgetRefreshState（新設）で状態を持ち、描き直しが一瞬で終わっても
      0.8 秒は回す
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/VoiceWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetColors.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetRefreshState.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - MemoListWidgetTest に更新中の表示の 1 件を追加。Glance のセッションの挙動は単体テストで再現できないため、
      実機での反映の確認は人手検証（BL-111）
    関連ID:
      - BL-110
- date: 2026-10-25 21:08
  summary: 背景の写真の上の文字が見えないときのために、ペンの色（おまかせ・くろ・あお・あか・しろ）を選べるようにする
  details:
    変更内容: >-
      きせかえの「背景の写真」に「文字の色（ペンの色）」を足した。選ぶと写真の上に直接置く文字（onBackground と LocalContentColor）をその色にし、
      色に合わせて膜の色も変える（くろ・あお・あかは明るい膜、しろは暗い膜。文字との比は 4.5:1 以上）。暗い写真でテーマの黒い文字が沈む問題は、
      しろを選べば解消する。背景の写真が無いときと おまかせ のときは今までどおり。保存キーは appearance_ink で、データの書き出し・読み込みの設定にも
      入れた（古いファイルは おまかせ に戻る）。メモの行・バー・下タブなどテーマの面の上の文字は変えない。
      detekt の指摘（メンバー名が型名と同じ）は名前を変えて解消した（設定は変えない）。使い方・DESIGN・WIREFRAME に反映した
    変更ファイル:
      - docs/USER_GUIDE.md
      - docs/design/WIREFRAME.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/assets/help/background.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppearancePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupSettingsStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/BackgroundScrim.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/InkColor.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/designsystem/InkColorTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - 追加は InkColorTest 3 件（各ペンの色と膜の比が 4.5:1 以上、おまかせは色を変えない、未知の保存値は おまかせ）と
      AppearanceViewModelTest 1 件（ペンの色の保存）。FlowRow の実験的 API の注釈を足した。スクリーンショットテストの対象画面（メモ・カレンダー・
      おしらせ）は変わらないため流していない。実機での全画面の見え方は人手検証（BL-109）
    関連ID:
      - BL-108
- date: 2026-10-25 20:57
  summary: メモ一覧ウィジェットをメモ帳風にする（見出しの下の線・行ごとの罫線・左の余白の縦線）
  details:
    変更内容: >-
      裏紙のような平らな表示に、メモ帳の線を足した。見出しの下にテーマ色の 2dp の線、大きい表示の各行の下にうすい 1dp の横罫線（onSurfaceVariant の
      透明度 0.35、WidgetColors.rule）、一覧の左に 2dp の余白の縦線。色はテーマと暗い画面に追従する。1 項目の根を 1 つにするため行と罫線を Column にまとめた。
      レイアウトの指定は PLAN・設計書に無かったため、この形を既定として選んだ（DESIGN に記録）
    変更ファイル:
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetColors.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 既存の MemoListWidgetTest（文言・タップ先）が通る。線の見え方は実機での確認が要る（BL-106 に含めて確認する）
    関連ID:
      - BL-107
- date: 2026-10-25 18:35
  summary: メモ一覧ウィジェットの見出しに更新ボタンを追加（メモを足した直後に反映されないときに、押してその場で描き直せる）
  details:
    変更内容: >-
      メモ一覧ウィジェット（MemoListWidget）の見出しの右端に、48dp の更新ボタン（Material の refresh 形、テーマ色）を足した。押すと RefreshMemoAction
      （Glance の ActionCallback）が WidgetUpdater.refresh を呼び、メモ一覧と 1×1 の両方を描き直す。空のメモでも、どの大きさでも出す。
      読み上げ用に「メモ一覧を更新」の説明を付けた。使い方の「2 つのウィジェット」と DESIGN の W-02 に反映した
    変更ファイル:
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - mobile/src/main/assets/help/widgets.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/MemoListWidget.kt
      - mobile/src/main/res/drawable/ic_widget_refresh.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/MemoListWidgetTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - MemoListWidgetTest に更新ボタンの有無（小・大・空）の 1 件を追加。途中で ktlint の import 順を ktlintFormat で直し、
      glance の hasRunCallbackClickAction が internal だったため説明文での照合に変えた。幅が狭いときの見出しの収まりと実機の描き直しは
      人手検証（BL-106）
    関連ID:
      - BL-105
- date: 2026-10-25 20:10
  summary: データの書き出し・読み込み（F-72）。ZIP（写真つき）・JSON・CSV（記録だけ）で書き出し、ZIP と JSON を読み込んで置き換える
  details:
    変更内容: >-
      設定の「データ」に「データを書き出す（写真つき）」「データを書き出す（写真なし）」「買った記録を表計算用に書き出す」「データを読み込む」を足した。
      DB の全表を ID ごと写す書き出しファイル（BackupFile、版 1）を JSON で作り、ZIP には写真と背景画像を同じ相対パスで入れる。CSV は買った記録だけ
      （BOM 付き、式になる先頭文字は無害化）。読み込みは外から来たファイルとして一時フォルダへ取り出し、BackupValidator が版・ID の重複・外部キー・
      写真のパス・日付の範囲を検査し、通ったときだけ 1 つのトランザクションで入れ替える（大きさにも上限）。ZIP の決まった名前以外の項目は読み飛ばす。
      detekt の関数数と引数の上限に収めるため、DAO を読む・入れる・消すの 3 つに分け、設定の読み書きを BackupSettingsStore、変換を 2 ファイル、
      上限つきコピーを LimitedIo に分けた（detekt の設定は変えない）。使い方に「データの書き出しと読み込み」を足し、機種変更の案内・SUPPORT・
      プライバシーポリシー・DESIGN・WIREFRAME・UI デザイン設計書に反映した
    変更ファイル:
      - docs/SUPPORT.md
      - docs/USER_GUIDE.md
      - docs/design/UI_DESIGN.html
      - docs/design/WIREFRAME.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/store/PRIVACY_POLICY.md
      - mobile/src/main/assets/help/data-export.md
      - mobile/src/main/assets/help/device-change.md
      - mobile/src/main/assets/help/index.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupExportMapping.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupExporter.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupImportMapping.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupImporter.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupSettingsStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/BackupValidator.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/LimitedIo.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/backup/RecordCsv.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/BackupDao.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/db/IkumemoDatabase.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/help/HelpScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/BackupRoundTripTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/BackupValidatorTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/backup/RecordCsvTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/help/HelpContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest.kt
      - scripts/ui-doc/gen_ui_doc.py
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - 追加は BackupRoundTripTest 7 件（ZIP で DB・写真・背景画像・設定が戻る、JSON は写真だけ外れる、読み込みで置き換わる、壊れたファイルと
      壊れた参照を拒否して今のデータが変わらない、ZIP の範囲外の名前を無視する、CSV）、BackupValidatorTest 6 件、RecordCsvTest 4 件、DataContentTest 5 件。
      途中で detekt の指摘（関数数・引数の数・入れ子・return の数）を分割で解消し、使い方の「やってみる」の行き先に「データ」を足した。BUILD SUCCESSFUL。
      実機のファイル選択（Storage Access Framework）での確認は人手検証
    関連ID:
      - BL-102
- date: 2026-10-25 18:30
  summary: データをすべて消す（F-76・R-43）。設定に「データ」を足し、確認のあとで記録・写真・設定を消して初めて使う状態に戻す
  details:
    変更内容: >-
      設定に「データ」を足し、「データをすべて消す」を置いた。確認を 1 回だけ出し（取り消せない操作の例外）、DataEraser が
      DB（clearAllTables）・設定（DataStore）・写真と背景画像のファイルを消す。消したあとは通知と予約を片付け、ウィジェットを空の表示に更新する。
      消し終えたら画面に知らせを出す。初めての案内の保存値も消えるため、案内がまた出る。プライバシーポリシー・DESIGN・
      WIREFRAME・UI デザイン設計書に反映した
    変更ファイル:
      - docs/design/UI_DESIGN.html
      - docs/design/WIREFRAME.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/store/PRIVACY_POLICY.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/DataEraser.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/DataViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/DataEraserTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/DataContentTest.kt
      - scripts/ui-doc/gen_ui_doc.py
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - detekt の MatchingDeclarationName でテストタグの object をファイル末尾へ移して解消。追加は DataEraserTest 2 件（DB・設定・写真・背景画像が消える、
      何も無くても動く）と DataContentTest 3 件（確認を出してから消す、やめれば何も起きない、消し終えた知らせ）。BUILD SUCCESSFUL
    関連ID:
      - BL-103
- date: 2026-10-25 17:10
  summary: 平仮名だけの表示文字列のうち意図のないものを漢字へ置き換え（お知らせ・設定・着せ替え・操作・買い物メモなど）
  details:
    変更内容: >-
      想定ユーザーが大人のため（オーナー指示）、strings.xml・アプリ内の使い方（assets/help/）・利用者向けドキュメント・
      UI デザイン設計書の生成スクリプトと、文言を検査するテストの平仮名を漢字へ改めた。置き換えたのは
      おしらせ→お知らせ、せってい→設定、きせかえ→着せ替え、そうさ→操作、かいものメモ→買い物メモ、もどる→戻る、あした→明日、きょう→今日、
      さがす→探す、れんしゅう→練習、わかった→分かった、あとで→後で、はじめて→初めて など。「いつもの」「やめた」「いくメモ」
      テーマ名「やさしい」、品名の例「たまご」は意図のある名前のため残した。PLAN.md・EXECUTE.md・CHANGELOG.md の過去の記述は履歴のため変えない。
      DESIGN の UI/UX 方針に表記の方針を足した。メモ・おしらせ画面のスクリーンショットの正解画像を記録し直した（カレンダーは変化なし）
    変更ファイル:
      - README.md
      - docs/CLOSED_TEST.md
      - docs/FEEDBACK.md
      - docs/RELEASE_NOTES.md
      - docs/SUPPORT.md
      - docs/USER_GUIDE.md
      - docs/design/UI_DESIGN.html
      - docs/design/WIREFRAME.md
      - docs/records/managed/DESIGN.md
      - docs/store/PRIVACY_POLICY.md
      - docs/store/STORE_LISTING.md
      - mobile/src/main/assets/help/add-later.md
      - mobile/src/main/assets/help/background.md
      - mobile/src/main/assets/help/bought-button.md
      - mobile/src/main/assets/help/dark.md
      - mobile/src/main/assets/help/device-change.md
      - mobile/src/main/assets/help/due.md
      - mobile/src/main/assets/help/faq.md
      - mobile/src/main/assets/help/feedback.md
      - mobile/src/main/assets/help/index.md
      - mobile/src/main/assets/help/not-arriving.md
      - mobile/src/main/assets/help/notified.md
      - mobile/src/main/assets/help/resize.md
      - mobile/src/main/assets/help/search.md
      - mobile/src/main/assets/help/shortcuts.md
      - mobile/src/main/assets/help/start.md
      - mobile/src/main/assets/help/store.md
      - mobile/src/main/assets/help/swipe.md
      - mobile/src/main/assets/help/text-size.md
      - mobile/src/main/assets/help/theme.md
      - mobile/src/main/assets/help/usual.md
      - mobile/src/main/assets/help/widgets.md
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/AppShortcutsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/help/HelpContentTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/reminder/ReminderSchedulerTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/GuideOverlayTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/OnboardingTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/help/HelpScreenTest.kt
      - scripts/make-user-guide.py
      - scripts/ui-doc/gen_ui_doc.py
      - mobile/screenshots/memo/*.png（24 枚）
      - mobile/screenshots/reminder/*.png（24 枚）
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"（calendar・reminder も同様）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - Gradle 系の5ゲートは BUILD SUCCESSFUL。スクリーンショットは記録し直した後、3 画面とも検証が成功
    関連ID:
      - BL-104
- date: 2026-10-25 16:39
  summary: 通知などから開いた直後はおしらせ・カレンダーでもガイドを出さない（R-52）、外から開いたあとのタブの切り替えの不具合を修正
  details:
    変更内容: >-
      IkumemoApp で通知・ウィジェット・ショートカットから開いた画面を覚え、その画面にいる間はおしらせ・カレンダーの吹き出しを出さないようにした
      （CalendarScreen・ReminderScreen の showGuide。表示済みにはしないので、ほかの画面へ移ってから自分で開くと出る）。ほかの画面へ移ったら忘れる。
      UI テストを書く中で、外から開くときに画面をメモの上に積んでいたため、そのあとメモのタブを押すとタブの戻る履歴の復元で開いた画面へ戻ってしまう
      不具合を見つけ、外から開くときも下タブと同じ切り替え方（navigateToTab）にした。DESIGN と UI デザイン設計書の所見を直した
    変更ファイル:
      - docs/design/UI_DESIGN.html
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/OpenedFromOutsideGuideTest.kt
      - scripts/ui-doc/gen_ui_doc.py
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - 途中で、外から開いたおしらせからメモのタブへ移れない現象をナビゲーションの状態の出力で調べ、上記の不具合と特定した
      （最初に疑った NavHost の画面一覧の作り直しは原因ではなかったため、その変更は戻した）。BUILD SUCCESSFUL（約 3 分 15 秒、単体テスト 237 件。
      追加は通知から開いたおしらせで G-08 を出さず、メモへ移ってから自分で開くと出る 1 件）、markdownlint 0 issues、記録ファイル OK、docs/USER_GUIDE.md OK
    関連ID:
      - BL-099
- date: 2026-10-25 16:16
  summary: はじめての案内 1 枚目と G-03 の手本のアニメーション（R-56 のアニメーションを減らす設定に対応）（M8）
  details:
    変更内容: >-
      はじめての案内 1 枚目に、「牛乳とたまご」が 2 つの品目に分かれる手本 VoiceSplitDemo を入れた（3 回くり返して分かれた形で止まる）。
      G-03 の間は、れんしゅう項目を右へ少し動かして戻す手本 SwipeNudge を 2 回見せて止まる。どちらも回数を決めて止まる動きにし、
      いつまでも動き続けない（UI テストの待ち合わせが止まらないようにするため）。端末の「アニメーションを減らす」
      （ANIMATOR_DURATION_SCALE が 0）では動かさず、案内は最初から分かれた形で出す。UI デザイン設計書の案内の絵と所見、
      DESIGN、リリースノートの「分かっている問題」を合わせて直した
    変更ファイル:
      - docs/RELEASE_NOTES.md
      - docs/design/UI_DESIGN.html
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/GuideDemos.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/OnboardingScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/GuideDemosTest.kt
      - scripts/ui-doc/gen_ui_doc.py
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check、
      python -I scripts/ui-doc/gen_ui_doc.py docs/design/UI_DESIGN.html とヘッドレスブラウザでの目視
    検証結果: >-
      成功 - BUILD SUCCESSFUL（約 3 分 20 秒、単体テスト 236 件。追加は案内の手本が話した文→分かれた形と動いて止まる・減らす設定では止まる・
      れんしゅう項目が右へ動く・減らす設定では動かない 4 件）、markdownlint 0 issues、記録ファイル OK、docs/USER_GUIDE.md OK、
      設計書の案内の絵を目視。メモ・カレンダー・おしらせの描画は変えていないためスクリーンショットの比較は流していない
    関連ID:
      - BL-098
- date: 2026-10-25 15:58
  summary: Baseline Profile をビルドに含め、リリースのサイズを確認（R8 は有効済み）（非機能要件）
  details:
    変更内容: >-
      release の R8（縮小・難読化）はすでに有効だった。Baseline Profile として、アプリのコード全体を対象にした広い規則を
      mobile/src/main/baseline-prof.txt に置き、Compose 経由で入っていた profileinstaller 1.3.1 を同じ版で明示的に依存に書いた。
      署名なしの assembleRelease で APK が約 3.2MB（上限 15MB）であること、APK に assets/dexopt/baseline.prof が入り、
      アプリの規則がライブラリの規則と合わせて入っていること（merged_art_profile 4374 行）を確かめた。署名鍵の設定（local.properties）は
      無く、本番の資格情報は使っていない。実機での Baseline Profile の作成と起動時間の計測は人手検証 BL-100 として起票した
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/src/main/baseline-prof.txt
    検証コマンド: >-
      ./gradlew :mobile:assembleRelease（署名なし）、unzip -l で APK の中身を確認、
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - assembleRelease BUILD SUCCESSFUL（約 8 分）、mobile-release-unsigned.apk 3,206,422 バイト、assets/dexopt/baseline.prof あり。
      全ゲート BUILD SUCCESSFUL（依存の追加で作り直しになり約 8 分、単体テスト 232 件）、markdownlint 0 issues、記録ファイル OK、docs/USER_GUIDE.md OK
    関連ID:
      - BL-089
- date: 2026-10-25 15:39
  summary: メモの行に読み上げ用の「買った」「やめた」の操作を追加（アクセシビリティの仕上げ）（M8）
  details:
    変更内容: >-
      メモの行（SwipeableItemRow）に CustomAccessibilityAction の「買った」「やめた」を付け、TalkBack の操作の一覧からスワイプせずに
      操作できるようにした（PLAN §8）。アイコンの説明を見直し、アイコンだけのボタン（前の月・次の月・戻る・○・マイクなど）と写真ありの印には
      説明があり、文字と並ぶアイコンは説明なしで正しいことを確かめた。「元に戻す」の表示時間（R-04）は実装済み。あわせて、前回追加した
      ShoppingListGuideTest の 1 件が、G-03 の保存より先に「買った」の結果が届くと失敗するため、保存されるまで待つように直した
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListGuideTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/shoppinglist/AccessibilityTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - 1 回目のゲートで ShoppingListGuideTest の 1 件が失敗（保存より先に結果が届くタイミング）。テストを直して再実行し
      BUILD SUCCESSFUL（約 2 分 30 秒、単体テスト 232 件。追加は行の「買った」「やめた」の操作・行のアイコンの説明・月の切り替えボタンの説明 3 件）、
      markdownlint 0 issues、記録ファイル OK、docs/USER_GUIDE.md OK。描画は変えていないためスクリーンショットの比較は流していない
    関連ID:
      - BL-085
- date: 2026-10-25 14:58
  summary: スクリーンショットテスト（Roborazzi）の導入と、品質ゲートのメモリ対策・カレンダーの暗い画面の色の不具合修正
  details:
    変更内容: >-
      Roborazzi 1.38.0（テスト専用）を入れ、メモ・カレンダー・おしらせを 4 テーマ × ライト/ダーク × 文字の大きさ 3 段階で記録する
      MainScreensScreenshotTest を追加した（遅いテスト扱い、正解画像 72 枚を mobile/screenshots/ に置く）。最初の記録で暗い画面なのに
      背景が白い画像になったため、アプリの Scaffold の代わりにテーマの背景色の Surface を敷くようにした。その画像で、端末が明るい設定のまま
      アプリだけ暗くするとカレンダーの日曜・土曜の数字とお店の点が明るい画面用の濃い色になる不具合を見つけ、IkumemoTheme が渡す
      LocalDarkTheme で明暗を決めるように直した（MonthGrid が isSystemInDarkTheme を見ていた）。品質ゲートが PC のメモリ不足で
      Claude Code に止められていたため、gradle.properties で Kotlin のコンパイルを Gradle の中で行い、並列の作業を 2 つまでにした。
      CLAUDE.md にスクリーンショットの比較・記録の手順（画面ごとに流す）と、Claude Code ではゲートを通常のコマンドで流す旨を足した
    変更ファイル:
      - CHANGELOG.md
      - CLAUDE.md
      - build.gradle.kts
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - gradle.properties
      - gradle/libs.versions.toml
      - mobile/build.gradle.kts
      - mobile/screenshots/（memo・calendar・reminder の 72 枚）
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/IkumemoTheme.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/designsystem/DarkThemeLocalTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/screenshot/MainScreensScreenshotTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.<画面>*"（memo・calendar・reminder）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - バックグラウンドでの記録・比較は 3 回メモリ不足で止められ、メモリ対策のあと全ゲート（--rerun-tasks、82 タスク）と画面ごとの記録は
      通ったが、比較が再び止められたため、オーナーの指示で通常のコマンドに切り替えて実行した。スクリーンショットの比較は 3 画面とも成功
      （結果はすべて unchanged、Java の合計は最大約 1.3GB）、全ゲート BUILD SUCCESSFUL（約 2 分 50 秒、単体テスト 229 件。追加は暗い画面の設定が
      テーマの外へ伝わる 1 件）、markdownlint 0 issues、記録ファイル OK、docs/USER_GUIDE.md OK
    関連ID:
      - BL-062
- date: 2026-10-25 14:02
  summary: 使い方（H-01〜H-03）と Web 版 docs/USER_GUIDE.md の生成（M8）
  details:
    変更内容: >-
      使い方の正本を mobile/src/main/assets/help/ の Markdown（目次 index.md と 27 ページ。見出し・段落・箇条書きと
      「<!-- try: 画面; guide: g02 -->」だけ）として書き、HelpParser・HelpRepository で読んで、せっていの一番上の「使い方」から
      目次と 1 ページずつ表示するようにした。各ページの下に「ガイドをもう一度見る」（GuidePrefs.showGuideAgain でそのガイドを未表示に戻して
      画面を開く）と「やってみる」、目次の先頭に「はじめての案内をもう一度見る」（showOnboardingAgain）を置いた（H-02）。メモが空の画面・
      音声が使えない機種の表示・おしらせ一覧の「おしらせが来ないときは」から関係するページへ移る（H-03）。scripts/make-user-guide.py で
      docs/USER_GUIDE.md を作り、--check で正本との一致を確かめる行を CLAUDE.md の品質ゲートに足した（CHANGELOG に記録）。
      未実装の「LINE などで送る」（F-08）は目次に載せず、画面の絵はスクリーンショットテスト（BL-062）が無いため入れていない
    変更ファイル:
      - CHANGELOG.md
      - CLAUDE.md
      - docs/USER_GUIDE.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/assets/help/（index.md と 27 ページ）
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/GuidePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/GuideViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/help/HelpContent.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/help/HelpScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/GuidePrefsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/help/HelpContentTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/help/HelpScreenTest.kt
      - scripts/make-user-guide.py
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/make-user-guide.py --check
    検証結果: >-
      成功 - 途中で detekt の MaxLineLength を直した。1 回目の全ゲートはメモリ不足で Claude Code に止められ、オーナーの指示した時刻に
      流し直して BUILD SUCCESSFUL（単体テスト 228 件。追加は目次の全ページが読める・G-02〜G-08 をどこかのページから出し直せる・
      困ったときの入口のページがある・読み方・無いページで落ちない 5 件、ガイドの出し直しと案内の出し直しの保存 2 件、ページのボタン 2 件、
      せってい→使い方→ガイドをもう一度見るでカレンダーが開く・空のメモから話して入れるページへ 2 件）、markdownlint 0 issues（56 ファイル）、
      記録ファイル OK、docs/USER_GUIDE.md OK
    関連ID:
      - BL-081
- date: 2026-10-25 11:32
  summary: カレンダーとおしらせのその場のガイド（G-07・G-08）（M8）
  details:
    変更内容: >-
      カレンダー・おしらせ画面用の ScreenGuides（保存と判定）を追加し、CalendarViewModel と ReminderViewModel にガイドを渡せるようにした
      （渡さなければ出さない）。G-07 は見ている月に買い物のあった日があるとき、その月の最初の買い物の日を照らす（検索中・記録のシート表示中は
      出さない。先に日付を押したら出さない）。G-08 は最近買った物があるとき一番上の「期限を決める」を照らす（期限の設定シート表示中は出さない。
      先に「期限を決める」を押したら出さない）。「ガイドを出さない」は表示済みと止める設定を GuidePrefs.disableAll で 1 回で保存するようにした
      （2 回に分けると、画面が先に片方だけを読んでしまうため）。通知から開いた直後の判定（R-52）は画面へ開いた元を渡す仕組みが無く、BL-099 に分けた
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/CalendarViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/calendar/MonthGrid.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/GuidePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/GuideViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/ScreenGuides.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/reminder/ReminderViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/MemoGuideController.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/guide/ScreenGuidesTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 1 回目のゲートで既存の CalendarViewModelTest 2 件が Turbine の 15 秒待ちで時間切れになった（単体では成功、続けて流し直した
      testDebugUnitTest 全件でも成功したため、負荷の高いときの待ち時間切れと判断。再発したら待ち方を見直す）。全ゲートを流し直して
      BUILD SUCCESSFUL（単体テスト 217 件。追加は G-07 が買い物の日があるときだけ 1 回・先に日付を押したら出さない・G-08 が初めての買い物の後に出て
      シート中・先に押したら出さない・ガイドを出さないで止まる 4 件）、markdownlint 0 issues、記録ファイル OK。実機での吹き出しの位置は未確認
    関連ID:
      - BL-097
- date: 2026-10-25 11:17
  summary: メモ画面のその場のガイド（G-02〜G-06）と「れんしゅう」項目（R-51・R-54・R-56）（M8）
  details:
    変更内容: >-
      その場のガイドの吹き出し GuideOverlay（照らす場所の周りを暗くするのは見た目だけで、照らした部品はそのまま操作できる。「わかった」
      「ガイドを出さない」。読み上げを使っているときは照らさず画面下のカードで出し「わかった」へ移動する）と、照らす場所を集める guideTarget を
      追加した。メモ画面では 🎤・れんしゅう項目・一番上の項目の ○・「元に戻す」・お店チップを照らし、きっかけは MemoGuides.candidates、
      出すかの判定・れんしゅう項目・先に使った機能の記録は MemoGuideController にまとめた（ViewModel が detekt の上限を超えないよう分けた）。
      れんしゅう項目（ID -1）は DB に入れず、スワイプ・○ で練習を終える（記録・品目・元に戻すに残さない）。○ に照らす目印を渡すため
      LocalBoughtButtonModifier を足した（行の引数を増やさない）。G-03 の手本のアニメーションは BL-098 に含めた
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/GuideOverlay.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/MemoGuides.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/MemoGuideController.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/shoppinglist/SwipeableItemRow.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/shoppinglist/ShoppingListGuideTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/GuideOverlayTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 1 回目は detekt（handleScreenEvent の複雑度・ViewModel の関数の数・SwipeableItemRow の引数の数）で失敗し、MemoGuideController と
      LocalBoughtButtonModifier へ分けた。2 回目はメモリ不足で Claude Code にゲートを止められ、オーナーの指示で再実行して BUILD SUCCESSFUL
      （約 2 分 45 秒。単体テスト 213 件。追加は空のメモで G-02・シート中は出さない・れんしゅうは記録に残らず次は G-04・先に本物を動かすと
      れんしゅうを出さず G-05・ガイドを出さないで全部止まる・G-05 の後にお店チップ 5 件、吹き出しとカード 2 件）、markdownlint 0 issues、記録ファイル OK。
      実機での吹き出しの位置は未確認
    関連ID:
      - BL-096
- date: 2026-10-25 10:59
  summary: はじめての案内（G-01）とその場のガイドの土台（R-50・R-52・R-53・R-55）（M8）
  details:
    変更内容: >-
      はじめての案内 OnboardingContent（「話すだけでメモ」「買ったらスワイプ」「あとから見返せる」の 3 枚と、最後に「ホーム画面に置く」
      の「置く」2 つ・「あとで」。「とばす」は常に出す。非対応のホーム画面アプリでは置き方を説明）を追加し、IkumemoApp で初回起動時だけ
      全画面で出すようにした（保存値を読み終えるまでは何も出さない。通知・ウィジェット・ショートカットからの起動では挟まない）。
      ガイドの保存 GuidePrefs（見終えたか・ガイドを出さない・表示済みのガイドの集合）と GuideViewModel、出すガイドの判定 GuidePolicy
      （未表示の最初の 1 つだけ、音声入力中・シート表示中・外から開いた直後・ガイドを出さない・案内の前は出さない）と GuideId（G-02〜G-08）を
      追加した。外から開く画面への移動は、NavHost が画面の一覧を設定し終えるまで待ってから行うようにした（ショートカットから起動した
      テストで setGraph 前に移動して落ちたため）。下タブにテスト用の目印を付けた。1 枚目のアニメーションは BL-098 に分けた
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/GuidePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/Guide.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/GuideViewModel.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/guide/OnboardingScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/data/GuidePrefsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/guide/GuidePolicyTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/guide/OnboardingTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 途中、ショートカットから起動したテストの setGraph 前の移動、Robolectric を使わない DataStore テストの Windows でのファイル置き換え失敗、
      detekt の ReturnCount（GuidePolicy.next）を直して再実行し BUILD SUCCESSFUL（約 3 分。単体テスト 206 件。追加は判定 4 件・保存 4 件・
      案内の画面 3 件・初回だけ出る/見終えたら出ない/外からの起動では挟まない 3 件）、markdownlint 0 issues、記録ファイル OK
    関連ID:
      - BL-080
- date: 2026-10-25 10:47
  summary: アプリからウィジェットを置く（W-03）とアイコン長押しのショートカット（F-74）（M7）
  details:
    変更内容: >-
      せっていに「ホーム画面に置く」を追加し、ウィジェット 2 種の見本（話して追加 1×1・メモ一覧 4×4）と「置く」
      （WidgetPinner、requestPinAppWidget）を置いた。置く大きさは Android の API で指定できないため、オーナー判断により
      メモ一覧の既定の大きさを 4×2 から 4×4 に変え（targetCellHeight 4、minHeight 250dp）、4×2 は置いた後に長押しで小さくできると
      説明だけ添えた。ホーム画面アプリが対応していなければボタンの代わりに置き方を説明する。ショートカット「話して追加」「文字で追加」
      「カレンダー」「おしらせ」は、debug 版の applicationId が違い静的な shortcuts.xml にはパッケージ名を固定で書くしかないため、
      起動時に ShortcutManagerCompat の動的ショートカットとして登録する（AppShortcuts、アイコンは Material Icons の形を丸い地に載せた
      ic_shortcut_*）。BL-073 の実機確認の対象に今回の 2 つを足した
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppShortcuts.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApplication.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/HomeScreenScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsNavigation.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/widget/WidgetPinner.kt
      - mobile/src/main/res/drawable/ic_shortcut_calendar.xml
      - mobile/src/main/res/drawable/ic_shortcut_reminder.xml
      - mobile/src/main/res/drawable/ic_shortcut_text.xml
      - mobile/src/main/res/drawable/ic_shortcut_voice.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/xml/widget_memo_info.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/AppShortcutsTest.kt
      - mobile/src/test/kotlin/com/ikumemo/mobile/widget/WidgetPinnerTest.kt
      - mobile/src/testDebug/kotlin/com/ikumemo/mobile/settings/HomeScreenContentTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - BUILD SUCCESSFUL（約 3 分 40 秒。単体テスト 192 件。追加はショートカット 4 種が各画面を開く・登録し直しても増えない・
      2 種の追加要求が出せる・非対応のホーム画面アプリでは要求しない・置くボタンが各ウィジェットを求める・非対応時は置き方を説明する 6 件）、
      markdownlint 0 issues、記録ファイル OK。実機のホーム画面アプリでの見え方は BL-073 で確認する
    関連ID:
      - BL-072
- date: 2026-10-25 10:29
  summary: 背景の写真と膜の濃さを選べるようにした（F-61）（M6）
  details:
    変更内容: >-
      きせかえに「背景の写真」（写真を選ぶ・外す、Photo Picker）と膜の濃さのスライダー（0.3〜0.9、既定 0.6、離したときに保存）を追加した。
      写真は PhotoStore に保存先フォルダを渡せるようにして filesDir/background/ へ写真と同じ形（長辺 1600px・Exif 無し）で取り込み、
      差し替え・解除では新しい設定を保存してから前の画像を消す。AppThemeHost に showBackground を足し、アプリ本体（MainActivity）だけ
      画面いっぱいに写真とテーマの背景色の膜を描いて、Scaffold の背景を透かす（LocalHasBackgroundImage）。暗い画面では膜を 0.15 濃くする
      （上限 0.95、BackgroundScrim）。メモの行・上部のバー・下タブは不透明のまま覆う。音声入力シートとウィジェットには描かない。
      背景の設定は AppearancePrefs の背景画像のパスと膜の濃さ（範囲外は丸める）として保存する
    変更ファイル:
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppContainer.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/AppViewModelFactory.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/IkumemoApp.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/AppearancePrefs.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/data/PhotoStore.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/AppThemeHost.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/BackgroundScrim.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceScreen.kt
      - mobile/src/main/kotlin/com/ikumemo/mobile/settings/AppearanceViewModel.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/ikumemo/mobile/settings/AppearanceViewModelTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest :core:test assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 1 回目は背景の差し替えテストが失敗（前の画像を消すのが設定の保存の後で、テストが消える前に確かめていた）。
      テストを「消えるまで実時間で短く待つ」形に直して再実行し BUILD SUCCESSFUL（約 2 分。単体テスト 186 件。追加は背景の設定・差し替え・解除で
      前の画像が消える・膜の濃さが範囲内で保存される・暗い画面で膜が濃くなる 3 件）、markdownlint 0 issues、記録ファイル OK。
      実機での見た目（写真と膜・各テーマ・暗い画面）は未確認で、BL-062 のスクリーンショットテストと M6 の組み合わせ確認で見る
    関連ID:
      - BL-061
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
