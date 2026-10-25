package com.ikumemo.mobile.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute

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

/** せっていとその下の画面（PLAN §3 右上メニュー）。項目の行き先は [SettingsItem.route]。 */
fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<SettingsRoute> {
        SettingsScreen(onBack = back, onOpen = { item -> navController.navigate(item.route) })
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
