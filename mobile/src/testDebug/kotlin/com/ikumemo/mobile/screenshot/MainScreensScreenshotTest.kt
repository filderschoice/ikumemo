package com.ikumemo.mobile.screenshot

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.ikumemo.mobile.calendar.CalendarContent
import com.ikumemo.mobile.calendar.CalendarUiState
import com.ikumemo.mobile.calendar.RecordRow
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.reminder.ReminderContent
import com.ikumemo.mobile.reminder.ReminderUiState
import com.ikumemo.mobile.shoppinglist.ShoppingListContent
import com.ikumemo.mobile.shoppinglist.ShoppingListUiState
import com.ikumemo.mobile.testing.SlowTest
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * 主要画面（メモ・カレンダー・おしらせ）のスクリーンショット（BL-062）。4 テーマ × ライト/ダーク × 文字の大きさ 3 段階を記録し、
 * 見た目が意図せず変わっていないかを比べる。時間がかかるため遅いテスト扱いで、`-PslowTests` の時だけ流す
 * （記録は `./gradlew recordRoborazziDebug -PslowTests`、比較は `./gradlew verifyRoborazziDebug -PslowTests`）。
 * 正解の画像は `mobile/screenshots/` に置いて Git で管理する。
 */
@Category(SlowTest::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class MainScreensScreenshotTest(
    private val theme: ThemePreset,
    private val dark: Boolean,
    private val textSize: TextSizeSetting,
) {
    @get:Rule
    val composeRule = createComposeRule()

    private val name = "${theme.storageKey}_${if (dark) "dark" else "light"}_${textSize.storageKey}"

    private fun capture(
        screen: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            IkumemoTheme(preset = theme, darkTheme = dark, textSize = textSize) {
                // アプリでは外側の Scaffold が背景色を塗るため、ここでも敷く（無いと暗い画面でも背景が白くなる）
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/$screen/$name.png")
    }

    @Test
    fun memo() =
        capture("memo") {
            ShoppingListContent(state = Samples.memo, voiceAvailable = true, onEvent = {})
        }

    @Test
    fun calendar() {
        val photoStore = PhotoStore(ApplicationProvider.getApplicationContext<Context>())
        capture("calendar") { CalendarContent(state = Samples.calendar, photoStore = photoStore, onEvent = {}) }
    }

    @Test
    fun reminder() =
        capture("reminder") {
            ReminderContent(state = Samples.reminder, onEvent = {}, onEnableNotifications = {})
        }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun parameters(): List<Array<Any>> =
            ThemePreset.entries.flatMap { theme ->
                listOf(false, true).flatMap { dark ->
                    TextSizeSetting.entries.map { size -> arrayOf<Any>(theme, dark, size) }
                }
            }
    }
}

/** 画面に出す見本のデータ（日付は固定して、記録した画像が日によって変わらないようにする）。 */
private object Samples {
    private val today = LocalDate.of(2026, 10, 25)
    private val created = Instant.parse("2026-10-25T01:00:00Z")
    private val store = StoreEntity(id = 1, name = "○○スーパー", createdAt = created)

    private fun item(
        id: Long,
        name: String,
        quantity: String? = null,
    ) = ShoppingItemEntity(
        id = id,
        productId = id,
        displayName = name,
        quantity = quantity,
        sortOrder = id.toInt(),
        createdAt = created,
    )

    private fun record(
        id: Long,
        name: String,
        date: LocalDate,
        quantity: String? = null,
    ) = PurchaseRecordEntity(
        id = id,
        tripId = 1,
        productId = id,
        displayName = name,
        quantity = quantity,
        purchasedDate = date,
        recordedAt = created,
        storeId = store.id,
    )

    val memo =
        ShoppingListUiState(
            items = listOf(item(1, "牛乳", "2本"), item(2, "たまご"), item(3, "食パン"), item(4, "トイレットペーパー")),
            loaded = true,
            selectedStore = store,
            stores = listOf(store),
        )

    private val milk = record(1, "牛乳", today, "2本")
    private val eggs = record(2, "たまご", today)

    val calendar =
        CalendarUiState(
            month = YearMonth.of(2026, 10),
            selectedDate = today,
            today = today,
            shoppingDays = mapOf(LocalDate.of(2026, 10, 8) to listOf(store.id), today to listOf(store.id, null)),
            selectedRecords = listOf(RecordRow(milk, store.name), RecordRow(eggs, null)),
            stores = listOf(store),
        )

    val reminder =
        ReminderUiState(
            today = today,
            recent = listOf(milk, eggs),
            pending =
                listOf(
                    ReminderRow(
                        ReminderEntity(id = 1, purchaseRecordId = 3, productId = 3, dueDate = today.plusDays(1)),
                        "ヨーグルト",
                    ),
                    ReminderRow(
                        ReminderEntity(id = 2, purchaseRecordId = 4, productId = 4, dueDate = today.minusDays(2)),
                        "豆腐",
                    ),
                ),
            loaded = true,
        )
}
