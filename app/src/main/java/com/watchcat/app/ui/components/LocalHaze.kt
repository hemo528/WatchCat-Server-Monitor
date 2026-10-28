package com.watchcat.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState

val LocalHazeState = staticCompositionLocalOf<HazeState> {
    error("HazeState 未提供，请用 WatchCatScaffold 包裹内容")
}
