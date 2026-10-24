package com.ikumemo.mobile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ikumemo.mobile.calendar.CalendarScreen
import com.ikumemo.mobile.reminder.ReminderScreen
import com.ikumemo.mobile.shoppinglist.ShoppingListScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object MemoRoute

@Serializable
object CalendarRoute

@Serializable
object ReminderRoute

/** 下タブ 3 つ（アイコン＋文字ラベル、PLAN §3）。 */
private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    MEMO(MemoRoute, MemoRoute::class, R.string.tab_memo, Icons.Rounded.ShoppingCart),
    CALENDAR(CalendarRoute, CalendarRoute::class, R.string.tab_calendar, Icons.Rounded.CalendarMonth),
    REMINDER(ReminderRoute, ReminderRoute::class, R.string.tab_reminder, Icons.Rounded.Notifications),
}

@Composable
fun IkumemoApp(
    navController: NavHostController = rememberNavController(),
    requestedTab: String? = null,
    onTabOpened: () -> Unit = {},
) {
    LaunchedEffect(requestedTab) {
        if (requestedTab == MainActivity.TAB_REMINDER) {
            navController.navigate(ReminderRoute) { launchSingleTop = true }
            onTabOpened()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomTabs(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MemoRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MemoRoute> { ShoppingListScreen() }
            composable<CalendarRoute> { CalendarScreen() }
            composable<ReminderRoute> { ReminderScreen() }
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
            )
        }
    }
}
