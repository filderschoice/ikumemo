---
name: screenshot-verify
description: ikumemo のスクリーンショットテスト（Roborazzi）で、画面の見た目を正解画像（mobile/screenshots/）と画面ごとに比べる手順と、見た目を意図して変えたときに記録し直す手順。流す前の ./gradlew --stop、memo・calendar・reminder を 1 画面ずつ流す理由（まとめるとメモリ不足）、失敗時の差分画像の場所と縮小してから読む作法、記録し直してよいかの判断と変わった画像のコミット、見た目に関わる変更かの判断基準を含む。「スクリーンショットを比べて」「Roborazzi を流して」「見た目の回帰を確かめて」「正解画像を記録し直して」等の依頼で使う。
model: sonnet
---

# スクリーンショットの比較と記録し直し（ikumemo）

コマンドとゲートの合否基準の正本は `CLAUDE.md`「本リポジトリの品質ゲート定義」です。本スキルはその流し方と、
失敗したときの調べ方・記録し直しの判断を補います。テストは
`mobile/src/testDebug/kotlin/com/ikumemo/mobile/screenshot/MainScreensScreenshotTest.kt`、正解画像は
`mobile/screenshots/{memo,calendar,reminder}/`（4 テーマ × ライト/ダーク × 文字の大きさ 3 段階 = 画面ごとに 24 枚）です。

## 1. 流すかどうかの判断（見た目に関わる変更か）

次のどれかに当たれば見た目に関わる変更として、関係する画面を比べます。迷ったら 3 画面とも比べます。

- `mobile/src/main/kotlin/com/ikumemo/mobile/designsystem/` の変更（色・テーマ・文字の大きさ・共通の部品）
- `*Screen.kt`・`*Content.kt` など画面を描く Composable の変更（`shoppinglist/` は memo、`calendar/` は calendar、`reminder/` は reminder）
- `mobile/src/main/res/values*/strings.xml` の文言の変更（文字幅で折り返しが変わる）
- `MainScreensScreenshotTest.kt` の見本データや画面サイズの変更
- Compose・Material3・Roborazzi・Robolectric の版の更新

ViewModel・DB・通知だけの変更、文書だけの変更では流しません。マイルストーンの区切りには変更が無くても 3 画面とも流します。

## 2. 比べる（画面ごとに 1 回ずつ）

1. 流す前に常駐プロセスを止める: `./gradlew --stop`
2. 1 画面ずつ流す（3 画面をまとめて流すと PC のメモリが足りない）。Claude Code ではバックグラウンドにせず、
   通常のコマンド（タイムアウト 600000 ミリ秒）で流す

   ```bash
   ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.memo*"
   ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.calendar*"
   ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.verify=true --tests "*MainScreensScreenshotTest.reminder*"
   ```

3. 終了コード 0 なら一致。出力は長いので、ファイルへ落として末尾だけ読む（`> build/screenshot-memo.log 2>&1` → `tail -20`）

## 3. 失敗したとき（差分画像の場所と読み方）

- 差分画像は `mobile/build/outputs/roborazzi/` に出る（`<テーマ>_<明暗>_<文字の大きさ>_compare.png` が正解と今回を並べた画像、
  `_actual.png` が今回の画像）。見つからなければ `find mobile/build -name "*_compare.png"` で探す
- どの組み合わせが違ったかは、画像を開く前にテキストで絞る。`mobile/build/test-results/roborazzi/results-summary.json`
  の `summary`（`changed` の件数）と、`results` のうち `type` が `changed` の `golden_file_path` で、違った枚数と組み合わせを先に数える
  （HTML の一覧は `mobile/build/reports/roborazzi/index.html`）
- 画像は必要な画面・組み合わせだけを、切り出して縮小してから読む（ユーザー共通スキル `verification-cost-control` の第 3 節）。
  24 枚すべてが違うときは、まず 1 枚（例 `simple_light_normal`）だけを見て原因の見当をつける。縮小の例（Pillow）

  ```python
  from PIL import Image
  img = Image.open("mobile/build/outputs/roborazzi/simple_light_normal_compare.png")
  img.thumbnail((720, 720))
  img.save("<スクラッチのディレクトリ>/compare_small.png")
  ```

- 違いが意図しないもの（回帰）なら、コードを直して同じ画面を流し直す。記録し直して合わせてはならない
- 同じ原因で 3 回続けて失敗したら、記録し直さずに止めて報告する（guardrails 12.3）

## 4. 記録し直す（見た目を意図して変えたときだけ）

1. 差分が BACKLOG のタスクで意図した変更だけであることを、上の 3. の読み方で確かめる。意図しない違いが混じるなら記録し直さない
2. 変えた画面だけを記録し直す（`--stop` の後、1 画面ずつ）

   ```bash
   ./gradlew :mobile:testDebugUnitTest -PslowTests -Proborazzi.test.record=true --tests "*MainScreensScreenshotTest.memo*"
   ```

3. 変わった画像を列挙する: `git diff --stat mobile/screenshots/`。意図した画面・組み合わせ以外が変わっていたら、
   その画像は `git checkout -- <パス>` で戻して原因を調べる（描画の揺れなら報告に書く）
4. 記録し直した画面を `-Proborazzi.test.verify=true` でもう一度流し、終了コード 0 を確かめる
5. 変わった画像をコードの変更と同じコミットに含める（`git add` は画像のパスを明示する）。実施記録（`EXECUTE.md`）の
   検証コマンドに、記録し直した画面と比べ直した結果を書く

## やってはいけないこと

- 3 画面を 1 回の Gradle でまとめて流す、Gradle をバックグラウンドで流す、2 つの Gradle を同時に流す
- 失敗の原因を確かめずに `-Proborazzi.test.record=true` で上書きする
- 差分画像を縮小せずに何枚も読む
