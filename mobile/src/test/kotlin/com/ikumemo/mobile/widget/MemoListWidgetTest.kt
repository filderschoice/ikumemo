package com.ikumemo.mobile.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.designsystem.ThemePreset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-071 の完了条件「サイズ別レイアウト・空の表示・タップ先」（glance-appwidget-testing）。 */
@RunWith(RobolectricTestRunner::class)
class MemoListWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val palette = ThemePreset.entries.first().palette(darkTheme = false)
    private val colors = WidgetColors(palette, palette)

    private val fiveItems =
        MemoWidgetState(
            items =
                listOf(
                    MemoWidgetItem(1, "牛乳", "2本"),
                    MemoWidgetItem(2, "たまご", "1パック"),
                    MemoWidgetItem(3, "食パン", null),
                    MemoWidgetItem(4, "トイレットペーパー", null),
                    MemoWidgetItem(5, "洗剤", null),
                ),
            dueSoonCount = 2,
            storeName = "さくらスーパー",
        )

    private fun render(
        state: MemoWidgetState,
        size: DpSize,
        block: GlanceAppWidgetUnitTest.() -> Unit,
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { MemoListContent(state, colors) }
        block()
    }

    @Test
    fun smallShowsHeaderAndFirstNamesInARow() =
        render(fiveItems, MemoListWidget.SMALL) {
            onNode(hasTextEqualTo("🛒メモ 5件")).assertExists()
            onNode(hasTextEqualTo("🔔期限が近い 2件")).assertExists()
            onNode(hasTextEqualTo("・牛乳 2本　・たまご 1パック　・食パン")).assertExists()
            onNode(hasTextEqualTo("ほか2件")).assertExists()
            // 小さい表示ではお店を出さない
            onAllNodes(hasText("さくらスーパー")).assertCountEquals(0)
        }

    @Test
    fun narrowSmallShowsFewerNames() =
        render(fiveItems, MemoListWidget.NARROW_SMALL) {
            onNode(hasTextEqualTo("・牛乳 2本　・たまご 1パック")).assertExists()
            onNode(hasTextEqualTo("ほか3件")).assertExists()
        }

    @Test
    fun largeShowsStoreAndEveryItemWithQuantity() =
        render(fiveItems, MemoListWidget.LARGE) {
            onNode(hasTextEqualTo("🏪 さくらスーパー で買う")).assertExists()
            onNode(hasTextEqualTo("・トイレットペーパー")).assertExists()
            onNode(hasTextEqualTo("・洗剤")).assertExists()
            onNode(hasTextEqualTo("1パック")).assertExists()
            onAllNodes(hasText("ほか")).assertCountEquals(0)
        }

    @Test
    fun rowOpensThatItemAndDueSoonOpensReminders() =
        render(fiveItems, MemoListWidget.LARGE) {
            onNode(hasStartActivityClickAction(MainActivity.itemIntent(context, 2))).assertExists()
            onNode(
                hasStartActivityClickAction(MainActivity.intent(context, MainActivity.TAB_REMINDER))
                    .and(hasAnyDescendant(hasTextEqualTo("🔔期限が近い 2件"))),
            ).assertExists()
            onNode(hasStartActivityClickAction(MainActivity.intent(context, MainActivity.OPEN_MEMO))).assertExists()
        }

    @Test
    fun emptyMemoShowsMessageWithoutDueSoon() =
        render(MemoWidgetState(), MemoListWidget.LARGE) {
            onNode(hasTextEqualTo("メモは空です")).assertExists()
            onNode(hasTextEqualTo("🛒メモ 0件")).assertExists()
            onAllNodes(hasText("期限が近い")).assertCountEquals(0)
            onNode(hasStartActivityClickAction(MainActivity.intent(context, MainActivity.OPEN_MEMO))).assertExists()
        }

    @Test
    fun refreshButtonIsShownInEverySizeEvenWhenEmpty() {
        listOf(MemoListWidget.SMALL, MemoListWidget.LARGE).forEach { size ->
            render(fiveItems, size) {
                onNode(hasContentDescriptionEqualTo("メモ一覧を更新")).assertExists()
            }
        }
        render(MemoWidgetState(), MemoListWidget.LARGE) {
            onNode(hasContentDescriptionEqualTo("メモ一覧を更新")).assertExists()
        }
    }
}
