package com.ikumemo.mobile.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ikumemo.mobile.help.HelpIndexScreen
import com.ikumemo.mobile.help.HelpPageScreen
import com.ikumemo.mobile.help.HelpScreens
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute

@Serializable
object HelpRoute

/** 使い方の 1 ページ（[pageId] は assets/help/ のファイル名）。 */
@Serializable
data class HelpPageRoute(
    val pageId: String,
)

@Serializable
object AppearanceRoute

@Serializable
object OperationRoute

@Serializable
object ReminderSettingsRoute

@Serializable
object StoreManageRoute

@Serializable
object ProductManageRoute

@Serializable
object HomeScreenRoute

@Serializable
object FeedbackRoute

@Serializable
object AboutRoute

@Serializable
object LicensesRoute

/**
 * せっていとその下の画面（PLAN §3 右上メニュー）。項目の行き先は [SettingsItem.route]。
 * [onOpenMainScreen] は使い方の「やってみる」でメモ・カレンダー・おしらせの画面を開く（H-02）。
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    onOpenMainScreen: (String) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val openScreen: (String) -> Unit = { screen ->
        when (screen) {
            HelpScreens.APPEARANCE -> navController.navigate(AppearanceRoute)
            HelpScreens.HOME_SCREEN -> navController.navigate(HomeScreenRoute)
            HelpScreens.FEEDBACK -> navController.navigate(FeedbackRoute)
            else -> onOpenMainScreen(screen)
        }
    }
    composable<SettingsRoute> {
        SettingsScreen(onBack = back, onOpen = { item -> navController.navigate(item.route) })
    }
    composable<HelpRoute> { HelpIndexScreen(onBack = back, onOpenPage = { navController.navigate(HelpPageRoute(it)) }) }
    composable<HelpPageRoute> { entry ->
        HelpPageScreen(pageId = entry.toRoute<HelpPageRoute>().pageId, onBack = back, onOpenScreen = openScreen)
    }
    composable<AppearanceRoute> { AppearanceScreen(onBack = back) }
    composable<OperationRoute> { OperationScreen(onBack = back) }
    composable<ReminderSettingsRoute> { ReminderSettingsScreen(onBack = back) }
    composable<StoreManageRoute> { StoreManageScreen(onBack = back) }
    composable<ProductManageRoute> { ProductManageScreen(onBack = back) }
    composable<HomeScreenRoute> { HomeScreenScreen(onBack = back) }
    composable<FeedbackRoute> { FeedbackScreen(onBack = back) }
    composable<AboutRoute> { AboutScreen(onBack = back, onOpenLicenses = { navController.navigate(LicensesRoute) }) }
    composable<LicensesRoute> { LicensesScreen(onBack = back) }
}
