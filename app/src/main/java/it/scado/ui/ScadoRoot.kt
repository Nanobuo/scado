package it.scado.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.scado.AppContainer
import it.scado.ScadoApp
import it.scado.ui.screens.AuthScreen
import it.scado.ui.screens.HomeScreen
import it.scado.ui.screens.HouseholdSetupScreen
import it.scado.ui.screens.SettingsScreen
import it.scado.ui.screens.ShoppingScreen

@Composable
fun ScadoRoot(openShopping: Boolean, onShoppingOpened: () -> Unit) {
    val app = LocalContext.current.applicationContext as ScadoApp
    val container = app.container
    val sessionVm: SessionViewModel = viewModel(
        factory = viewModelFactory { initializer { SessionViewModel(container) } }
    )
    val state by sessionVm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            SessionState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            SessionState.SignedOut -> AuthScreen(container.authRepo)
            is SessionState.NoHousehold -> HouseholdSetupScreen(container, s)
            is SessionState.Ready -> key(s.uid, s.householdId) {
                FridgeNav(app, container, s, openShopping, onShoppingOpened)
            }
        }
    }
}

@Composable
private fun FridgeNav(
    app: Application,
    container: AppContainer,
    session: SessionState.Ready,
    openShopping: Boolean,
    onShoppingOpened: () -> Unit,
) {
    val vm: FridgeViewModel = viewModel(
        key = "fridge_${session.uid}_${session.householdId}",
        factory = viewModelFactory { initializer { FridgeViewModel(app, container, session) } },
    )
    val nav = rememberNavController()

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onShopping = { nav.navigate("shopping") { launchSingleTop = true } },
                onSettings = { nav.navigate("settings") { launchSingleTop = true } },
            )
        }
        composable("shopping") { ShoppingScreen(vm, onDone = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
    }

    LaunchedEffect(openShopping) {
        if (openShopping) {
            nav.navigate("shopping") { launchSingleTop = true }
            onShoppingOpened()
        }
    }
}
