// Android に依存しない純 Kotlin/JVM のドメインロジック（PLAN §4.5）。
// JVM 単体テストだけで検証できるロジック（音声の分割・表記ゆれ・候補・期限計算・共有テキスト）を置く。
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
