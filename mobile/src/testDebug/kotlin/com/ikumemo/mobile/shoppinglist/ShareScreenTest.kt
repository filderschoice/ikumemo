package com.ikumemo.mobile.shoppinglist

import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import com.ikumemo.mobile.voice.VoiceInputViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** 「送る」の画面の操作（BL-196）。共有シートの Intent が組まれること、出す・出さないの場面を確かめる。 */
@RunWith(RobolectricTestRunner::class)
class ShareScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        repository = ShoppingRepository(db, ProductNameNormalizer(), clock)
        val stores = StoreRepository(db, testSettings(), ProductNameNormalizer(), clock)
        val viewModel = ShoppingListViewModel(repository, stores)
        val voiceViewModel = VoiceInputViewModel(repository, FakeSpeechEngine())
        composeRule.setContent { IkumemoTheme { ShoppingListScreen(viewModel, voiceViewModel) } }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun shareNodes() = composeRule.onAllNodesWithTagCount(TestTags.SHARE)

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodesWithTag(tag).fetchSemanticsNodes().size

    @Test
    fun emptyMemoHasNoShareButton() {
        composeRule.awaitCondition { composeRule.onAllNodesWithTagCount(TestTags.SELECT_START) == 0 }
        assertEquals(0, shareNodes())
    }

    @Test
    fun shareOpensChooserWithMemoText() {
        runBlocking {
            repository.addItem("牛乳", quantity = "2本")
            repository.addItem("たまご")
        }
        composeRule.awaitTag(TestTags.SHARE)
        composeRule.onNodeWithTag(TestTags.SHARE).performClick()
        val started = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_CHOOSER, started.action)
        @Suppress("DEPRECATION")
        val send = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("🛒いくメモ\n・牛乳 2本\n・たまご", send.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun shareButtonIsHiddenWhileSelecting() {
        runBlocking { repository.addItem("牛乳") }
        composeRule.awaitTag(TestTags.SHARE)
        composeRule.onNodeWithTag(TestTags.SELECT_START).performClick()
        composeRule.awaitTag(TestTags.SELECT_DELETE)
        assertEquals(0, shareNodes())
    }
}
