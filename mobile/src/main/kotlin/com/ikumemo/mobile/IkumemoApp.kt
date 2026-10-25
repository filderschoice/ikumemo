package com.ikumemo.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ikumemo.mobile.calendar.CalendarScreen
import com.ikumemo.mobile.designsystem.LocalHasBackgroundImage
import com.ikumemo.mobile.guide.GuideViewModel
import com.ikumemo.mobile.guide.OnboardingContent
import com.ikumemo.mobile.reminder.ReminderScreen
import com.ikumemo.mobile.settings.SettingsRoute
import com.ikumemo.mobile.settings.settingsGraph
import com.ikumemo.mobile.shoppinglist.HomeNavigation
import com.ikumemo.mobile.shoppinglist.ShoppingListScreen
import com.ikumemo.mobile.voice.FieldDictationViewModel
import com.ikumemo.mobile.voice.LocalFieldDictation
import com.ikumemo.mobile.widget.WidgetPinner
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object MemoRoute

@Serializable
object CalendarRoute

@Serializable
object ReminderRoute

/** 下タブのテスト用の目印。 */
object TopLevelTabTags {
    const val MEMO = "tab_memo"
    const val CALENDAR = "tab_calendar"
    const val REMINDER = "tab_reminder"
}

/** 下タブ 3 つ（アイコン＋文字ラベル、PLAN §3）。 */
private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
    val testTag: String,
) {
    MEMO(MemoRoute, MemoRoute::class, R.string.tab_memo, Icons.Rounded.ShoppingCart, TopLevelTabTags.MEMO),
    CALENDAR(
        CalendarRoute,
        CalendarRoute::class,
        R.string.tab_calendar,
        Icons.Rounded.CalendarMonth,
        TopLevelTabTags.CALENDAR,
    ),
    REMINDER(
        ReminderRoute,
        ReminderRoute::class,
        R.string.tab_reminder,
        Icons.Rounded.Notifications,
        TopLevelTabTags.REMINDER,
    ),
}

@Composable
fun IkumemoApp(
    navController: NavHostController = rememberNavController(),
    requestedScreen: ScreenRequest? = null,
    onTabOpened: () -> Unit = {},
) {
    var memoRequest by remember { mutableStateOf<ScreenRequest?>(null) }
    // 通知・ウィジェット・ショートカットから起動したときは、はじめての案内を挟まずにその画面を開く
    val launchedFromOutside = remember { requestedScreen != null }
    val guide: GuideViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val guideState by guide.state.collectAsStateWithLifecycle()
    val showOnboarding = guideState?.onboardingDone == false && !launchedFromOutside
    LaunchedEffect(requestedScreen, showOnboarding) {
        // 案内を出している間はメモ等の画面（NavHost）が無いため、案内を閉じてから開く
        if (showOnboarding) return@LaunchedEffect
        val request = requestedScreen ?: return@LaunchedEffect
        val route: Any =
            when (request.screen) {
                MainActivity.TAB_REMINDER -> ReminderRoute
                MainActivity.TAB_CALENDAR -> CalendarRoute
                MainActivity.OPEN_ADD_TEXT, MainActivity.OPEN_VOICE, MainActivity.OPEN_ITEM ->
                    MemoRoute.also { memoRequest = request }
                MainActivity.OPEN_MEMO -> MemoRoute
                else -> return@LaunchedEffect
            }
        // 起動直後は NavHost が画面の一覧（graph）を設定し終える前に来ることがあるため、設定されるまで待つ
        navController.currentBackStackEntryFlow.first()
        navController.navigate(route) { launchSingleTop = true }
        onTabOpened()
    }
    // 編集シートの各欄のマイク（F-04）。アプリ全体で 1 つの聞き取りを共有する
    val dictation: FieldDictationViewModel = viewModel(factory = AppViewModelFactory.Factory)
    when {
        // 保存値を読み終えるまでは何も出さない（案内とメモ画面が入れ替わって見えないように）
        guideState == null && !launchedFromOutside -> Unit
        showOnboarding -> Onboarding(onFinish = guide::finishOnboarding)
        else ->
            CompositionLocalProvider(LocalFieldDictation provides dictation) {
                AppScaffold(navController, memoRequest) { memoRequest = it }
            }
    }
}

/** はじめての案内（G-01）。最後のページの「置く」は、せっていの「ホーム画面に置く」と同じ処理（W-03）。 */
@Composable
private fun Onboarding(onFinish: () -> Unit) {
    val context = LocalContext.current
    val pinner = remember(context) { WidgetPinner(context) }
    OnboardingContent(pinSupported = pinner.isSupported, onPin = { pinner.pin(it) }, onFinish = onFinish)
}

@Composable
private fun AppScaffold(
    navController: NavHostController,
    memoRequest: ScreenRequest?,
    onMemoRequestChange: (ScreenRequest?) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 背景画像を設定していれば、背景色を塗らずに写真と膜を透かす（F-61）
        containerColor =
            if (LocalHasBackgroundImage.current) Color.Transparent else MaterialTheme.colorScheme.background,
        bottomBar = { BottomTabs(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MemoRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MemoRoute> {
                ShoppingListScreen(
                    navigation =
                        HomeNavigation(
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            request = memoRequest,
                            onRequestHandled = { onMemoRequestChange(null) },
                        ),
                )
            }
            composable<CalendarRoute> { CalendarScreen() }
            composable<ReminderRoute> { ReminderScreen() }
            settingsGraph(navController)
        }
    }
}

@Composable
private fun BottomTabs(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = destination?.hasRoute(tab.routeClass) == true,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                modifier = Modifier.testTag(tab.testTag),
            )
        }
    }
}
