import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.roborazzi)
}

// 現在のバージョンは scripts/version.properties が正本（PLAN §4.5）。リリースビルドでは
// scripts/release-build.ps1 が -PappVersionCode / -PappVersionName で次の値を渡し、成功時だけ正本を更新する。
val versionProperties =
    Properties().apply {
        val file = rootProject.file("scripts/version.properties")
        if (file.exists()) {
            FileInputStream(file).use { load(it) }
        }
    }

// リリース署名の情報は local.properties（.gitignore 対象）から読み込む（PLAN §6、sesami-wear と同じ）。
// 未設定でも assembleDebug 等の通常ビルドには影響しない（release は署名なしのまま作られる）。
val releaseKeystoreProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            FileInputStream(file).use { load(it) }
        }
    }
val hasReleaseSigningConfig = releaseKeystoreProperties.containsKey("RELEASE_STORE_FILE")

android {
    namespace = "com.ikumemo.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ikumemo.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode =
            (findProperty("appVersionCode") as String?)?.toIntOrNull()
                ?: versionProperties.getProperty("VERSION_CODE")?.toIntOrNull()
                ?: 1
        versionName =
            findProperty("appVersionName") as String?
                ?: versionProperties.getProperty("VERSION_NAME")
                ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = releaseKeystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = releaseKeystoreProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = releaseKeystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Play 版と同じ端末に並べて入れられるよう applicationId を分ける（PLAN §4.5、sesami-wear と同じ形）。
            // アプリ名は src/debug/res の strings.xml で「いくメモ（debug）」に差し替える
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric で Room・リソースを使うテストを JVM 上で動かす（PLAN §11）
        unitTests.isIncludeAndroidResources = true
        // 時間のかかるテスト（SlowTest）は -PslowTests の時だけ流す（CLAUDE.md「本リポジトリの品質ゲート定義」）
        unitTests.all { test ->
            if (!project.hasProperty("slowTests")) {
                test.useJUnit { excludeCategories("com.ikumemo.mobile.testing.SlowTest") }
            }
        }
    }
}

// Room のスキーマ JSON を Git 管理し、AutoMigration とマイグレーションテストの基準にする（PLAN §4.3）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    // ホーム画面ウィジェット（W-01・W-02、PLAN §4.1）。Glance が推移的に持ち込む古い work-runtime は
    // sesami-wear と同じく 2.10 系へ明示して引き上げる（写真の掃除 R-14 でも使う）
    implementation(libs.androidx.glance.appwidget)
    // このアプリについて のライセンス一覧（Play Services に依存しない、PLAN §4.1・§7）
    implementation(libs.aboutlibraries.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.roborazzi)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
}
