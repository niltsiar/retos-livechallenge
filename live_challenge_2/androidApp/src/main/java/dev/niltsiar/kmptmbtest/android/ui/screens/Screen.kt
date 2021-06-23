package dev.niltsiar.kmptmbtest.android.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DepartureBoard
import androidx.compose.material.icons.twotone.Map
import androidx.compose.material.icons.twotone.Subway
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val name: String, val icon: ImageVector) {
    object SubwayLinesScreen : Screen("subwayLines", "Metro Lines", Icons.TwoTone.Subway)
    object SubwayPathsScreen : Screen("subwayPaths", "Metro map", Icons.TwoTone.Map)
    object BusStopsScreen : Screen("busStops", "Bus Stops", Icons.TwoTone.DepartureBoard)
}
