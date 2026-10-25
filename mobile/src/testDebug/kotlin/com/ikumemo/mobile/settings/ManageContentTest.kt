package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** BL-092 のお店の管理・よく買う物の管理。確認を経て操作が届くこと。 */
@RunWith(RobolectricTestRunner::class)
class ManageContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sakura = StoreEntity(id = 1, name = "さくらスーパー", createdAt = Instant.EPOCH)
    private val station = StoreEntity(id = 2, name = "駅前ストア", createdAt = Instant.EPOCH)

    @Test
    fun archiveAndMergeAskBeforeSending() {
        val events = mutableStateListOf<StoreManageEvent>()
        composeRule.setContent {
            IkumemoTheme { StoreManageContent(listOf(sakura, station), onEvent = { events += it }) }
        }

        composeRule.onNodeWithTag(ManageTestTags.archive(sakura.id)).performClick()
        // 確認を出すまでは何も送らない
        assertEquals(emptyList<StoreManageEvent>(), events.toList())
        composeRule.onNodeWithTag(ManageTestTags.ARCHIVE_CONFIRM).performClick()

        composeRule.onNodeWithTag(ManageTestTags.merge(station.id)).performClick()
        composeRule.onNodeWithTag(ManageTestTags.mergeInto(sakura.id)).performClick()

        assertEquals(
            listOf(StoreManageEvent.Archive(sakura.id), StoreManageEvent.Merge(from = station.id, into = sakura.id)),
            events.toList(),
        )
    }

    @Test
    fun productSwitchSendsVisibility() {
        val events = mutableStateListOf<SetProductVisible>()
        val hidden =
            ProductEntity(id = 5, name = "牛乳", normalizedName = "ぎゅうにゅう", hidden = true, createdAt = Instant.EPOCH)
        composeRule.setContent { IkumemoTheme { ProductManageContent(listOf(hidden), onEvent = { events += it }) } }

        composeRule.onNodeWithTag(ManageTestTags.product(hidden.id)).assertIsOff().performClick()

        assertEquals(listOf(SetProductVisible(hidden.id, visible = true)), events.toList())
    }
}
