# Baseline Profile の作成と起動時間の計測

BL-100。`:baselineprofile` モジュール（Macrobenchmark）で、実機の起動操作から Baseline Profile を作り、コールドスタートを計測する。
PLAN §8 の目標は中級機で 1.5 秒以内（中級機の確認は BL-171）。

## 構成

- `baselineprofile/`: `com.android.test` モジュール。対象は `:mobile`
  - `BaselineProfileGenerator`: 起動してメモ一覧を出し、下のタブ（記録・期限・メモ）を巡る。規則はここから作る
  - `StartupBenchmark`: コールドスタートを規則なし（`CompilationMode.None`）と規則あり（`BaselineProfileMode.Require`）で 10 回ずつ測る
- 規則は `mobile/src/release/generated/baselineProfiles/` へ出力され、`:mobile` のリリースビルドに入る。
  （旧 `mobile/src/main/baseline-prof.txt` の広い規則は削除して置き換えた）
- 版: AGP 8.13・Kotlin 2.0.21 に合わせ、`androidx.baselineprofile` と `benchmark-macro-junit4` は 1.4.1（`gradle/libs.versions.toml`）

## 手順

1. 実機を USB 接続し、画面を点灯・ロック解除しておく（消灯・ロックでは起動が確認できず失敗する）
2. `./gradlew --stop` の後、`ANDROID_SERIAL=<シリアル> ./gradlew :mobile:generateBaselineProfile`
3. 計測: `ANDROID_SERIAL=<シリアル> ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
4. 計測用のビルドの applicationId は `com.ikumemo.mobile`（デバッグ版 `.debug` とは別）。終わったら `adb uninstall com.ikumemo.mobile` で消してよい

## 状況（2026-11-14、完了）

- 規則は `mobile/src/release/generated/baselineProfiles/` にある。旧 `mobile/src/main/baseline-prof.txt` は削除済み
- 初回の `Unable to confirm activity launch completion` は、benchmark-macro 1.3.4 が Android 17 に未対応だったため（アプリは起動していた）。1.4.1 に上げて解消した。
  規則あり計測は対象アプリの `profileinstaller` が 1.4.0 以上でないと API 34 以降で失敗するため、これも 1.4.1 にした
- 計測（Pixel 8 Pro・Android 17、コールドスタート 10 回、`timeToInitialDisplayMs`）: 規則なし 中央値 186.3ms（163.3〜240.8）、規則あり 中央値 182.3ms（162.3〜202.9）。
  上位機のため差は小さい。中級機で 1.5 秒以内かは BL-171（人手検証）

## baseline-prof.txt と startup-prof.txt が同じ内容になる理由（BL-183、2026-11-15）

両ファイルは中身が同じ（各 14,869 行）だが、役割が違うため両方を残す。重複の解消（片方の削除）はしない。

- `baseline-prof.txt`: Baseline Profile。ART がインストール時に規則のクラス・メソッドを AOT コンパイルする（`profileinstaller` と合わせて効く）
- `startup-prof.txt`: Startup Profile。Baseline Profile の部分集合で、R8 が DEX 内のコードの配置を最適化するのに使う
  （DEX レイアウト最適化。AGP 8.3 以降は既定で有効、R8 が必要で、このアプリは release が `isMinifyEnabled = true`）。
  Baseline Profile と併用すると、起動が Baseline Profile だけのときより 15〜30% 速くなるとされる
- 同じ中身になるのは、`BaselineProfileGenerator` が `rule.collect(..., includeInStartupProfile = true)` で、起動とタブ巡りの 1 本の操作をどちらにも入れているため。
  片方を消すと、`baseline-prof.txt` なら AOT コンパイルが、`startup-prof.txt` なら DEX の配置の最適化が効かなくなる
- 差分を減らすなら、`includeInStartupProfile = true` を起動だけの操作に分けて、タブ巡りは Baseline Profile だけに入れる（Startup Profile を小さくすると
  1 つ目の `classes.dex` に収まりやすくなる利点もある）。ただし規則の中身が変わり、実機での再生成と起動の再計測が要るため、実機を使える時に別タスクで行う
- 参照: <https://developer.android.com/topic/performance/baselineprofiles/dex-layout-optimizations>、
  <https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles>

## :baselineprofile の minSdk が 28 である理由（BL-184、2026-11-15）

- `:baselineprofile`（計測・生成側の `com.android.test` モジュール）の `minSdk = 28` は、アプリ（`:mobile`）の `minSdk = 26` より高いが、**意図どおりで変えない**。
  アプリ本体の対応範囲（Android 8.0 以降）には影響せず、Baseline Profile を生成・計測する端末の下限だけを決める
- 根拠は、Baseline Profile の生成に使う端末が API 28 以上（Android 9 以降。ルート化していない実機は Android 13 以降が目安）を要件とすること。
  Macrobenchmark 自体は API 23 以上で動くが、プロファイルの生成はそれより高い下限になるため、AGP の Baseline Profile 既定の 28 に合わせている
- 計測側のモジュールは `targetProjectPath` で `:mobile` を指すだけで、`:mobile` の APK にはマージされない。minSdk を 26 へ下げても、
  生成できる端末が増えるわけではなく、Android 8（API 26・27）の端末ではプロファイルを作れない
- 実機での再生成・再計測は行っていない（文書の根拠のみ。要件の最新は developer.android.com の Baseline Profiles の「Create Baseline Profiles」を参照）
