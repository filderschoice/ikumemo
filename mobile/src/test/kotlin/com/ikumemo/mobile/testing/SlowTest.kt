package com.ikumemo.mobile.testing

/**
 * 時間のかかるテストの印（JUnit の `@Category(SlowTest::class)`）。通常の品質ゲートでは流さず、
 * `-PslowTests` を付けた時だけ流す（性能に関わる変更の時とマイルストーンの区切り、CLAUDE.md「本リポジトリの品質ゲート定義」）。
 */
interface SlowTest
