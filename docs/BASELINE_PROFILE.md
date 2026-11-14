# Baseline Profile の作成と起動時間の計測

BL-100。`:baselineprofile` モジュール（Macrobenchmark）で、実機の起動操作から Baseline Profile を作り、コールドスタートを計測する。
PLAN §8 の目標は中級機で 1.5 秒以内（中級機の確認は BL-171）。

## 構成

- `baselineprofile/`: `com.android.test` モジュール。対象は `:mobile`
  - `BaselineProfileGenerator`: 起動してメモ一覧を出し、下のタブ（記録・期限・メモ）を巡る。規則はここから作る
  - `StartupBenchmark`: コールドスタートを規則なし（`CompilationMode.None`）と規則あり（`BaselineProfileMode.Require`）で 10 回ずつ測る
- 規則は `mobile/src/main/generated/baselineProfiles/` へ出力され、`:mobile` のリリースビルドに入る（旧 `mobile/src/main/baseline-prof.txt` の広い規則は、新しい規則ができたら削除して置き換える。計測前に消すこと）
- 版: AGP 8.13・Kotlin 2.0.21 に合わせ、`androidx.baselineprofile` と `benchmark-macro-junit4` は 1.3.4（`gradle/libs.versions.toml`）

## 手順

1. 実機を USB 接続し、画面を点灯・ロック解除しておく（消灯・ロックでは起動が確認できず失敗する）
2. `./gradlew --stop` の後、`ANDROID_SERIAL=<シリアル> ./gradlew :mobile:generateBaselineProfile`
3. 計測: `ANDROID_SERIAL=<シリアル> ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`
4. 計測用のビルドの applicationId は `com.ikumemo.mobile`（デバッグ版 `.debug` とは別）。終わったら `adb uninstall com.ikumemo.mobile` で消してよい

## 状況（2026-11-14 時点、作業中断）

- 手順 2 は実機（Pixel 8 Pro）で `Unable to confirm activity launch completion` で失敗した。画面の状態か、リリース相当のビルドの起動失敗かは未切り分け
- 計測値は未取得
