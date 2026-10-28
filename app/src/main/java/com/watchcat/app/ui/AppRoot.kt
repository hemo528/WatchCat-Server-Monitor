package com.watchcat.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watchcat.app.ui.components.AuroraBackground
import com.watchcat.app.ui.components.LocalHazeState
import com.watchcat.app.ui.components.glassSurface
import com.watchcat.app.ui.screens.DetailScreen
import com.watchcat.app.ui.screens.EditServerScreen
import com.watchcat.app.ui.screens.HomeScreen
import com.watchcat.app.ui.screens.SettingsScreen
import com.watchcat.app.ui.screens.TerminalScreen
import com.watchcat.app.data.ThemeMode
import com.watchcat.app.ui.theme.AccentCyan
import com.watchcat.app.ui.theme.WatchCatTheme
import com.watchcat.app.ui.theme.TextPrimary
import com.watchcat.app.vm.AppViewModel
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    WatchCatTheme(darkTheme = darkTheme) {
        AppContent(vm = vm, darkTheme = darkTheme)
    }
}

@Composable
private fun AppContent(vm: AppViewModel, darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val hazeState = rememberHazeState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onForeground()
                Lifecycle.Event.ON_STOP -> vm.onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            vm.onForeground()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(hazeState = hazeState)

        CompositionLocalProvider(LocalHazeState provides hazeState) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 14 }
                },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = {
                    fadeOut(tween(150)) + slideOutHorizontally(tween(240)) { it / 14 }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = vm,
                        onAddServer = { navController.navigate("edit/new") },
                        onOpenServer = { id -> navController.navigate("detail/$id") },
                        onOpenTerminal = { id -> navController.navigate("terminal/$id") },
                        onOpenSettings = { navController.navigate("settings") },
                    )
                }
                composable("detail/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    DetailScreen(
                        viewModel = vm,
                        serverId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { serverId -> navController.navigate("edit/$serverId") },
                        onTerminal = { serverId -> navController.navigate("terminal/$serverId") },
                    )
                }
                composable("edit/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    EditServerScreen(
                        viewModel = vm,
                        serverId = id.ifBlank { "new" },
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable("terminal/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    TerminalScreen(
                        viewModel = vm,
                        serverId = id,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface(
                            shape = RoundedCornerShape(20.dp),
                            accent = AccentCyan,
                            fillAlpha = 0.10f,
                            borderAlpha = 0.22f,
                            elevated = true,
                        )
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(17.dp),
                    )
                    Box(Modifier.width(9.dp))
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
