package com.list_it.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.list_it.app.R
import com.list_it.app.ui.theme.IosColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import com.list_it.app.ui.components.TABS
import com.list_it.app.ui.components.TabBar
import com.list_it.app.ui.screens.ProgressScreen
import com.list_it.app.ui.screens.settings.SettingsScreen
import com.list_it.app.ui.theme.appBackground
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.list_it.app.data.Prefs
import com.list_it.app.data.Supabase
import com.list_it.app.model.QuickAction
import com.list_it.app.ui.screens.AllListsScreen
import com.list_it.app.ui.screens.IntroScreen
import com.list_it.app.ui.screens.ListDetailScreen
import com.list_it.app.ui.screens.auth.LoginScreen
import com.list_it.app.ui.screens.auth.SignUpScreen
import com.list_it.app.ui.screens.defaultlists.CompletedScreen
import com.list_it.app.ui.screens.defaultlists.NotCompletedScreen
import com.list_it.app.ui.screens.defaultlists.OverdueScreen
import com.list_it.app.ui.screens.defaultlists.PriorityScreen
import com.list_it.app.ui.screens.defaultlists.ScheduledScreen
import com.list_it.app.ui.screens.defaultlists.TodayScreen
import com.list_it.app.ui.screens.defaultlists.TomorrowScreen
import com.list_it.app.ui.theme.ListItTheme
import com.list_it.app.ui.theme.LocalPalette
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first

/** App root (iOS `List_ItApp`): restores the session, then shows the lists or the intro. */
@Composable
fun ListItApp(db: Supabase, prefs: Prefs) {
    var isAuthenticating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val status = db.client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        prefs.isSignedIn = status is SessionStatus.Authenticated && db.initializeSession()
        isAuthenticating = false
    }

    val signedIn = prefs.isSignedIn && db.currentUser != null
    CompositionLocalProvider(LocalDb provides db, LocalPrefs provides prefs) {
        ListItTheme(prefs.darkMode, prefs::surface) {
            when {
                isAuthenticating -> LoadingView()
                signedIn -> MainNavigation()
                else -> AuthNavigation()
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        Modifier.fillMaxSize().background(LocalPalette.current.systemBackground),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.app_logo), contentDescription = "List It", modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = IosColor.orange)
        Text("Loading data...", color = LocalPalette.current.secondary)
    }
}

@Composable
private fun AuthNavigation() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "intro") {
        composable("intro") { IntroScreen(onGetStarted = { nav.navigate("signup") }) }
        composable("signup") { SignUpScreen(onSignIn = { nav.navigate("login") }) }
        composable("login") { LoginScreen(onSignUp = { nav.navigate("signup") }) }
    }
}

@Composable
private fun MainNavigation() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val onTab = route in TABS.map { it.route }
    Scaffold(
        containerColor = appBackground,
        bottomBar = {
            if (onTab && route != null) {
                TabBar(route) { target ->
                    nav.navigate(target) {
                        popUpTo("lists") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        // Only the tab bar's bottom space is applied here, so only that is consumed; consuming the whole
        // padding swallowed the status-bar inset and pushed every screen's top bar under the clock and icons.
        val bottomOnly = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            nav, startDestination = "lists",
            modifier = Modifier.padding(bottomOnly).consumeWindowInsets(bottomOnly),
        ) {
            composable("lists") {
                AllListsScreen(
                    onOpenList = { nav.navigate("list/${it.id}") },
                    onQuickAction = { nav.navigate("quick/${it.name}") },
                )
            }
            composable("progress") { ProgressScreen() }
            composable("settings") { SettingsScreen() }
            composable("list/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val db = LocalDb.current
                val id = entry.arguments?.getString("id")
                val list = (db.defaultLists + db.lists).firstOrNull { it.id == id }
                if (list != null) ListDetailScreen(list, onBack = back) else LaunchedEffect(Unit) { back() }
            }
            composable("quick/{action}") { entry ->
                when (QuickAction.valueOf(entry.arguments?.getString("action") ?: QuickAction.TODAY.name)) {
                    QuickAction.TODAY -> TodayScreen(back)
                    QuickAction.TOMORROW -> TomorrowScreen(back)
                    QuickAction.SCHEDULED -> ScheduledScreen(back)
                    QuickAction.PRIORITY -> PriorityScreen(back)
                    QuickAction.COMPLETED -> CompletedScreen(back)
                    QuickAction.NOT_COMPLETED -> NotCompletedScreen(back)
                    QuickAction.OVERDUE -> OverdueScreen(back)
                }
            }
        }
    }
}
